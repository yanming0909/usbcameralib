package com.serenegiant.usb.mydb;

public class Movie {
    private int id;

    private String path;

    private String currentdate;

    private String file_name;
    private String audio_path;
    public Movie(String path, String currentdate,String file_name,String audio_path) {
        this.path = path;
        this.currentdate = currentdate;
        this.file_name = file_name;
        this.audio_path = audio_path;
    }
    public Movie(int id,String path, String currentdate,String file_name,String audio_path) {
        this.id=id;
        this.path = path;
        this.currentdate = currentdate;
        this.file_name = file_name;
        this.audio_path = audio_path;
    }

    public Movie(String path, String currentdate,String file_name) {
        this.path = path;
        this.currentdate = currentdate;
        this.file_name = file_name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCurrentdate() {
        return currentdate;
    }

    public void setCurrentdate(String currentdate) {
        this.currentdate = currentdate;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getAudio_path() {
        return audio_path;
    }

    public void setAudio_path(String audio_path) {
        this.audio_path = audio_path;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "path='" + path + '\'' +
                ", currentdate='" + currentdate + '\'' +
                ", file_name='" + file_name + '\'' +
                ", audio_path='" + audio_path + '\'' +
                '}';
    }
}
