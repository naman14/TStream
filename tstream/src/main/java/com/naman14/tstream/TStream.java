package com.naman14.tstream;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Created by naman on 16/02/16.
 */
public class TStream {

    private static final String TAG = "TStream";
    private long cbSkip;
    private boolean seekRequest;
    private TServer mServer;

    public String ACTION_PLAY = "play";
    public String ACTION_PAUSE = "pause";

    public TStream(File file) {
        mServer = new TServer(file, this);
        String deviceIp = Utils.getIPAddress(true);
        mServer.init(deviceIp);
    }

    public TStream(File file, MediaObject mediaObject) {
        mServer = new TServer(file, this, mediaObject);
        String deviceIp = Utils.getIPAddress(true);
        mServer.init(deviceIp);
    }

    public void startStreaming() {
        mServer.start();
    }

    /*
    * Sends the HTTP response to the client, including headers (as applicable)
    * and content.
    */
    protected void processRequest(ExternalResourceDataSource dataSource,
                                  Socket client) throws IllegalStateException, IOException {
        if (dataSource == null) {
            Log.e(TAG, "Invalid (null) resource.");
            client.close();
            return;
        }
        InputStream is = client.getInputStream();
        final int bufsize = 8192;
        byte[] buf = new byte[bufsize];
        int splitbyte = 0;
        int rlen = 0;
        {
            int read = is.read(buf, 0, bufsize);
            while (read > 0) {
                rlen += read;
                splitbyte = findHeaderEnd(buf, rlen);
                if (splitbyte > 0)
                    break;
                read = is.read(buf, rlen, bufsize - rlen);
            }
        }

        ByteArrayInputStream hbis = new ByteArrayInputStream(buf, 0, rlen);
        BufferedReader hin = new BufferedReader(new InputStreamReader(hbis));
        Properties pre = new Properties();
        Properties parms = new Properties();
        Properties header = new Properties();

        try {
            decodeHeader(hin, pre, parms, header);
        } catch (InterruptedException e1) {
            Log.e(TAG, "Exception: " + e1.getMessage());
            e1.printStackTrace();
        }
        for (Map.Entry<Object, Object> e : header.entrySet()) {
            Log.e(TAG, "Header: " + e.getKey() + " : " + e.getValue());
        }
        String range = header.getProperty("range");
        cbSkip = 0;
        seekRequest = false;
        if (range != null) {
            Log.e(TAG, "range is: " + range);
            seekRequest = true;
            range = range.substring(6);
            int charPos = range.indexOf('-');
            if (charPos > 0) {
                range = range.substring(0, charPos);
            }
            cbSkip = Long.parseLong(range);
            Log.e(TAG, "range found!! " + cbSkip);
        }
        String headers = "";
        // Log.e(TAG, "is seek request: " + seekRequest);
        if (seekRequest) {// It is a seek or skip request if there's a Range
            // header
            headers += "HTTP/1.1 206 Partial Content\r\n";
            headers += "Content-Type: " + dataSource.getContentType() + "\r\n";
            headers += "Accept-Ranges: bytes\r\n";
            headers += "Content-Length: " + dataSource.getContentLength(false)
                    + "\r\n";
            headers += "Content-Range: bytes " + cbSkip + "-"
                    + dataSource.getContentLength(true) + "/*\r\n";
            headers += "\r\n";
        } else {
            headers += "HTTP/1.1 200 OK\r\n";
            headers += "Content-Type: " + dataSource.getContentType() + "\r\n";
            headers += "Accept-Ranges: bytes\r\n";
            headers += "Content-Length: " + dataSource.getContentLength(false)
                    + "\r\n";
            headers += "\r\n";
        }

        InputStream data = null;
        try {
            data = dataSource.createInputStream();
            byte[] buffer = headers.getBytes();
            Log.e(TAG, "writing to client");
            client.getOutputStream().write(buffer, 0, buffer.length);

            // Start sending content.

            byte[] buff = new byte[1024 * 50];
            Log.e(TAG, "No of bytes skipped: " + data.skip(cbSkip));
            int cbSentThisBatch = 0;
            while (mServer.isRunning()) {
                int cbRead = data.read(buff, 0, buff.length);
                if (cbRead == -1) {
                    Log.e(TAG,
                            "readybytes are -1 and this is simulate streaming, close the ips and crate anotber  ");
                    data.close();
                    data = dataSource.createInputStream();
                    cbRead = data.read(buff, 0, buff.length);
                    if (cbRead == -1) {
                        Log.e(TAG, "error in reading bytess**********");
                        throw new IOException(
                                "Error re-opening data source for looping.");
                    }
                }
                client.getOutputStream().write(buff, 0, cbRead);
                client.getOutputStream().flush();
                cbSkip += cbRead;
                cbSentThisBatch += cbRead;

            }
            Log.e(TAG, "cbSentThisBatch: " + cbSentThisBatch);
            // If we did nothing this batch, block for a second
            if (cbSentThisBatch == 0) {
                Log.e(TAG, "Blocking until more data appears");
                Thread.sleep(1000);
            }
        } catch (SocketException e) {
            // Ignore when the client breaks connection
            Log.e(TAG, "Ignoring " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Error getting content stream.", e);
        } catch (Exception e) {
            Log.e(TAG, "Error streaming file content.", e);
        } finally {
            if (data != null) {
                data.close();
            }
            client.close();
        }
    }

    /**
     * Decodes the sent headers and loads the data into java Properties' key -
     * value pairs
     **/
    private void decodeHeader(BufferedReader in, Properties pre,
                              Properties parms, Properties header) throws InterruptedException {
        try {
            // Read the request line
            String inLine = in.readLine();
            if (inLine == null)
                return;
            StringTokenizer st = new StringTokenizer(inLine);
            if (!st.hasMoreTokens())
                Log.e(TAG,
                        "BAD REQUEST: Syntax error. Usage: GET /example/file.html");

            String method = st.nextToken();
            pre.put("method", method);

            if (!st.hasMoreTokens())
                Log.e(TAG,
                        "BAD REQUEST: Missing URI. Usage: GET /example/file.html");

            String uri = st.nextToken();

            // Decode parameters from the URI
            int qmi = uri.indexOf('?');
            if (qmi >= 0) {
                decodeParms(uri.substring(qmi + 1), parms);
                uri = decodePercent(uri.substring(0, qmi));
            } else
                uri = decodePercent(uri);

            if (st.hasMoreTokens()) {
                String line = in.readLine();
                while (line != null && line.trim().length() > 0) {
                    int p = line.indexOf(':');
                    if (p >= 0)
                        header.put(line.substring(0, p).trim().toLowerCase(),
                                line.substring(p + 1).trim());
                    line = in.readLine();
                }
            }

            pre.put("uri", uri);
        } catch (IOException ioe) {
            Log.e(TAG,
                    "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
        }
    }


    private void decodeParms(String parms, Properties p)
            throws InterruptedException {
        if (parms == null)
            return;

        StringTokenizer st = new StringTokenizer(parms, "&");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            if (sep >= 0)
                p.put(decodePercent(e.substring(0, sep)).trim(),
                        decodePercent(e.substring(sep + 1)));
        }
    }

    /**
     * Find byte index separating header from body. It must be the last byte of
     * the first two sequential new lines.
     **/
    private int findHeaderEnd(final byte[] buf, int rlen) {
        int splitbyte = 0;
        while (splitbyte + 3 < rlen) {
            if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n'
                    && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n')
                return splitbyte + 4;
            splitbyte++;
        }
        return 0;
    }


    private String decodePercent(String str) throws InterruptedException {
        try {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                switch (c) {
                    case '+':
                        sb.append(' ');
                        break;
                    case '%':
                        sb.append((char) Integer.parseInt(
                                str.substring(i + 1, i + 3), 16));
                        i += 2;
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "BAD REQUEST: Bad percent-encoding.");
            return null;
        }
    }

    public String getStreamUrl() {
        return mServer.getFileUrl();
    }

    public void seekTo(float position) {
        mServer.seek(position);
    }

    public void setOnSeekListener(SeekListener listener) {
        mServer.setSeekListener(listener);
    }

    public void setActionCallback(ActionCallback callback) {

    }

    public void sendCommand(String command) {
        mServer.sendCommand(command);
    }


    public void updateMediaObject(MediaObject object) {

    }

    public interface SeekListener {
        void onSeekChanged(float position);
    }

    public interface ActionCallback {
        void onCommandReceived(String command);
    }


}
