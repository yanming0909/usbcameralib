/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.usb.encoder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.usb.mydb.MovieDao;

public class MediaMuxerWrapper {
    private static final boolean DEBUG = true;    // TODO set false on release
    private static final String TAG = "MediaMuxerWrapper";

    private static final String DIR_NAME = "USBCameraTest";
    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("0-1-yyyyMMddHHmmss_", Locale.CHINA);
    private int id;

    private String mOutputPath;
    private MediaMuxer mMediaMuxer;    // API >= 18
    private int mEncoderCount, mStatredCount;
    private boolean mIsStarted;
    private MediaEncoder mVideoEncoder, mAudioEncoder;
    private MediaFormat mAudioFormat;
    private MediaFormat mVideoFormat;
    private int mVideoTrackIndex;
    private int mAudioTrackIndex;

    /**
     * Constructor
     *
     * @param ext extension of output file
     * @throws IOException
     */
    public MediaMuxerWrapper(int id, String ext) throws IOException {
        this.id = id;
        if (TextUtils.isEmpty(ext)) ext = ".mp4";
        try {
            mOutputPath = getCaptureFile(id, ext).toString();
        } catch (final NullPointerException e) {
            throw new RuntimeException("This app has no permission of writing external storage");
        }
        mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mEncoderCount = mStatredCount = 0;
        mIsStarted = false;
    }

    public String getOutputPath() {
        return mOutputPath;
    }

    public void prepare() throws IOException {
        if (mVideoEncoder != null)
            mVideoEncoder.prepare();
        if (mAudioEncoder != null)
            mAudioEncoder.prepare();
    }

    private long mBeginMillis;

    public void startRecording() {

        if (mVideoEncoder != null)
            mVideoEncoder.startRecording();
        if (mAudioEncoder != null)
            mAudioEncoder.startRecording();
    }

    public void stopRecording() {
        if (mVideoEncoder != null)
            mVideoEncoder.stopRecording();
        mVideoEncoder = null;
        if (mAudioEncoder != null)
            mAudioEncoder.stopRecording();
        mAudioEncoder = null;
    }

    public synchronized boolean isStarted() {
        return mIsStarted;
    }

//**********************************************************************
//**********************************************************************

    /**
     * assign encoder to this calss. this is called from encoder.
     *
     * @param encoder instance of MediaVideoEncoder or MediaAudioEncoder
     */
    /*package*/ void addEncoder(final MediaEncoder encoder) {
        if (encoder instanceof MediaVideoEncoder) {
            if (mVideoEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mVideoEncoder = encoder;
        } else if (encoder instanceof MediaSurfaceEncoder) {
            if (mVideoEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mVideoEncoder = encoder;
        } else if (encoder instanceof MediaVideoBufferEncoder) {
            if (mVideoEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mVideoEncoder = encoder;
        } else if (encoder instanceof MediaAudioEncoder) {
            if (mAudioEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mAudioEncoder = encoder;
        } else
            throw new IllegalArgumentException("unsupported encoder");
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }

    /**
     * request start recording from encoder
     *
     * @return true when muxer is ready to write
     */
    /*package*/
    synchronized boolean start() {
        if (DEBUG) Log.v(TAG, "start:");
        mStatredCount++;
        if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
            mMediaMuxer.start();
            mIsStarted = true;
            mBeginMillis = SystemClock.elapsedRealtime();
            notifyAll();
            if (DEBUG) Log.v(TAG, "MediaMuxer started:");
        }
        return mIsStarted;
    }

    /**
     * request stop recording from encoder when encoder received EOS
     */
    /*package*/
    synchronized void stop() {
        if (DEBUG) Log.v(TAG, "stop:mStatredCount=" + mStatredCount);
        mStatredCount--;
        if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
            try {
                mMediaMuxer.stop();
            } catch (final Exception e) {
                Log.w(TAG, e);
            }
            mIsStarted = false;
            if (DEBUG) Log.v(TAG, "MediaMuxer stopped:");
        }
    }

    /**
     * assign encoder to muxer
     *
     * @param format
     * @return minus value indicate error
     */
    /*package*/
    synchronized int addTrack(final MediaFormat format, boolean isVideo) {

        if (mIsStarted)
            throw new IllegalStateException("muxer already started");
        final int track = mMediaMuxer.addTrack(format);
        if (isVideo) {
            if (DEBUG)
                Log.i(TAG, "mVideoFormat addTrack:trackNum=" + mEncoderCount + ",trackIx=" + track + ",format=" + format);

            mVideoFormat = format;
            mVideoTrackIndex = track;
        } else {
            if (DEBUG)
                Log.i(TAG, "mAudioFormat addTrack:trackNum=" + mEncoderCount + ",trackIx=" + track + ",format=" + format);

            mAudioFormat = format;
            mAudioTrackIndex = track;

        }
        return track;
    }

    /**
     * write encoded data to muxer
     *
     * @param trackIndex
     * @param byteBuf
     * @param bufferInfo
     */
    /*package*/
    synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
        if (mAudioTrackIndex == -1 || mVideoTrackIndex == -1) {
            return;
        }
        if (mStatredCount > 0) {
            byteBuf.position(bufferInfo.offset);
            byteBuf.limit(bufferInfo.offset + bufferInfo.size);
            if (!TextUtils.isEmpty(initPath())) {
                mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
            } else {
                release();
            }
            if (SystemClock.elapsedRealtime() - mBeginMillis >= 60000) {
                release();
                mMediaMuxer = null;
                mAudioTrackIndex = mVideoTrackIndex = -1;
                Log.d(TAG, "pumpStream: 分隔开始");
                try {
                    mOutputPath = getCaptureFile(id, ".mp4").getAbsolutePath();
                    Log.d(TAG, "mOutputPath: " + mOutputPath);
                    mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    addTrack(mVideoFormat, true);
//                    addTrack(mAudioFormat, false);
                    synchronized (mMediaMuxer) {
                        mMediaMuxer.wait(200);
                    }
                    mMediaMuxer.start();
                    mIsStarted = true;
                    mBeginMillis = SystemClock.elapsedRealtime();
                    notifyAll();
                } catch (IOException e) {
                    Log.i(TAG, String.format("muxer is start. now it will be IOException."));
                } catch (IllegalStateException e) {
                    Log.i(TAG, String.format("muxer is start. now it will be IllegalStateException."));
                } catch (InterruptedException e) {
                    Log.i(TAG, String.format("muxer is start. now it will be InterruptedException."));
                }
            }
        }
    }

    public synchronized void release() {
        if (mMediaMuxer != null) {
            if (mVideoTrackIndex != -1) {
                Log.i(TAG, String.format("muxer is started. now it will be stoped."));
                try {
                    mMediaMuxer.stop();
                } catch (IllegalStateException ex) {
                    Log.i(TAG, String.format("muxer is stoped. now it will be IllegalStateException."));
                }

                if (SystemClock.elapsedRealtime() - mBeginMillis <= 1500) {
                    File videoFile = new File(mOutputPath);
                    if (videoFile.exists()) videoFile.delete();
                }
                mIsStarted = false;
            } else {
                Log.i(TAG, String.format("muxer is failed to be stoped."));
            }
        }
    }
//**********************************************************************
//**********************************************************************

    /**
     * generate output file
     *
     * @param ext .mp4(.m4a for audio) or .png
     * @return return null when this app has no writing permission to external storage.
     */
    public static File getCaptureFile(int id, final String ext) {
        final File dir = new File(initPath(), ext.equals(".jpg") ? "pic" : "video");
        dir.mkdirs();
        if (dir.canWrite()) {
            if (id == 1)
                return new File(dir, getDateTimeString() + "A" + ext);
            if (id == 2)
                return new File(dir, getDateTimeString() + "B" + ext);
        }
        return null;
    }

    /**
     * get current date and time as String
     *
     * @return
     */
    private static final String getDateTimeString() {
        return mDateTimeFormat.format(new Date());
    }

    public static String initPath() {
        String path = "";
        File file = new File("/mnt/extsd/YC8300A");
        if (file.exists()) {
            if (file.canWrite()) {
                path = file.getAbsolutePath();
            }
        }
        return path;
    }
}
