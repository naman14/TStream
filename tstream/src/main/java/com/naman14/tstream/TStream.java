package com.naman14.tstream;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by naman on 24/01/16.
 */
public class TStream {

    private static Context mContext;

    public static TStream getInstance(Context context) {
        mContext = context;
        return new TStream();
    }

    public byte[] encodeMp3(int Rid) {
        try {
            InputStream byteInputStream = mContext.getResources().openRawResource(Rid);
            byte[] encodedFile = Base64.encode(inputStreamToByteArray(byteInputStream), Base64.DEFAULT);

            return encodedFile;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    public byte[] inputStreamToByteArray(InputStream inStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inStream.read(buffer)) > 0) {
            baos.write(buffer, 0, bytesRead);
        }

        return baos.toByteArray();
    }


    public byte[] decodeMp3(byte[] encodedBytes) {
        byte[] decodeFile = Base64.decode(encodedBytes, Base64.DEFAULT);

        return decodeFile;
    }


    public void playMp3(byte[] mp3SoundByteArray) {
        try {
            // create temp file that will hold byte array
            File tempMp3 = File.createTempFile("abba", ".mp3", Environment.getExternalStorageDirectory());
            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(mp3SoundByteArray);
            fos.close();

            MediaPlayer mediaPlayer = new MediaPlayer();

            FileInputStream fis = new FileInputStream(tempMp3);
            mediaPlayer.setDataSource(fis.getFD());

            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException ex) {
            String s = ex.toString();
            ex.printStackTrace();
        }
    }
}
