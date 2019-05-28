package com.example.torrent;

import android.view.View;
import android.widget.ImageView;

public class MyTorrent {

    private int manage;
    private String name;
    private int percent_progress;
    private String speed;
    private String progress;
    private String status;

    MyTorrent(String name, String speed, String progress, int percent_progress, String status, int manage) {
        this.name = name;
        this.speed = speed;
        this.progress = progress;
        this.percent_progress = percent_progress;
        this.status = status;
        this.manage = manage;
    }

    public int getmanage() {
        return manage;
    }

    public String getNameOfTorrent() {
        return name;
    }

    public String getProgress() {
        return progress;
    }

    public int getPercentProgress() {
        return percent_progress;
    }

    public String getStatus() {
        return status;
    }

    public String getSpeed() {
        return speed;
    }

}
