package com.example.torrent;

import android.content.res.Resources;
import android.util.Log;

import com.turn.ttorrent.client.Context;
import com.turn.ttorrent.client.PeerInformation;
import com.turn.ttorrent.client.PieceInformation;
import com.turn.ttorrent.client.SimpleClient;
import com.turn.ttorrent.client.TorrentListener;
import com.turn.ttorrent.client.TorrentManager;
import com.turn.ttorrent.common.TorrentFile;
import com.turn.ttorrent.common.TorrentMetadata;
import com.turn.ttorrent.common.TorrentParser;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.List;

public class TorrentManagement extends Thread {

    private String metadata_path;
    private String destination_path;
    private InetAddress address;

    private long downloaded = 0; //То, что класс получает из ini файла. Из этого только status и name нужно возвращать в MainActivity
    private String status;
    private long total_size = 0;
    private String name;
    private boolean launched;
    private int resource;

    private String progress; //Данные для передачи в MainActivity. В одной строке прогресс в весе и процентах
    private String speed; //Нужно сделать функции-геттеры для возвращения этих данных в MainActivity
    private ArrayDeque<Integer> speed_data = new ArrayDeque<Integer>();

    private SimpleClient client;
    private TorrentManager manager;
    private TorrentMetadata metadata;
    private TorrentParser parser;

    TorrentManagement(String metadata_path, String destination_path, InetAddress address) {
        this.metadata_path = metadata_path;
        this.destination_path = destination_path;
        this.address = address;

    }

    TorrentManagement(String metadata_path, String destination_path, InetAddress address, String status, long total_size, long downloaded) {
        this.metadata_path = metadata_path;
        this.destination_path = destination_path;
        this.address = address;
        this.status = status;
        this.total_size = total_size;
        this.downloaded = downloaded;

    }

    @Override
    public void run() {
        try {
            client = new SimpleClient();
            parser = new TorrentParser();
            metadata = parser.parseFromFile(new File(metadata_path)); //При попытке открыть торрент, папка с названием которого уже существует
            List<TorrentFile> files = metadata.getFiles();            //программа не кладет файлы в нее, а создает новые в родительской. Исправить
            String[] segments = destination_path.split("/");
            String temp_dp = segments[segments.length - 1];
            File folder = null;
            if (!temp_dp.equals(getTorrentName()))
                folder = new File(destination_path + "/" + metadata.getDirectoryName());
            else
                folder = new File(destination_path);
            if (files.size() > 1) {
                if (!folder.exists()) {
                    folder.mkdir();
                    destination_path = destination_path + "/" + metadata.getDirectoryName();
                }
            }
            if (status == null || status.equals("Раздается") || status.equals("Загружается") || status.equals("Парсинг метаданных...")) {
                launched = true;
                status = "Парсинг метаданных...";
                manager = client.downloadTorrentAsync(metadata_path, destination_path, address);
                status = "Загружается";
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        manager.addListener(buildListener());
    }

    private TorrentListener buildListener() {
        TorrentListener listener = new TorrentListener() {
            @Override
            public void peerConnected(PeerInformation peerInformation) {

            }

            @Override
            public void peerDisconnected(PeerInformation peerInformation) {

            }

            @Override
            public void pieceDownloaded(PieceInformation pieceInformation, PeerInformation peerInformation) {
                downloaded = downloaded + pieceInformation.getSize();
                speed_data.addFirst(pieceInformation.getSize());
            }

            @Override
            public void downloadComplete() {
                status = "Раздается";
                Log.d("END", "Загрузка завершена");
            }

            @Override
            public void pieceReceived(PieceInformation pieceInformation, PeerInformation peerInformation) {

            }

            @Override
            public void downloadFailed(Throwable throwable) {

            }

            @Override
            public void validationComplete(int i, int i1) {
                status = "Раздается";
            }
        };
        return listener;
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public String getTorrentName() {
        try {
            parser = new TorrentParser();
            File f = new File(metadata_path);
            metadata = parser.parseFromFile(f);
            List<TorrentFile> files = metadata.getFiles();
            if (files.size() == 1)
                name = new File(files.get(0).getRelativePathAsString()).getName();
            else
                name = metadata.getDirectoryName();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

    public String getStatus() {
        return status;
    }

    public long getTotalSize() {
        long size = 0;
        try {
            parser = new TorrentParser();
            metadata = parser.parseFromFile(new File(metadata_path));
            List<TorrentFile> files = metadata.getFiles();
            for (TorrentFile file : files)
                size = size + file.size;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    public String getProgress() {
            long size = getTotalSize();
            long ratio = (long) ((float) downloaded / size * 100);
            progress = humanReadableByteCount(downloaded, true) + "/" + humanReadableByteCount(size, true) + "   " + String.valueOf(ratio) + "%";
            return progress;

    }

    public String getMetadataPath() {
        return metadata_path;
    }

    public String getDestinationPath() {
        return destination_path;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public void stopClient() {
        client.stop();
    }

    public void deleteAllFiles() {
        try {
            parser = new TorrentParser();
            File f = new File(metadata_path);
            metadata = parser.parseFromFile(f);
            List<TorrentFile> files = metadata.getFiles();
            if (files.size() == 1) {
                File to_delete = new File(destination_path + "/" + getTorrentName());
                to_delete.delete();
            }
            else {
                File to_delete = new File(destination_path);
                to_delete.delete();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("MSG", "Deleted");
    }

    public String getSpeed() {
        long common_speed = 0;
        if (!speed_data.isEmpty()) {
            for (Integer item : speed_data) {
                common_speed = common_speed + item;
            }
            speed = humanReadableByteCount(common_speed, true) + "/s";
            speed_data.clear();
            return speed;
        }
        else
            return "0 B/s";
    }

    public int getPercentProgress() { //для прогресс-бара

        long size = getTotalSize();
        long ratio = (long) ((float) downloaded / size * 100);
        return (int) ratio;

    }

    public boolean getLaunched() {
        return launched;
    }

    public void setLaunched(boolean launched) {
        this.launched = launched;
        if (launched) {
            try {
                client.downloadTorrentAsync(metadata_path, destination_path, address); //После паузы загрузка заново не начинается
                status = "Загружается";
                Log.d("MSG", "Launched");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            status = "Пауза";
            client.stop();
            Log.d("MSG", "Stopped");
        }
    }

    public int getResource() {
        if (launched)
            resource = R.drawable.baseline_stop_black_48;
        else
            resource = R.drawable.baseline_play_arrow_black_48;
        return resource;
    }

}
