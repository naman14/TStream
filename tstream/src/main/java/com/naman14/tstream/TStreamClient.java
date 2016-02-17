package com.naman14.tstream;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by naman on 17/02/16.
 */
public class TStreamClient {

    private List<WebSocket> _clientSockets = new ArrayList<>();
    private SeekListener seekListener;
    private ActionCallback actionCallback;

    public interface ClientConnectedListener {
        void clientConnected();

        void clientConnectionFailed();
    }

    public interface MediaListener {
        void onMediaAvailable(MediaObject mediaObject);

        void onPlaylistAvailable(List<MediaObject> playlist);
    }

    public void connectClient(String ip, MediaListener mediaListener) {
        setupClient(ip, mediaListener, null);
    }

    public void connectClient(String ip, MediaListener mediaListener, ClientConnectedListener listener) {
        setupClient(ip, mediaListener, listener);
    }

    public void setupClient(String ip, final MediaListener mediaListener, final ClientConnectedListener listener) {

        AsyncHttpClient.getDefaultInstance().websocket("ws://" + ip + ":5000/connect", null, new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                if (ex == null) {
                    listener.clientConnected();
                    _clientSockets.add(webSocket);

                    webSocket.setStringCallback(new WebSocket.StringCallback() {
                        @Override
                        public void onStringAvailable(String s) {

                            if (s.contains("MEDIA")) {
                                try {
                                    JSONObject object = new JSONObject(s.substring(8, s.length()));
                                    MediaObject mediaObject = new MediaObject();
                                    mediaObject.setTitle(object.getString("title"));
                                    mediaObject.setAuthor(object.getString("author"));
                                    mediaObject.setUrl(object.getString("URL"));
                                    mediaListener.onMediaAvailable(mediaObject);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (s.contains("SEEK") && seekListener != null) {
                                seekListener.onSeekChanged(Float.parseFloat(s.substring(10, s.length())));
                            }
                            actionCallback.onCommandReceived(s);

                        }
                    });

                } else {
                    listener.clientConnectionFailed();
                }
            }
        });
    }

    public void seek(float position) {
        for (WebSocket socket : _clientSockets)
            socket.send("SEEK TO : " + position);
    }

    public void sendCommand(String command) {
        for (WebSocket socket : _clientSockets)
            socket.send(command);
    }

    public void setOnSeekListener(SeekListener listener) {
        this.seekListener = listener;
    }


    public void setActionCallback(ActionCallback callback) {
        this.actionCallback = callback;
    }


    protected void updateMediaObject(MediaObject object) {

    }


    public interface SeekListener {
        void onSeekChanged(float position);
    }

    public interface ActionCallback {
        void onCommandReceived(String command);
    }

}
