package com.example.torrent;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.obsez.android.lib.filechooser.ChooserDialog;
import com.turn.ttorrent.client.PeerInformation;
import com.turn.ttorrent.client.PieceInformation;
import com.turn.ttorrent.client.TorrentListener;
import com.turn.ttorrent.client.TorrentManager;

import org.apache.log4j.chainsaw.Main;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile;
import org.ini4j.Wini;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.net.ssl.ManagerFactoryParameters;


public class MainActivity extends AppCompatActivity {

    private static Vector<TorrentManagement> management = new Vector<>();
    private List<MyTorrent> torrents = new ArrayList();
    Toolbar toolbar;
    private ListView torrents_list;
    String metadata_path;
    String destination_path;
    Timer t;
    Timer r;
    private boolean delete_mode = false;
    private int last_item_clicked = 0;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.function_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_quit)
            quit();
        if (item.getItemId() == R.id.action_add)
            chooseFile();
        if (item.getItemId() == R.id.action_delete) {
            if (!delete_mode) {
                delete_mode = true;
                Toast.makeText(this, "Режим удаления включен. Кликните по торренту, который хотите удалить. Кликните на кнопку \"Удалить\" снова, чтобы отменить удаление", Toast.LENGTH_LONG).show();
                return true;
            }
            if (delete_mode) {
                delete_mode = false;
                Toast.makeText(this, "Режим удаления выключен", Toast.LENGTH_LONG).show();
                return true;
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        torrents_list = findViewById(R.id.torrent_list);
        MyTorrentAdapter start_adapter = new MyTorrentAdapter(getApplicationContext(), R.layout.mytorrent, torrents);
        torrents_list.setAdapter(start_adapter);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                    {
                        management.get(last_item_clicked).stopClient();
                        management.remove(last_item_clicked);
                        start_adapter.remove(start_adapter.getItem(last_item_clicked));
                        start_adapter.notifyDataSetChanged();
                    }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:

                        break;
                }
            }
        };

        torrents_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (delete_mode) {
                    last_item_clicked = position;
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Сообщение");
                    builder.setMessage("Вы уверены, что хотите удалить этот торрент?");
                    builder.setPositiveButton("Да", dialogClickListener);
                    builder.setNegativeButton("Нет", dialogClickListener);
                    builder.show();
                }
            }
        });


        File file = new File(getApplication().getFilesDir().getPath() + "/torrents_data.ini"); //Восстановление данных
        clearFile(file);
        if (file.exists() && (file.length() > 0)) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(getApplication().getFilesDir().getPath() + "/torrents_data.ini"));
                int lines = 0;
                while (reader.readLine() != null)
                    lines++;
                reader.close();

                IPGetter getter = new IPGetter();
                getter.start();
                try {
                    getter.join();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < lines; i = i + 7) {
                    Wini ini = new Wini(file);
                    String metadata_path = ini.get("Torrent " + String.valueOf(i / 7), "metadata_path", String.class);
                    String destination_path = ini.get("Torrent " + String.valueOf(i / 7), "destination_path", String.class);
                    String status = ini.get("Torrent " + String.valueOf(i / 7), "status", String.class);
                    long total_size = ini.get("Torrent " + String.valueOf(i / 7), "total_size", long.class);
                    long downloaded = ini.get("Torrent " + String.valueOf(i / 7), "downloaded", long.class);
                    TorrentManagement temp = new TorrentManagement(metadata_path, destination_path, getter.getValue(), status, total_size, downloaded);
                    resumeTorrent(temp);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        //print(file.getPath());

    }

    private void startTorrent() {
        IPGetter getter = new IPGetter();
        getter.start();
        try {
            getter.join();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        management.add(new TorrentManagement(metadata_path, destination_path, getter.getValue()));
        management.get(management.size() - 1).start();

        String name = management.get(management.size() - 1).getTorrentName();
        String speed = management.get(management.size() - 1).getSpeed();
        String progress = management.get(management.size() - 1).getProgress();
        int percent_progress = management.get(management.size() - 1).getPercentProgress();
        String status = management.get(management.size() - 1).getStatus();
        int resource = management.get(management.size() - 1).getResource();

        torrents.add(new MyTorrent(name, speed, progress, percent_progress, status, resource));

        t = new Timer();


        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        torrents_list = findViewById(R.id.torrent_list);
                        ListIterator<MyTorrent> torrent_iterator = torrents.listIterator();
                        ListIterator<TorrentManagement> management_iterator = management.listIterator();
                        while (torrent_iterator.hasNext() && management_iterator.hasNext()) {
                            String name = management_iterator.next().getTorrentName();
                            management_iterator.previous();
                            String speed = management_iterator.next().getSpeed();
                            management_iterator.previous();
                            String progress = management_iterator.next().getProgress();
                            management_iterator.previous();
                            int percent_progress = management_iterator.next().getPercentProgress();
                            management_iterator.previous();
                            String status = management_iterator.next().getStatus();
                            management_iterator.previous();
                            int resource = management_iterator.next().getResource();
                            torrent_iterator.next();
                            torrent_iterator.set(new MyTorrent(name, speed, progress, percent_progress, status, resource));

                        }
                        MyTorrentAdapter adapter = new MyTorrentAdapter(MainActivity.this, R.layout.mytorrent, torrents);
                        torrents_list.setAdapter(adapter);

                    }
                });
            }
        }, 0, 1000);
        torrents_list = findViewById(R.id.torrent_list);

    }

    private void resumeTorrent(TorrentManagement torrent) {
        management.add(torrent);
        management.get(management.size() - 1).start();

        String name = management.get(management.size() - 1).getTorrentName();
        String speed = management.get(management.size() - 1).getSpeed();
        String progress = management.get(management.size() - 1).getProgress();
        int percent_progress = management.get(management.size() - 1).getPercentProgress();
        String status = management.get(management.size() - 1).getStatus();
        int resource = management.get(management.size() - 1).getResource();

        torrents.add(new MyTorrent(name, speed, progress, percent_progress, status, resource));

        r = new Timer();
        r.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        torrents_list = findViewById(R.id.torrent_list);
                        ListIterator<MyTorrent> torrent_iterator = torrents.listIterator();
                        ListIterator<TorrentManagement> management_iterator = management.listIterator();
                        while (torrent_iterator.hasNext() && management_iterator.hasNext()) {
                            String name = management_iterator.next().getTorrentName();
                            management_iterator.previous();
                            String speed = management_iterator.next().getSpeed();
                            management_iterator.previous();
                            String progress = management_iterator.next().getProgress();
                            management_iterator.previous();
                            int percent_progress = management_iterator.next().getPercentProgress();
                            management_iterator.previous();
                            String status = management_iterator.next().getStatus();
                            management_iterator.previous();
                            int resource = management_iterator.next().getResource();
                            torrent_iterator.next();
                            torrent_iterator.set(new MyTorrent(name, speed, progress, percent_progress, status, resource));

                        }
                        MyTorrentAdapter adapter = new MyTorrentAdapter(MainActivity.this, R.layout.mytorrent, torrents);
                        torrents_list.setAdapter(adapter);
                        Log.d("MSG", "Adapter set");
                    }
                });

            }
        }, 0, 1000);
        torrents_list = findViewById(R.id.torrent_list);
    }

    public void chooseFile() {

        new ChooserDialog(MainActivity.this)
                .withStartFile(Environment.DIRECTORY_DOWNLOADS)
                .withFilter(false, false, "torrent")
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String s, File file) {
                        metadata_path = s;
                        chooseDirectory();
                    }
                })
                .build()
                .show();

    }

    public void chooseDirectory() {
        new ChooserDialog(MainActivity.this)
                .withFilter(true, false)
                .withStartFile(Environment.DIRECTORY_DOWNLOADS)
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String s, File file) {
                        destination_path = s;
                        startTorrent();
                    }
                })
                .build()
                .show();

    }

    public void quit() {
        for (int i = 0; i < management.size(); i++) {
            try {
                Wini ini = new Wini(new File(getApplication().getFilesDir().getPath() + "/torrents_data.ini"));
                String name = "Torrent " + String.valueOf(i);
                String metadata_path =  management.get(i).getMetadataPath();
                String destination_path = management.get(i).getDestinationPath();
                String status = management.get(i).getStatus();
                long total_size = management.get(i).getTotalSize();
                long downloaded = management.get(i).getDownloaded();
                ini.put(name, "metadata_path", metadata_path);
                ini.put(name, "destination_path", destination_path);
                ini.put(name, "status", status);
                ini.put(name, "total_size", total_size);
                ini.put(name, "downloaded", downloaded);
                ini.store();
                management.get(i).stopClient();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        finish();
    }

    private void print(String path) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            while (line != null) {
                Log.d("MSG", line);
                line = reader.readLine();
            }
            reader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearFile(File file) {
        try {
            PrintWriter writer = new PrintWriter(file);
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void linesNumber(String path) {
        int lines = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(getApplication().getFilesDir().getPath() + "/torrents_data.ini"));

            while (reader.readLine() != null)
                lines++;
            reader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("MSG", String.valueOf(lines));
    }

    public static Vector<TorrentManagement> getManagement() {
        return management;
    }

}