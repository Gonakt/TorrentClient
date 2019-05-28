package com.example.torrent;

import android.content.Context;
import android.media.Image;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;
import java.util.Vector;

public class MyTorrentAdapter extends ArrayAdapter {

    private LayoutInflater inflater;
    private int layout;
    private List<MyTorrent> torrents;

    public MyTorrentAdapter(Context context, int resource, List<MyTorrent> torrents) {

        super(context, resource, torrents);
        this.torrents = torrents;
        this.layout = resource;
        this.inflater = LayoutInflater.from(context);
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View view = inflater.inflate(this.layout, parent, false);

        ImageView actionView = view.findViewById(R.id.action);
        TextView name = view.findViewById(R.id.name);
        ProgressBar pb = view.findViewById(R.id.progress_bar);
        TextView status = view.findViewById(R.id.status);
        TextView progress = view.findViewById(R.id.progress);
        TextView speed = view.findViewById(R.id.speed);

        MyTorrent torrent = torrents.get(position);

        actionView.setImageResource(torrent.getmanage());
        actionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Vector<TorrentManagement> control = MainActivity.getManagement();
                boolean check_launched = control.get(position).getLaunched();
                check_launched = !check_launched;
                control.get(position).setLaunched(check_launched);
                Log.d("MSG", "Clicked");
            }
        });
        name.setText(torrent.getNameOfTorrent());
        pb.setProgress(torrent.getPercentProgress());
        status.setText(torrent.getStatus());
        progress.setText(torrent.getProgress());
        speed.setText(torrent.getSpeed());

        return view;
    }

}
