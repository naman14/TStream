package com.naman14.tstream.app;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.WebSocket;
import com.naman14.tstream.ClientConnectedListener;
import com.naman14.tstream.TClient;
import com.naman14.tstream.TServer;
import com.naman14.tstream.TStream;

public class MainActivity extends AppCompatActivity {

    TServer server;
    TClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        server = TServer.getInstance();
        client = TClient.getInstance();

        server.startServer();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                client.setupClient(new ClientConnectedListener() {
                    @Override
                    public void clientConnected(WebSocket ws) {

                        byte[] encodedBytes = TStream.getInstance(MainActivity.this).encodeMp3(R.raw.test);
                        server.sendByteArray(encodedBytes);

                        ws.setStringCallback(new WebSocket.StringCallback() {
                            @Override
                            public void onStringAvailable(String s) {

                            }
                        });

                        ws.setDataCallback(new DataCallback() {
                            @Override
                            public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {

                                byte[] encodedBytes = bb.getAllByteArray();
                                byte[] decodedBytes = TStream.getInstance(MainActivity.this).decodeMp3(encodedBytes);

                                TStream.getInstance(MainActivity.this).playMp3(decodedBytes);
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
