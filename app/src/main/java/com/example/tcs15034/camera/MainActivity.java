package com.example.tcs15034.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ZoomControls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

//public class MainActivity extends AppCompatActivity implements View.OnClickListener {
public class MainActivity extends AppCompatActivity {
    // カメラインスタンス
    private Camera mCam = null;

    // カメラプレビュークラス
    private CameraPreview mCamPreview = null;

    // 画面タッチの2度押し禁止用フラグ
    private boolean mIsTake = false;

    // カメラ切り替え用フラグ
    private int currentCameraId;

    // Bluetoothサーバ
    private BluetoothServer mBluetoothServer;
    RemoteControlEventListener mRemoteControlListener;
    Handler mUiHandler;

    byte[] jpgarr = new byte[1024*1024*10];
    int jpgLength = 0;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);

        // 起動時はフロントカメラを使う
        currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;


        // カメラインスタンスの取得
        try {
            mCam = Camera.open(currentCameraId);

            //プレビューを縦向きにしたいので回転。
            mCam.setDisplayOrientation(90);

            android.util.Log.d("_Debug", "mCam = " + mCam);
        } catch (Exception e) {
            // エラー
            this.finish();
        }

        // FrameLayout に CameraPreview クラスを設定
        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
        mCamPreview = new CameraPreview(this, mCam);
        preview.addView(mCamPreview);


        /*
        // mCamPreview に タッチイベントを設定
        mCamPreview.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (mIsTake) {
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // 撮影中の2度押し禁止用フラグ
                    mIsTake = true;
                    // オートフォーカス
                    Log.d("_Debug","  autoFocus");
                    mCam.autoFocus(mAutoFocusListener);
                }
                return true;
            }
        });
        */


        findViewById(R.id.buttonChangeCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        findViewById(R.id.buttonShutter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsTake) {
                    // 撮影中の2度押し禁止用フラグ
                    mIsTake = true;
//                        // 画像取得
//                        mCam.takePicture(null, null, mPicJpgListener);

                    //オートフォーカス
                    mCam.autoFocus(mAutoFocusListener);
                }
            }
        });

        findViewById(R.id.buttonMenu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MenuActivity.class);
                startActivity(intent);
            }
        });

        mBluetoothServer = new BluetoothServer(this);
        mRemoteControlListener = new BtRemoteControllEventlistener();
        mUiHandler = new Handler(Looper.getMainLooper());
        mBluetoothServer.setRemoteControlEventListener(mRemoteControlListener, mUiHandler);
    }

    /**
     * オートフォーカス完了のコールバック
     */
   private Camera.AutoFocusCallback mAutoFocusListener = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            // 撮影
            Log.d("_Debug","  takePicture");
            mCam.takePicture(null, null, mPicJpgListener);
        }
    };

    /*
    @Override
    protected void onPause() {
        super.onPause();
        // カメラ破棄インスタンスを解放
        if (mCam != null) {
            mCam.release();
            mCam = null;
        }
    }
    */

    @Override
    protected void onStart() {
        super.onStart();
        try {
            mBluetoothServer.start();
        } catch (IOException e) {}
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBluetoothServer.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // カメラ破棄インスタンスを解放
        if (mCam != null) {
            mCam.release();
            mCam = null;
        }
        mBluetoothServer.stop();
    }

    @Override
    protected  void onResume() {
        super.onResume();

        startCameraPreview();
    }

    /**
     * JPEG データ生成完了時のコールバック
     */
    private Camera.PictureCallback mPicJpgListener = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            if (data == null) {
                return;
            }

            // グローバルに画像データを保存
            System.arraycopy(data,0,jpgarr,0,data.length);
            jpgLength = data.length;

            String saveDir = Environment.getExternalStorageDirectory().getPath() + "/test";

            // SD カードフォルダを取得
            File file = new File(saveDir);

            // フォルダ作成
            if (!file.exists()) {
                if (!file.mkdir()) {
                    Log.e("Debug", "Make Dir Error");
                }
            }

            // 画像保存パス
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String imgPath = saveDir + "/" + sf.format(cal.getTime()) + ".jpg";

            // ファイル保存
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(imgPath, true);
                fos.write(data);
                fos.close();

                // アンドロイドのデータベースへ登録
                // (登録しないとギャラリーなどにすぐに反映されないため)
                registAndroidDB(imgPath);

            } catch (Exception e) {
                Log.e("Debug", e.getMessage());
            }

            fos = null;

            // takePicture するとプレビューが停止するので、再度プレビュースタート
            mCam.startPreview();

            mIsTake = false;
        }
    };

    /**
     * アンドロイドのデータベースへ画像のパスを登録
     * @param path 登録するパス
     */
    private void registAndroidDB(String path) {
        // アンドロイドのデータベースへ登録
        // (登録しないとギャラリーなどにすぐに反映されないため)
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = MainActivity.this.getContentResolver();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put("_data", path);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    // カメラを切り替える　前⇔後
    private void switchCamera()
    {
        mCam.stopPreview();
        mCam.release();

        if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        try {
            startCameraPreview();
            Log.d("_Debug","  switchCamera");
        }
        catch (Exception e)
        {
            Log.e("Debug", e.getMessage());
        }

    }

    // カメラプレビュー開始
    private void startCameraPreview()
    {
        /*
        try {
            mCam = Camera.open(currentCameraId);

            //プレビューを縦向きにしたいので回転。
            mCam.setDisplayOrientation(90);

            android.util.Log.d("_Debug", "mCam = " + mCam);
        } catch (Exception e) {
            // エラー
            this.finish();
        }

        // FrameLayout に CameraPreview クラスを設定
        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
        mCamPreview = new CameraPreview(this, mCam);
        preview.addView(mCamPreview);
        */

        mCam.startPreview();
    }
    public class BtRemoteControllEventlistener implements RemoteControlEventListener {
        @Override
        public void onCommandTakePicture() {
            if (!mIsTake) {
                // 撮影中の2度押し禁止用フラグ
                mIsTake = true;

                //オートフォーカス
                mCam.autoFocus(mAutoFocusListener);
            }
        }
        @Override
        public void onCommandSuccess() {

            if(jpgLength!=0) {
                mBluetoothServer.setJPGimage(jpgarr,jpgLength);
                jpgLength=0;
            }

        }
        @Override
        public void onConnect() {
//            textView_Status.setText("Status: TakePicture コマンドを受信しました。");
        }
    }
}
