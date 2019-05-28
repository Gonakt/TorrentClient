package com.example.torrent;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

public class IPGetter extends Thread {

    private volatile InetAddress value;
    String public_ip;

    @Override
    public void run() {
        public_ip = null;
        try {
            URL url_name = new URL("http://bot.whatismyipaddress.com");
            BufferedReader sc = new BufferedReader(new InputStreamReader(url_name.openStream()));
            public_ip = sc.readLine().trim();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            value = InetAddress.getByName(public_ip);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public InetAddress getValue() {
        return value;
    }

}
