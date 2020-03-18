// IUSBCamera1.aidl
package com.jiangdg.libusbcamera;
import com.jiangdg.libusbcamera.IUSBInfoCallBack;

// Declare any non-default types here with import statements

interface IUSBCamera1 {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
     //注册的预览回调
     void registerCallback(IUSBInfoCallBack callBack);

     //注销
     void unregisterCallback(IUSBInfoCallBack callBack);
}
