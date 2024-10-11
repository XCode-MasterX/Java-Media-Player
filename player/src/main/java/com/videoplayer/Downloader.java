package com.videoplayer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

class Downloader {
    public static String download(String downloadLink, String path) {
        if(path == null) return null;

        VideoPlayer inst = VideoPlayer.getInstance();
        String pathExt = inst.getFileExtension(path);
        
        if(pathExt == null) { path += "." + inst.getFileExtension(downloadLink); System.out.println("New path: " + path); }

        pathExt = inst.getFileExtension(path);

        if(!pathExt.equals(inst.getFileExtension(downloadLink))) {
            path = path.substring(0, path.lastIndexOf(".") + 1) + inst.getFileExtension(downloadLink);
            pathExt = inst.getFileExtension(path);
        }

        if(!(downloadLink.contains("http:") || downloadLink.contains("https:")))
            downloadLink = new File(downloadLink).toURI().toString();

        System.out.println(downloadLink + " " + path);

        File saveFile = new File(path);
        FileOutputStream fos;

        try{
            URL link = new URL(downloadLink);
            InputStream is = link.openStream();
            BufferedInputStream linkReader = new BufferedInputStream(is);
            fos = new FileOutputStream(saveFile);

            fos.write(linkReader.readAllBytes());
            fos.flush();
            fos.close();
            linkReader.close();
        }
        catch(IOException e) {
            e.printStackTrace();
            System.out.println("Error occured during download: " + e);
        }

        return saveFile.getAbsolutePath();
    }
}