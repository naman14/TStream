package com.naman14.tstream.app;

import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.naman14.tstream.MediaObject;
import com.naman14.tstream.TStream;
import com.naman14.tstream.TStreamClient;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by naman on 17/02/16.
 */
public class MusicStreamActivity extends AppCompatActivity {

    private MediaPlayer mMediaPlayer;
    private String[] mMusicList;
    private String[] mPathList;
    private EditText serverIp;
    private String path;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_stream);
        mMediaPlayer = new MediaPlayer();

        ListView mListView = (ListView) findViewById(R.id.listView1);
        serverIp = (EditText) findViewById(R.id.serverip);

        mMusicList = getMusic();

        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mMusicList);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                path = mPathList[arg2];
                File file = new File(path);

                if (file.exists()) {

                    MediaObject object = new MediaObject();
                    TStream stream = new TStream(file, object);
                    stream.startStreaming();
                } else {
                    Toast.makeText(MusicStreamActivity.this,"File does not exist",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String[] getMusic() {
        final Cursor mCursor = managedQuery(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media.DISPLAY_NAME,MediaStore.Audio.Media.DATA}, null, null,
                "LOWER(" + MediaStore.Audio.Media.TITLE + ") ASC");

        int count = mCursor.getCount();

        String[] songs = new String[count];
        mPathList = new String[count];

        int i = 0;
        if (mCursor.moveToFirst()) {
            do {
                songs[i] = mCursor.getString(0);
                mPathList[i] = mCursor.getString(1);
                i++;
            } while (mCursor.moveToNext());
        }

        mCursor.close();

        return songs;
    }

    public void connectClient(View view) {
        TStreamClient client = new TStreamClient();
        client.connectClient(serverIp.getText().toString(), new TStreamClient.MediaListener() {
            @Override
            public void onMediaAvailable(MediaObject mediaObject) {
                try {
                    mMediaPlayer.reset();
                    mMediaPlayer.setDataSource(mediaObject.getUrl());
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPlaylistAvailable(List<MediaObject> playlist) {

            }
        });


    }
}
