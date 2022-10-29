package ru.nsu.fit.zolotorevskii.NetworkTech.lab2;

public class MessageUser {
    String fileName;
    long lengthFile;

    public MessageUser(String fileName, long lengthFile){
        this.fileName = fileName;
        this.lengthFile = lengthFile;
    }

    public long getLengthFile() {
        return lengthFile;
    }

    public String getFileName() {
        return fileName;
    }
}
