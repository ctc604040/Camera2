package com.example.tcs15034.camera;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;


public class BluetoothServer {
    private AppCompatActivity mActivity;
    private BluetoothAdapter mBluetoothAdapter;
    BTServerThread btServerThread;

    RemoteControlEventListener mRemoteControlEventListener;
    Handler mUiHandler;

    JpgContainer jpgData = new JpgContainer();
    byte[] jpgarr = new byte[1024*1024*10];
    boolean jpgf = false;
    int jpLength;

    final int REQUEST_ENABLE_BT = 1;

    BluetoothServer(AppCompatActivity activity) {
        mActivity = activity;
    }

    void setRemoteControlEventListener(RemoteControlEventListener listener, Handler uiHandler){
        mRemoteControlEventListener = listener;
        mUiHandler = uiHandler;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    synchronized void start() throws IOException {
        if (mBluetoothAdapter == null) {
            BluetoothManager bluetoothManager =
                    (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                Log.d(MainActivity.class.getName(), "Device does not support Bluetooth");
                throw new IOException("Device does not support Bluetooth.");
            }
        }
        btServerThread = new BTServerThread();
        btServerThread.start();
    }

    synchronized void stop() {
        if( btServerThread != null){
            btServerThread.cancel();
            btServerThread = null;
        }
    }

    public class BTServerThread extends Thread {
        static final String TAG = "BTTest1Server";
        static final String BT_NAME = "BTTEST1";
        UUID BT_UUID = UUID.fromString(
                "41eb5f39-6c3a-4067-8bb9-bad64e6e0908");
        BluetoothServerSocket bluetoothServerSocket;
        BluetoothSocket bluetoothSocket;
        InputStream inputStream;
        OutputStream outputStream;

        @SuppressLint("MissingPermission")
        BTServerThread(){
            BluetoothServerSocket socket = null;
            try {
                socket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        BT_NAME, BT_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            bluetoothServerSocket = socket;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        public void run() {
            byte[] incomingBuff = new byte[64];

            try {
                while (true) {
                    if(bluetoothServerSocket == null){
                        break;
                    }
                    try {
                        bluetoothSocket = bluetoothServerSocket.accept();
                        processConnect();

                        inputStream = bluetoothSocket.getInputStream();
                        outputStream = bluetoothSocket.getOutputStream();

                        while (true) {
                            int incomingBytes = inputStream.read(incomingBuff);
                            byte[] buff = new byte[incomingBytes];
                            System.arraycopy(incomingBuff, 0, buff, 0, incomingBytes);
                            String cmd;
                            cmd = new String(buff, java.nio.charset.StandardCharsets.UTF_8);
                            String chkCmd = "GetImage";
                            //コマンドのチェック
                            if(chkCmd.equals(cmd))
                            {
                                // 写真撮影
                                processBtCommand();
                                //画像が取れるまで待つ
                                while(!jpgf){
                                    Thread.sleep(1000);
                                    successBtCommand();
                                }

                                jpgf = false;
                                try {
                                    //画像サイズを送信
                                    int i = jpLength;
                                    byte[] bytes = ByteBuffer.allocate(4).putInt(i).array();
                                    outputStream.write(bytes,0,4);
                                    //ack待ち
                                    inputStream.read(incomingBuff);
                                    //画像を500byteに分割して送信
                                    int sndCnt = 0;
                                    while(i>sndCnt){
                                        if((i - sndCnt) > 500) {
                                            outputStream.write(jpgarr, sndCnt, 500);
                                            sndCnt += 500;
                                        }else{
                                            outputStream.write(jpgarr, sndCnt, i - sndCnt);
                                            sndCnt = i;
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }else {
                                processBtCommand();
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "accept() failed", e);
                    }

                    if (bluetoothSocket != null) {
                        try {
                            bluetoothSocket.close();
                            bluetoothSocket = null;
                        } catch (IOException e) {
                        }
                        Log.d(TAG, "Close socket.");
                    }

                    if (Thread.interrupted()) {
                        break;
                    }

                    // Bluetooth connection broke. Start Over in a few seconds.
                    Thread.sleep(3 * 1000);
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Cancelled ServerThread");
            }

            Log.d(TAG, "ServerThread exit");
        }

        void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "bluetoothServerSocket close() failed", e);
            }
            interrupt();
        }

    }

    private void processBtCommand(){
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                mRemoteControlEventListener.onCommandTakePicture();
            }
        });
    }

    private void processConnect(){
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                mRemoteControlEventListener.onConnect();
            }
        });
    }

    private void setStatusTextView(final String str){
//        mUiHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                textView_Status.setText(str);
//            }
//        });
    }
    private void successBtCommand(){
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                mRemoteControlEventListener.onCommandSuccess();
            }
        });
    }

    public void setJPGimage(final byte[] inJpgarr,int insize){
        jpgf = true;
        jpLength = insize;
        System.arraycopy(inJpgarr,0,jpgarr,0,insize);

        jpgData.SetAllValue(inJpgarr,insize,true);
    }
}

class JpgContainer{
    // 保存するJPGの最大値
    int JPGSIZE = 1024*1024*10;
    // JPG保存領域
    byte[] data = new byte[JPGSIZE];
    // 保存したJPGサイズ
    int size;
    // 保存データの有効性
    boolean enable = false;

    public boolean SetAllValue(byte[] inData,int inSize,boolean inEnable){
        // 初期値は失敗にする
        enable = false;
        // JPGサイズが大きすぎる場合はNULLを返す
        if(inSize > JPGSIZE) {
            return false;
        }
        // サイズが0でもNG
        if(inSize == 0) {
            return false;
        }
        enable = inEnable;
        size = inSize;
        System.arraycopy(inData,0,data,0,inSize);

        return true;
    }
}