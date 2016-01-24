package com.naman14.tstream;

import com.koushikdutta.async.http.WebSocket;

/**
 * Created by naman on 25/01/16.
 */
public interface ClientConnectedListener {

    public void clientConnected(WebSocket ws);
}
