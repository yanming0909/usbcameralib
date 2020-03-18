package com.serenegiant.usb.encoder.biz;

import com.serenegiant.usb.common.AbstractUVCCameraHandler;

public class AACEncodeManager {
    private AACEncodeConsumer mAacConsumer;

    private static final class SingleHolder {
        static AACEncodeManager mInstance = new AACEncodeManager();
    }

    public static AACEncodeManager getInstance() {
        return SingleHolder.mInstance;
    }
    public void startAudioRecord(Mp4MediaMuxer mMuxer,final AbstractUVCCameraHandler.OnEncodeResultListener mListener) {
        if (mAacConsumer==null) {
            mAacConsumer = new AACEncodeConsumer();
            mAacConsumer.setOnAACEncodeResultListener(new AACEncodeConsumer.OnAACEncodeResultListener() {
                @Override
                public void onEncodeResult(byte[] data, int offset, int length, long timestamp,int keyFrame) {
                    if (mListener != null) {
                        mListener.onEncodeResult(data, offset, length, timestamp, 0,keyFrame);
                    }
                }
            });
            mAacConsumer.start();
        }
            // 添加混合器
            if (mMuxer != null) {
                if (mAacConsumer != null) {
                    mAacConsumer.setTmpuMuxer(mMuxer);
                }
            }
    }
    public void stopAudioRecord() {
        if (mAacConsumer != null) {
            mAacConsumer.exit();
            mAacConsumer.setTmpuMuxer(null);
            try {
                Thread t1 = mAacConsumer;
                mAacConsumer = null;
                if (t1 != null) {
                    t1.interrupt();
                    t1.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
