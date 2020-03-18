package com.serenegiant.usb;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;

import com.serenegiant.usb.mydb.MovieDao;
import com.serenegiant.utils.FileUtils;
import com.wzh.yuvwater.utils.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.content.Context.STORAGE_SERVICE;

public class SDCardUtil {
    private static final String TAG = "Util";

    /**
     * 获取外置
     * @param mContext
     * @return
     */
    public static String externalSDCardPath(Context mContext) {
        try {
            StorageManager storageManager = (StorageManager) mContext.getSystemService(STORAGE_SERVICE);
            // 7.0才有的方法
            List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
            Class<?> volumeClass = Class.forName("android.os.storage.StorageVolume");
            Method getPath = volumeClass.getDeclaredMethod("getPath");
            Method isRemovable = volumeClass.getDeclaredMethod("isRemovable");
            getPath.setAccessible(true);
            isRemovable.setAccessible(true);
            for (int i = 0; i < storageVolumes.size(); i++) {
                StorageVolume storageVolume = storageVolumes.get(i);
                String mPath = (String) getPath.invoke(storageVolume);
                Boolean isRemove = (Boolean) isRemovable.invoke(storageVolume);
                if (isRemove) {
                    return mPath;
                }
            }
        }catch (Exception e){
            Log.d("tag2","e == "+e.getMessage());
        }
        return "";
    }

    // 获取存储卡的挂载状态
    public static boolean checkMounted(Context context) {
        try {
            StorageManager sm = (StorageManager)context.getSystemService(STORAGE_SERVICE);
            Method getVolumeStateMethod = StorageManager.class.getMethod("getVolumeState", new Class[] {String.class});
            String state = (String) getVolumeStateMethod.invoke(sm, externalSDCardPath(context));
            return Environment.MEDIA_MOUNTED.equals(state);
        } catch (Exception e) {
            Log.e(TAG, "checkMounted() failed", e);
        }
        return false;
    }

    /**
     * sd卡可写
     * @param context
     * @return
     */
    public static boolean canWrite(Context context) {
        return !TextUtils.isEmpty(externalSDCardPath(context))&&checkMounted(context);
    }
    public static double getAvailableBlock(String path) {
        if (!TextUtils.isEmpty(path)) {
            try {
                android.os.StatFs f = new android.os.StatFs(path);
                long blockSize = f.getBlockSizeLong();//文件系统中每个存储区块的字节数
                long availableBlocks = f.getAvailableBlocksLong();//文件系统中可被应用程序使用的空闲存储区块的数量
                double sd2 = (blockSize * availableBlocks * 1.0) / 1024 / 1024 / 1024;
                String availableBlocks1 = String.valueOf(sd2);
                return Double.valueOf(availableBlocks1.substring(0, 3));
            } catch (Exception e) {
                return 0;
            }
        }
        return 0f;
    }
    /**
     * 检查内存是否够用，不够删除文件
     */
    public static void checkSDAvailableBlock(final Context context) {
        if (!canWrite(context)) return;
            Log.d(TAG, "getAvailableBlock: " + getAvailableBlock(externalSDCardPath(context)));
            if (getAvailableBlock(externalSDCardPath(context)) >0 &&getAvailableBlock(externalSDCardPath(context)) < 1) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        delAllFile(context, externalSDCardPath(context)+File.separator+"mp4");
//                        delAllFile(context,externalSDCardPath(context)+File.separator+"m4a");
                    }
                }).start();
        }
    }

    /**
     * 删除指定文件夹下所有文件
     * @param path 文件夹完整绝对路径
     */
    public static boolean delAllFile(Context context,String path) {

        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;// 第一個子文件夾
        int length = tempList.length;
        if (path.endsWith(File.separator)) {
            temp = new File(path + tempList[0]);
        } else {
            temp = new File(path + File.separator + tempList[0]);
        }
        if (length>1){
            if (delFile(temp.getAbsolutePath())) {
                delFile(externalSDCardPath(context) + File.separator + "m4a" + File.separator + temp.getName());
                MovieDao.getInstance(context).deleteForDate(temp.getName());
                Log.d(TAG, "delAllFile: " + temp.getName());
            }
        }else {
            clearFileDirectory(context,temp.getAbsolutePath());
        }
        return flag;
    }
    /**
     * 删除文件或文件夹所有文件，包括文件夹
     * @return
     */
    public static boolean delFile(String filePath) {
        File fileDirectory = new File(filePath);
        if (fileDirectory.isDirectory()) {
            String[] files = fileDirectory.list();
            boolean isSuccess = true;
            if (files != null) {
                for (String file : files) {
                    boolean del = delFile(filePath + File.separator + file);
                    if (!del) {
                        isSuccess = false;
                    }
                }
            }
            isSuccess = fileDirectory.delete() && isSuccess;
            return isSuccess;
        } else {

            return fileDirectory.delete();
        }
    }
    /**
     * 删除文件夹下文件，不删除自己
     *
     * @param filePath
     * @return
     */
    private static void clearFileDirectory(Context context, String filePath) {
        File fileDirectory = new File(filePath);
        if (fileDirectory.isDirectory()) {
            String[] files = fileDirectory.list();
            if (files != null) {
                int len = files.length;
                if (len>=10) {
                    len = 5;
                }else if (len<5&& len>2){
                    len = 2;
                }
                for (int i =0;i<len-1;i++){
                    clearFileDirectory(context,filePath + File.separator + files[i]);
                }
            }
        } else {
            //刪除
//            File audio = new File(MovieDao.getInstance(context).searchAudioForVideo(fileDirectory.getAbsolutePath()));
            if (fileDirectory.delete()) {
//                audio.delete();
                //刪除數據庫
                MovieDao.getInstance(context).deleteForPath(fileDirectory.getAbsolutePath());
            }
        }
    }
    public static String getDateStr(Date date, String format) {
        if (format == null || format.isEmpty()) {
            format = "yyyyMMdd";
        }
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);

    }
//    public static String getAudioPath(Context context) {
//        if (!canWrite(context)) return "";
//            File Folder = new File(externalSDCardPath(context)+File.separator+"m4a", Preferences.year+Preferences.month+Preferences.day);
//            if (!Folder.exists())//判断文件夹是否存在，不存在则创建文件夹
//            {
//                Folder.mkdirs();//创建文件夹
//            }
//            return Folder + File.separator ;
//    }
//    public static String getVideoPath(Context context) {
//        if (!canWrite(context)) return "";
//        File Folder = new File(externalSDCardPath(context)+File.separator+"mp4", Preferences.year+Preferences.month +Preferences.day);
//        if (!Folder.exists())//判断文件夹是否存在，不存在则创建文件夹
//        {
//            Folder.mkdirs();//创建文件夹
//        }
//        return Folder + File.separator;
//    }
    public static String getApkPath(Context context) {
        if (TextUtils.isEmpty(externalSDCardPath(context))) return "";
        //解析output.json 获取apk名称
        String json = FileUtil.readTextFile(externalSDCardPath(context)+File.separator+"output.json");
        try {
            JSONArray output =  new JSONArray(json);
            if (output.length()<0)return "";
            JSONObject object = output.optJSONObject(0);
            return externalSDCardPath(context)+File.separator+object.optString("path");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取sd卡中apk版本
     * @param context
     * @return
     */
    public static String getOutputVersion(Context context) {
        if (TextUtils.isEmpty(externalSDCardPath(context))) return "";
        //解析output.json 获取版本号
        String json = FileUtil.readTextFile(externalSDCardPath(context)+File.separator+"output.json");
        try {
            JSONArray output =  new JSONArray(json);
            if (output.length()<0)return "";
            JSONObject object = output.optJSONObject(0);
            JSONObject apkInfo = object.optJSONObject("apkInfo");
            return apkInfo.optString("versionName");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }
    /**
     * 获取系统版本
     * @param context
     * @return
     */
    public static String getVersion(Context context) {

        return FileUtil.readTextFile("/data/version.txt");
    }


    /**
     * 通过反射调用获取内置存储和外置sd卡根路径(通用)
     *
     * @param mContext    上下文
     * @param is_removale 是否可移除，false返回内部存储路径，true返回外置SD卡路径
     * @return
     */
    public static String getStoragePath(Context mContext, boolean is_removale) {
        String path = "";
        //使用getSystemService(String)检索一个StorageManager用于访问系统存储功能。
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);

            for (int i = 0; i < Array.getLength(result); i++) {
                Object storageVolumeElement = Array.get(result, i);
                path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    return path;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }
}