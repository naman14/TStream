package com.naman14.tstream;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

/**
 * Created by naman on 24/01/16.
 */
public class TClient {

    private static String TAG = "TClient";

    public static TClient getInstance() {
        return new TClient();
    }

    public void setupClient(final ClientConnectedListener listener) {

        AsyncHttpClient.getDefaultInstance().websocket("ws://" + "192.168.0.107:5000/connect", null, new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                    listener.clientConnected(webSocket);
            }
        });
    }
}
