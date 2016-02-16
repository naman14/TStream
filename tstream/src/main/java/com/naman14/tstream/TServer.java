package com.naman14.tstream;

import android.util.Log;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class TServer implements Runnable {

    private static final String TAG = "TServer";
    private int port = 0;
    private boolean isRunning = false;
    private ServerSocket socket;
    private AsyncHttpServer server;
    private List<WebSocket> _sockets = new ArrayList<>();
    private TStream.SeekListener seekListener;
    private TStream.ActionCallback actionCallback;
    private TStream stream;
    private Thread thread;
    private File mStreamFile;
    private MediaObject mediaObject = new MediaObject();


    public TServer(File file, TStream stream) {
        mStreamFile = file;
        this.stream = stream;
    }

    public TServer(File file, TStream stream, MediaObject object) {
        this(file, stream);
        this.mediaObject = object;
    }

    public int getPort() {
        return port;
    }

    public String init(String ip) {
        String url = null;
        try {
            InetAddress inet = InetAddress.getByName(ip);
            byte[] bytes = inet.getAddress();
            socket = new ServerSocket(port, 0, InetAddress.getByAddress(bytes));
            server = new AsyncHttpServer();
            socket.setSoTimeout(10000);
            port = socket.getLocalPort();
            url = "http://" + socket.getInetAddress().getHostAddress() + ":"
                    + port;
            Log.e(TAG, "Server started at " + url);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Error UnknownHostException server", e);
        } catch (IOException e) {
            Log.e(TAG, "Error IOException server", e);
        }
        return url;
    }

    public String getFileUrl() {
        return "http://" + socket.getInetAddress().getHostAddress() + ":"
                + port + "/" + mStreamFile.getName();
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
        isRunning = true;
        setupWebSocket();
    }

    public void stop() {
        isRunning = false;
        if (thread == null) {
            Log.e(TAG, "Server was stopped without being started.");
            return;
        }
        Log.e(TAG, "Stopping server.");
        thread.interrupt();
    }


    public boolean isRunning() {
        return isRunning;
    }


    @Override
    public void run() {
        Log.e(TAG, "running");
        while (isRunning) {
            try {
                Socket client = socket.accept();
                if (client == null) {
                    continue;
                }
                Log.e(TAG, "client connected at " + port);
                ExternalResourceDataSource data = new ExternalResourceDataSource(
                        mStreamFile);
                Log.e(TAG, "processing request...");
                stream.processRequest(data, client);
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "No client connected, waiting for client...", e);
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to client", e);
            }
        }
        Log.e(TAG, "Server interrupted or stopped. Shutting down.");
    }

    private void setupWebSocket() {
        server.websocket("/connect", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
                _sockets.add(webSocket);

                try {
                    JSONObject object = new JSONObject();

                    object.put("title", mediaObject.getTitle());
                    object.put("author", mediaObject.getAuthor());
                    object.put("URL", getFileUrl());

                    webSocket.send("MEDIA : " + object.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Use this to clean up any references to your websocket
                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        try {
                            if (ex != null)
                                Log.e("WebSocket", "Error");
                        } finally {
                            _sockets.remove(webSocket);
                        }
                    }
                });

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        if (s.contains("SEEK TO : ")) {
                            seekListener.onSeekChanged(Float.parseFloat(s.substring(10, s.length())));
                        }
                        actionCallback.onCommandReceived(s);
                    }
                });

            }
        });

        server.listen(5000);

    }

    public void seek(float position) {
        for (WebSocket socket : _sockets)
            socket.send("SEEK TO : " + position);
    }

    public void setSeekListener(TStream.SeekListener listener) {
        this.seekListener = listener;
    }

    public void setActionCallback(TStream.ActionCallback callback) {
        this.actionCallback = callback;
    }

    public void sendCommand(String command) {
        for (WebSocket socket : _sockets)
            socket.send(command);
    }

    public void updateMedia(MediaObject mediaObject) {
        this.mediaObject = mediaObject;
        try {
            JSONObject object = new JSONObject();

            object.put("title", mediaObject.getTitle());
            object.put("author", mediaObject.getAuthor());
            object.put("URL", getFileUrl());

            for (WebSocket socket : _sockets)
                socket.send("MEDIA : " + object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}