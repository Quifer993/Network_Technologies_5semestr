package ru.nsu.fit.zolotorevskii.NetworkTech.lab2;

import com.google.gson.Gson;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket clientSocket = new Socket();
        String filename = "ubuntu-22.04.1-desktop-amd64.iso";
//        String filename = "aa.txt";
        File f = new File("src/main/resources/" + filename);

        System.out.println("Sending " + f.getName() + "...");
        System.out.println("@FILE_SEND");
        System.out.println(f.getName());

        clientSocket.connect(new InetSocketAddress(Constants.PORT));
        try {
            long lengthFile = f.length();
            if (lengthFile > Math.pow(Constants.LENGTH_STEP,4)){
                System.out.println(lengthFile);
                return;
            }
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

            //-----First Message-----
            Gson gson = new Gson();
            MessageUser messageUser = new MessageUser(filename, lengthFile);
            String s = gson.toJson(messageUser);
            System.out.println(s);
            out.writeUTF(s);
            out.flush();

            //-----First Message was written-----
            byte[] byteArray = new byte[Constants.LENGTH_STEP];
            FileInputStream fis = new FileInputStream(f.getPath());

            long lengthDone = 0;
            while (lengthDone < lengthFile) {
                long i = fis.read(byteArray);
                out.write(byteArray);
                out.flush();
                lengthDone += i;
            }
            fis.close();
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.getStackTrace();
        } catch (IOException e) {
            e.getStackTrace();
        } catch (Exception e) {
            e.getStackTrace();
        }
    }
}