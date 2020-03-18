package com.jiangdg.usbcamera;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import com.jiangdg.libusbcamera.IUSBCamera2;
import com.jiangdg.libusbcamera.IUSBInfoCallBack;
import com.jiangdg.libusbcamera.R;
import com.serenegiant.usb.SDCardUtil;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;
import java.util.Objects;

public class MonitorService2 extends Service {
    public static final String ACTION_BACK_HOME = "com.patent.monitorService2.ACTION_BACK_HOME";
    public static final String ACTION_SHOW_RECORDER = "com.patent.monitorService2.ACTION_SHOW_RECORDER";
    public static final String ACTION_START_RECORD = "com.patent.monitorService2.ACTION_START_RECORD";
    public static final String ACTION_STOP_RECORD = "com.patent.monitorService2.ACTION_STOP_RECORD";

    private static final int NOTIFICATION_DI = 1235;
    private static final String TAG = "monitorService2";

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private int screenWidth;
    private int screenHeight;
    private View mRecorderView;
    CameraViewInterface uvc_camera2;
    private UVCCameraHelper mCameraHelper2;//默認车内
    //    int camID1 =25408;
    int camID2 = 25413;//2141 25413 768

    //    private boolean isRequest;
    private boolean isRequest2;
    /* RecorderManager manages the job of video recording */
    private MonitorServiceReceiver mMonitorServiceReceiver;
    private boolean isHide = true;

    private Handler msgHandler;


    private IUSBInfoCallBack infoCallBack2;

    public class CamBinder extends IUSBCamera2.Stub {
        @Override
        public void registerCallback(IUSBInfoCallBack callBack) throws RemoteException {
            infoCallBack2 = callBack;
        }

        @Override
        public void unregisterCallback(IUSBInfoCallBack callBack) throws RemoteException {
            infoCallBack2 = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new CamBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        msgHandler = new Handler();
        // Start foreground service to avoid unexpected kill
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Background Video Recorder")
                .setContentText("Click into the application")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        startForeground(NOTIFICATION_DI, notification);

        mWindowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        mRecorderView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.layout_camera, null);
        initUVC();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        int defaultSize = 50;
        mLayoutParams.width = defaultSize;
        mLayoutParams.height = defaultSize;
        mLayoutParams.gravity = Gravity.END | Gravity.BOTTOM;
        mWindowManager.addView(mRecorderView, mLayoutParams);
        registerReceiver();
    }
    private void initUVC() {
        uvc_camera2 = mRecorderView.findViewById(R.id.uvc_camera1);

        mCameraHelper2 = new UVCCameraHelper();
//        mCameraHelper2.setDefaultPreviewSize(800,600);
        mCameraHelper2.initUSBMonitor(this, uvc_camera2, listener1);
        uvc_camera2.setCallback(new CameraViewInterface.Callback() {
            @Override
            public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
                // must have
                if (mCameraHelper2.isCameraOpened()) {
                    mCameraHelper2.startPreview(uvc_camera2);
                }
            }

            @Override
            public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

            }

            @Override
            public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
                // must have
                if (mCameraHelper2.isCameraOpened()) {
                    mCameraHelper2.stopPreview();
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: pid" + Process.myPid());

        if (mCameraHelper2 != null) {
            mCameraHelper2.registerUSB();
        }
//        crashHandler();
        return super.onStartCommand(intent, flags, startId);
    }

    private void crashHandler() {
        // 崩溃时触发线程
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (msgHandler != null) {
                    msgHandler.removeCallbacksAndMessages(null);
                }
                Log.d(TAG, "uncaughtException:  e" + e);
                Process.killProcess(Process.myPid());
                startService(new Intent("com.patent.MonitorService2"));
            }
        });
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_BACK_HOME);
        intentFilter.addAction(ACTION_SHOW_RECORDER);
        intentFilter.addAction(ACTION_START_RECORD);
        intentFilter.addAction(ACTION_STOP_RECORD);
        mMonitorServiceReceiver = new MonitorServiceReceiver();
        registerReceiver(mMonitorServiceReceiver, intentFilter);
    }

    public void hideRecorder(boolean isHide) {
        this.isHide = isHide;
        mLayoutParams.width = isHide ? 1 : screenWidth;
        mLayoutParams.height = isHide ? 1 : screenHeight;
        mWindowManager.updateViewLayout(mRecorderView, mLayoutParams);
    }

    @Override
    public void onDestroy() {
        stopRecorder();
        if (msgHandler != null) {
            msgHandler.removeCallbacksAndMessages(null);
        }
        if (mCameraHelper2 != null) {
            mCameraHelper2.unregisterUSB();
            mCameraHelper2.release();
        }
        if (mWindowManager != null) {
            mWindowManager.removeView(mRecorderView);
        }
        unregisterReceiver(mMonitorServiceReceiver);
        stopForeground(true);
        Process.killProcess(Process.myPid());
    }

    private class MonitorServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), ACTION_BACK_HOME)) {
                hideRecorder(true);
            }
            if (Objects.equals(intent.getAction(), ACTION_SHOW_RECORDER)) {
                hideRecorder(false);
            }
            if (Objects.equals(intent.getAction(), ACTION_START_RECORD)) {
                startRecorder();
            }
            if (Objects.equals(intent.getAction(), ACTION_STOP_RECORD)) {
                stopRecorder();
            }
        }
    }

    private void stopRecorder() {

            if (mCameraHelper2 != null) mCameraHelper2.stopPusher();

    }

    private void startRecorder() {
        if (!SDCardUtil.canWrite(this)) return;
        RecordParams params = new RecordParams();
        params.setId(2);
        params.setVoiceClose(false);
            if (mCameraHelper2 != null)
                mCameraHelper2.startPusher(this, params, new AbstractUVCCameraHandler.OnEncodeResultListener() {
                    @Override
                    public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {

                    }
                    @Override
                    public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type, int keyFrame) {
//                        Log.d(TAG, "onEncodeResult 2: size: " + data.length + " length: " + length + " offset: "
//                                + offset + " type: " + type
//                                + " 关键帧： " + keyFrame);
                        if (infoCallBack2 != null) {
                            try {
                                if (type == 1) {
                                    infoCallBack2.h264CallBack(0, 1, data);

                                }
                            } catch (RemoteException e) {
                                Log.d(TAG, "数据回调错误: ");
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onRecordResult(String videoPath) {
                        Log.d(TAG, "videoPath 2");

                    }
                });


    }

    private UVCCameraHelper.OnMyDevConnectListener listener1 = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            Log.d(TAG, "onAttachDev: " + device.toString());
            // request open permission
            if (device.getProductId() == camID2 && !isRequest2) {
                isRequest2 = true;
                if (mCameraHelper2 != null) {
                    mCameraHelper2.requestPermission(device, 100);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            if (device.getProductId() == camID2 && isRequest2) {
                stopRecorder();
                isRequest2 = false;
                mCameraHelper2.closeCamera();
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {

            if (device.getProductId() == camID2) {
                if (!isConnected) {
                    Log.d("onConnectDev:", "mCameraHelper1 fail to connect,please check resolution params ");
                } else {
                    msgHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startRecorder();
                        }
                    }, 500);
                }
            }

        }

        @Override
        public void onDisConnectDev(UsbDevice device) {

            if (device.getProductId() == camID2 && isRequest2) {
                stopRecorder();
                isRequest2 = false;
                mCameraHelper2.closeCamera();
            }
        }
    };
}
