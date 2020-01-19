package com.example.tcs15034.camera;

interface RemoteControlEventListener {
    void onCommandTakePicture();
    void onConnect();
    void onCommandSuccess();
}
