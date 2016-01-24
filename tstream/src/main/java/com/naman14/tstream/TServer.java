package com.naman14.tstream;

import android.util.Log;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by naman on 24/01/16.
 */
public class TServer {

    private static String TAG = "TServer";

    private List<WebSocket> _sockets = new ArrayList<>();

    public static TServer getInstance() {
        return new TServer();
    }

    public void startServer() {
        AsyncHttpServer server = new AsyncHttpServer();

        server.websocket("/connect", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(WebSocket webSocket, AsyncHttpServerRequest request) {
                _sockets.add(webSocket);

                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        try {
                            if (ex != null)
                                ex.printStackTrace();
                        } finally {
                            _sockets.clear();
                        }
                    }
                });

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        Log.d(TAG, s);
                    }
                });

                webSocket.setDataCallback(new DataCallback() {
                    @Override
                    public void onDataAvailable(DataEmitter dataEmitter, ByteBufferList byteBufferList) {
                        byteBufferList.recycle();
                    }
                });

            }
        });

        server.listen(5000);
    }
}
