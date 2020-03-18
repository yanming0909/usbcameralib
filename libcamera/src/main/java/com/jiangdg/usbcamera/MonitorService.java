package com.jiangdg.usbcamera;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import com.jiangdg.libusbcamera.IUSBCamera1;
import com.jiangdg.libusbcamera.IUSBInfoCallBack;
import com.jiangdg.libusbcamera.R;
import com.serenegiant.usb.SDCardUtil;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.encoder.MediaMuxerWrapper;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class MonitorService extends Service {
    public static final String ACTION_BACK_HOME = "com.patent.monitorService.ACTION_BACK_HOME";
    public static final String ACTION_SHOW_RECORDER = "com.patent.monitorService.ACTION_SHOW_RECORDER";
    public static final String ACTION_START_RECORD = "com.patent.monitorService.ACTION_START_RECORD";
    public static final String ACTION_STOP_RECORD = "com.patent.monitorService.ACTION_STOP_RECORD";

    private static final int NOTIFICATION_DI = 1234;
    private static final String TAG = "MonitorService";

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private int screenWidth;
    private int screenHeight;
    private View mRecorderView;
    CameraViewInterface uvc_camera1;
    private UVCCameraHelper mCameraHelper1;//默認车外
    //  2141 515 1873
    // 25408 车外  25413 车内 (圆头)
    // 25446 海康
    int camID1 = 25446;

    private boolean isRequest;
    /* RecorderManager manages the job of video recording */
    private MonitorServiceReceiver mMonitorServiceReceiver;
    private boolean isHide = true;

    private Handler msgHandler;


    private IUSBInfoCallBack infoCallBack;

    public class CamBinder extends IUSBCamera1.Stub{
        @Override
        public void registerCallback(IUSBInfoCallBack callBack) throws RemoteException {
            infoCallBack = callBack;
        }

        @Override
        public void unregisterCallback(IUSBInfoCallBack callBack) throws RemoteException {
            infoCallBack = null;

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
                .setContentTitle(TAG)
                .setContentText("Recording")
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
        int defaultSize = 1;
        mLayoutParams.width = defaultSize;
        mLayoutParams.height = defaultSize;
        mLayoutParams.gravity = Gravity.START | Gravity.BOTTOM;
        mWindowManager.addView(mRecorderView, mLayoutParams);
        registerReceiver();
    }

    private void initUVC() {
        uvc_camera1 = mRecorderView.findViewById(R.id.uvc_camera1);
        mCameraHelper1 = new UVCCameraHelper();
//        mCameraHelper1.setDefaultPreviewSize(1280, 800);
        mCameraHelper1.initUSBMonitor(this, uvc_camera1, listener1);

        uvc_camera1.setCallback(new CameraViewInterface.Callback() {
            @Override
            public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
                // must have
                if (mCameraHelper1.isCameraOpened()) {
                    mCameraHelper1.startPreview(uvc_camera1);
                }
            }

            @Override
            public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

            }

            @Override
            public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
                // must have
                if (mCameraHelper1.isCameraOpened()) {
                    mCameraHelper1.stopPreview();
                }
            }
        });
//        mCameraHelper1.setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
//            @Override
//            public void onPreviewResult(byte[] data) {
//                libPublisher.SmartPublisherOnCaptureVideoData(publisherHandle, data, data.length, 2, 2);
//            }
//        });
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: pid" + Process.myPid());
        if (mCameraHelper1 != null) {
            mCameraHelper1.registerUSB();
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
                startService(new Intent("com.patent.MonitorService"));
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
        if (mCameraHelper1 != null) {
            mCameraHelper1.unregisterUSB();
            mCameraHelper1.release();
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
            if (mCameraHelper1 != null) mCameraHelper1.stopPusher();
    }

    private void startRecorder() {
        RecordParams params = new RecordParams();
        params.setId(1);
        params.setVoiceClose(false);
            if (mCameraHelper1 != null)
                mCameraHelper1.setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
                    @Override
                    public void onPreviewResult(byte[] data) {
                        if(infoCallBack!=null){
                            try {
                                infoCallBack.onPreViewCallBack(data);
                            } catch (RemoteException e) {
                                Log.d(TAG, "数据回调错误: ");
                                e.printStackTrace();
                            }
                        }
                    }
                });
                mCameraHelper1.startPusher(this, params, new AbstractUVCCameraHandler.OnEncodeResultListener() {
                    @Override
                    public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
                        //0是音频 1是视频
                        Log.d(TAG, "onEncodeResult 1: size: " + data.length);
                    }

                    @Override
                    public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type, int keyFrame) {
//                        Log.d(TAG, "onEncodeResult 1: size: " + data.length + " length: " + length + " offset: "
//                                + offset + " type: " + type
//                                + " 关键帧： " + keyFrame);
                        if (infoCallBack != null) {
                            try {
                                if(type==1){
                                    infoCallBack.h264CallBack(0, 0, Arrays.copyOfRange(data,offset,length));
                                }
                            } catch (RemoteException e) {
                                Log.d(TAG, "数据回调错误: ");
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onRecordResult(String videoPath) {
                        Log.d(TAG, "videoPath 1");
                    }
                });
    }

    private UVCCameraHelper.OnMyDevConnectListener listener1 = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            Log.d(TAG, "onAttachDev: " + device.toString());
            // request open permission
            if (device.getProductId() == camID1 && !isRequest) {
                isRequest = true;
                if (mCameraHelper1 != null) {
                    mCameraHelper1.requestPermission(device, 200);
                }
            }

        }

        @Override
        public void onDettachDev(UsbDevice device) {
            if (device.getProductId() == camID1 && isRequest) {
                isRequest = false;
                mCameraHelper1.closeCamera();
                stopRecorder();
            }


        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (device.getProductId() == camID1) {
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
            if (device.getProductId() == camID1 && isRequest) {
                isRequest = false;
                mCameraHelper1.closeCamera();
            }

        }
    };
}
