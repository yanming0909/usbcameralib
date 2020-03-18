package com.serenegiant.usb.mydb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class MovieDao {
    private SQLiteOpenHelper helper;
    private Context context;
    private static MovieDao movieDao;

    public MovieDao(Context context) {
        this.context = context;
        helper = new MySqliteHelper(context, "batong.db", null, 1);

    }

    public static MovieDao getInstance(Context context) {
        if (movieDao == null) {
            movieDao = new MovieDao(context);
        }
        return movieDao;
    }

   /**删除前N条记录
    */
    public void delete(int  count){
    String sql="DELETE FROM  "+ "t_movie"+"  where  "+"id"+ " in (SELECT "+"id"+" FROM "+ "t_movie"+" order by "+"id"+"  limit "+count+")";
    SQLiteDatabase db=helper.getWritableDatabase();
    db.execSQL(sql);
    }

    /**
     * 删除指定日期数据
     *
     * @param currentdate 指定日期
     */
    public void deleteForDate(String currentdate) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction(); // 手动设置开始事务
        //批量删除
        String where = "currentdate = ?";
        db.delete("t_movie", where, new String[]{currentdate});
        db.setTransactionSuccessful(); // 设置事务处理成功，不设置会自动回滚不提交
        db.endTransaction(); // 处理完成
        db.close();
    }
    /**
     * 删除指定視頻文件数据
     *
     * @param path
     */
    public void deleteForPath(String path) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction(); // 手动设置开始事务
        //批量删除
        String where = "path = ?";
        db.delete("t_movie", where, new String[]{path});
        db.setTransactionSuccessful(); // 设置事务处理成功，不设置会自动回滚不提交
        db.endTransaction(); // 处理完成
        db.close();
    }
    /**
     * 根據視頻地址查找語音
     *
     * @param path
     */
    public String searchAudioForVideo(String path) {
        String audioPath="";
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction(); // 手动设置开始事务
        String[] columns = new String[]{"audio_path"};
        String selection = "path = ?";
        String[] selectionArgs= new String[]{path};
        Cursor cursor = db.query("t_movie",columns,selection,selectionArgs,null,null,null);
        while (cursor.moveToNext()) {
            audioPath = cursor.getString(0); //获取第一列的值,第一列的索引从0开始
        }
        cursor.close();
        db.setTransactionSuccessful(); // 设置事务处理成功，不设置会自动回滚不提交
        db.endTransaction(); // 处理完成
        db.close();
        return audioPath;
    }
    /**
     * 修改视频数据
     *
     * @param oldPath
     * @param newPath
     */
    public void updateVideoPath(String oldPath,String newPath) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction(); // 开始事务
        //删除
        String where = "path = ?";
        ContentValues contentValues = new ContentValues();
        contentValues.put("path", newPath);
        db.update("t_movie",contentValues, where, new String[]{oldPath});
        db.setTransactionSuccessful(); // 设置事务处理成功，不设置会自动回滚不提交
        db.endTransaction(); // 处理完成
        db.close();
    }
    public void insert(Movie stu) {

        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("insert into t_movie(path, currentdate,file_name,audio_path) values(?,?,?,?)",
                new Object[]{stu.getPath(), stu.getCurrentdate(), stu.getFile_name(), stu
                        .getAudio_path()});

        db.close();

    }
    /**
     * 查询前N条数据
     * @return List<Movie>
     */

    public List<Movie> getMovies(int count) {
        List<Movie> list = new ArrayList<Movie>();
        String sql2="SELECT * FROM "+ "t_movie"+" order by "+"id"+"  limit "+count;
        SQLiteDatabase db= helper.getReadableDatabase();
        Cursor cursor=db.rawQuery(sql2, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Movie stu = new Movie(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4));
                list.add(stu);
            }
            cursor.close();
        }
        return list;
    }
    /**
     * 查询所有数据
     * @return List<Movie>
     */

    public List<Movie> getAll() {

        List<Movie> list = new ArrayList<Movie>();

        SQLiteDatabase db = helper.getWritableDatabase();

        Cursor cursor = db.rawQuery("select id,path,currentdate,file_name,audio_path from " +
                "t_movie", null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Movie stu = new Movie(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4));
                list.add(stu);
            }
            cursor.close();

        }
        return list;

    }
}
