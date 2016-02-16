package com.naman14.tstream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ExternalResourceDataSource {

    private FileInputStream inputStream;
    private final File fileResource;
    long contentLength;

    public ExternalResourceDataSource(File resource) {
        fileResource = resource;
    }

    public String getContentType() {
        return "audio/mpeg";
    }


    public InputStream createInputStream() throws IOException {
        getInputStream();
        return inputStream;
    }


    public long getContentLength(boolean ignoreSimulation) {
        if (!ignoreSimulation) {
            return -1;
        }
        return contentLength;
    }

    private void getInputStream() {
        try {
            inputStream = new FileInputStream(fileResource);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}