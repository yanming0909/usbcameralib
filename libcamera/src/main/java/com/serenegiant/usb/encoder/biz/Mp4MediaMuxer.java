package com.serenegiant.usb.encoder.biz;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;

import com.serenegiant.usb.SDCardUtil;
import com.serenegiant.usb.mydb.Movie;
import com.serenegiant.usb.mydb.MovieDao;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Mp4封装混合器
 * <p>
 * Created by jianddongguo on 2017/7/28.
 */

public class Mp4MediaMuxer {
    private static final boolean VERBOSE = false;
    private static final String TAG = Mp4MediaMuxer.class.getSimpleName();
    private String time;
    private String mVideoPath;
    private String mAudioPath;
    private String mVideoFilePath;
    private String mAudioFilePath;
    private MediaMuxer mMuxer;
    private long durationMillis;
    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;
    private long mBeginMillis;
    private MediaFormat mVideoFormat;
    private MediaFormat mAudioFormat;
    private boolean isVoiceClose;
    private Context context;
    private long videiIndex;
    private long audioIndex;
    private int id;
    private SimpleDateFormat format = new SimpleDateFormat("0-1-yyyyMMddHHmmss");
    // 文件路径；文件时长
    public Mp4MediaMuxer(Context context, int id, long durationMillis, boolean isVoiceClose) {
        if (TextUtils.isEmpty(initPath())) return;
        this.id=id;
        this.isVoiceClose = isVoiceClose;
        this.durationMillis = durationMillis;
        this.context = context;
        time = format.format(new Date());
        mVideoFilePath = initPath() + time +(id==1?"_A":"_B") + ".mp4";

//        try {
//            mMuxer = new MediaMuxer(mVideoFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//        } catch (FileNotFoundException ex){
//            if (ex.getMessage().contains("No space left on device")){
//                delMovie(20);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        MovieDao.getInstance(context).insert(new Movie(mVideoFilePath, time.split("-")[2], time, mAudioFilePath));
    }

    private void delMovie(int count) {
        //删除文件
        List<Movie> movies = MovieDao.getInstance(context).getMovies(count);
        for (Movie m : movies) {
            File file = new File(m.getFile_name());
            if (file.exists()&&file.delete())
                MovieDao.getInstance(context).deleteForPath(m.getPath());
        }
    }

    public synchronized void addTrack(MediaFormat format, boolean isVideo) {
        // now that we have the Magic Goodies, start the muxer
        if ((!isVoiceClose && mAudioTrackIndex != -1) && mVideoTrackIndex != -1)
            throw new RuntimeException("already add all tracks");

        if (mMuxer == null || format == null) return;
        int track = mMuxer.addTrack(format);
        if (VERBOSE)
            Log.i(TAG, String.format("addTrack %s result %d", isVideo ? "video" : "audio", track));

        if (isVideo) {
            mVideoFormat = format;
            mVideoTrackIndex = track;
            // 当音频轨道添加
            // 或者开启静音就start
            if (isVoiceClose || mAudioTrackIndex != -1) {
                if (VERBOSE)
                    Log.i(TAG, "both audio and video added,and muxer is started");
                mMuxer.start();
                mBeginMillis = SystemClock.elapsedRealtime();
            }
        } else {
            mAudioFormat = format;
            mAudioTrackIndex = track;
            if (mVideoTrackIndex != -1) {
                mMuxer.start();
                mBeginMillis = SystemClock.elapsedRealtime();
            }
        }
    }

    public synchronized void pumpStream(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo, boolean isVideo) {
        if ((!isVoiceClose && mAudioTrackIndex == -1) || mVideoTrackIndex == -1) {
//            Log.i(TAG, String.format("pumpStream [%s] but muxer is not start.ignore..", isVideo ? "video" : "audio"));
            return;
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
        } else if (bufferInfo.size != 0) {

            // adjust the ByteBuffer values to match BufferInfo (not needed?)
            outputBuffer.position(bufferInfo.offset);
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (!TextUtils.isEmpty(initPath())) {
                    mMuxer.writeSampleData(isVideo ? mVideoTrackIndex : mAudioTrackIndex, outputBuffer, bufferInfo);
                } else {
                    release();
                    return;
                }
            }
//            if (VERBOSE)
//                Log.d(TAG, String.format("sent %s [" + bufferInfo.size + "] with timestamp:[%d] to muxer", isVideo ? "video" : "audio", bufferInfo.presentationTimeUs / 1000));
        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//            if (VERBOSE)
//                Log.i(TAG, "BUFFER_FLAG_END_OF_STREAM received");
        }
        if (durationMillis != 0 && (SystemClock.elapsedRealtime() - mBeginMillis >= durationMillis||SystemClock.elapsedRealtime() < mBeginMillis)) {
            checkSDAvailable();
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
            reRecord();
            pumpStream(outputBuffer, bufferInfo, true);
        }
    }

    private void reRecord() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "pumpStream: 重新开始");
            mVideoTrackIndex = mAudioTrackIndex = -1;
            if (TextUtils.isEmpty(initPath()))return;
            try {
            time = format.format(new Date());
            mVideoFilePath = initPath() + time +(id==1?"_A":"_B") + ".mp4";
                mMuxer = new MediaMuxer(mVideoFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                addTrack(mVideoFormat, true);
                addTrack(mAudioFormat, false);;
                MovieDao.getInstance(context).insert(new Movie(mVideoFilePath, time.split("-")[2], time, mAudioFilePath));
            } catch (IOException e) {
                Log.d(TAG, "Muxer is start  now throw IOException");
            }
        }
    }

    public synchronized void release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (mMuxer != null) {
                //(!isVoiceClose&&mAudioTrackIndex != -1)
                if (mVideoTrackIndex != -1) {
                    if (VERBOSE)
                        Log.i(TAG, String.format("muxer is started. now it will be stoped."));
                    try {
                        mMuxer.stop();
                        mMuxer.release();
                    } catch (IllegalStateException ex) {
                        Log.i(TAG, String.format("muxer is stoped. now it will be IllegalStateException."));
                    }
                    mMuxer=null;
                    if (SystemClock.elapsedRealtime() - mBeginMillis <= 1500) {
                       File videoFile =  new File(mVideoFilePath);
                       if (videoFile.exists()&&videoFile.delete())
                        MovieDao.getInstance(context).deleteForPath(mVideoFilePath);
                    }
                    mAudioTrackIndex = mVideoTrackIndex = -1;
                } else {
                    if (VERBOSE)
                        Log.i(TAG, String.format("muxer is failed to be stoped."));
                }
            }
        }
    }
    public static String initPath(){
        String path="";
        File file = new File("/mnt/extsd/YC8300A/video");
        if (file.exists()&&file.canWrite()){
            path=file.getAbsolutePath()+"/";
        }
        return path;
    }
    public void checkSDAvailable(){
        if (SDCardUtil.getAvailableBlock("/mnt/extsd")<1){
            //删除视频
         delMovie(20);
        }
    }
}
