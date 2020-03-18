// IUSBInfoCallBack.aidl
package com.jiangdg.libusbcamera;

// Declare any non-default types here with import statements

interface IUSBInfoCallBack {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void h264CallBack(int status, int channel, in byte[] nalu);

    void onPreViewCallBack(in byte[] yuv);
}
