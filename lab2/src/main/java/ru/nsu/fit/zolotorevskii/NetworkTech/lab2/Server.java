package ru.nsu.fit.zolotorevskii.NetworkTech.lab2;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.net.*;
import java.util.*;

import static ru.nsu.fit.zolotorevskii.NetworkTech.lab2.Constants.*;

public class Server {
    private static final boolean NOT_NULL_BYTE = true;
    List<Socket> listSockets;
    public Map<Socket, Double> mapSpeed;
    List<String> filesNamesAlreadyExist;
    Map<Socket, String> mapUsers = new HashMap<Socket, String>();
    boolean serverWorking;
    double speedAll;


    Server() {
        listSockets = new ArrayList<>();
        mapSpeed = new HashMap<>();
        speedAll = 0;
        filesNamesAlreadyExist = new ArrayList<>();
        try {
            File dir = new File(URL_TO_UPLOADS);
            File[] files = dir.listFiles();
            assert files != null;
            for (File file : files) {
                if (file.isDirectory()) {
                    System.out.println("directory: " + file.getCanonicalPath());
                } else {
                    filesNamesAlreadyExist.add(file.getCanonicalPath());
                    System.out.println("file already exist in folder: " + file.getName());
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }catch (IOException e ) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.startServer();
    }

    public void startServer(){
        serverWorking = true;

        try {
            ServerSocket server = new ServerSocket(PORT);
            System.out.println("Server socket created");

            while (!server.isClosed()) {
                Socket client = server.accept();
                listSockets.add(client);
                MonoThreadClientHandler thread = new MonoThreadClientHandler(client);
                thread.start();
                System.out.println("Connection accepted. " + listSockets.size() );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverWorking = false;
    }


    class MonoThreadClientHandler extends Thread {
        Socket clientSocket;
        boolean byteWasTransferred;
        boolean clientIsTransfering;

        public MonoThreadClientHandler(Socket client) {
            clientSocket = client;
            byteWasTransferred = NOT_NULL_BYTE;
            clientIsTransfering = true;
        }

        @Override
        public void run() {
            try{
                DataInputStream in;
                DataOutputStream out;
                in = new DataInputStream(clientSocket.getInputStream());
                out = new DataOutputStream(clientSocket.getOutputStream());
                Gson gson = new Gson();

                String entry = in.readUTF();
                MessageUser messageUser = gson.fromJson(entry, MessageUser.class);
                System.out.println("\nREAD from clientDialog message - \n" + entry);

                String filename = messageUser.getFileName();
                long lengthFile = messageUser.getLengthFile();
                boolean fileIsNotExist = true;
                Integer iteration = 0;
                File f = new File(URL_TO_UPLOADS + filename);
                String newFilename = filename;
                while(fileIsNotExist){
                    f = new File(URL_TO_UPLOADS + newFilename);
                    if(f.createNewFile()) {
                        System.out.println("File was created. His name : " + newFilename);
                        filename = newFilename;
                        fileIsNotExist = false;
                    }
                    else{
                        System.out.println("File " + iteration + "_" + filename  + " already exist");
                        newFilename = iteration.toString() + "_" +filename;
                        iteration++;
                    }
                    Thread.sleep(300);
                }

                FileOutputStream fileOutputStream = new FileOutputStream(f.getPath());
                long lengthDone = 0;
                byte[] byteArray = new byte[Constants.LENGTH_STEP];
                long lengthInputCount = Constants.LENGTH_STEP;
                long prevAllReadBytes = 0;
                long allReadBytes = 0;
                long initTime = System.currentTimeMillis();
                long lastTime = initTime;
                long currentTime = 0;

                while (lengthDone < lengthFile) {
                    if(lengthFile - lengthDone < Constants.LENGTH_STEP){
                        lengthInputCount = lengthFile - lengthDone;
                        byteArray = new byte[(int)lengthInputCount];
                    }
                    allReadBytes += in.read(byteArray);
                    fileOutputStream.write(byteArray);

                    lengthDone += Constants.LENGTH_STEP;
                    currentTime = System.currentTimeMillis();
                    if (currentTime - lastTime > Constants.SPEED_TEST_INTERVAL){
                        long currentSpeed = (allReadBytes - prevAllReadBytes) * Constants.MILLSEC_IN_SEC / (currentTime - lastTime);
                        long avgSpeed = allReadBytes * MILLSEC_IN_SEC / (currentTime - initTime);
                        lastTime = currentTime;
                        prevAllReadBytes = allReadBytes;
                        System.out.println("File - {" + filename +"} has current speed = " + currentSpeed + ", avg speed = " + avgSpeed);
                    }
                }

                long currentSpeed = (allReadBytes - prevAllReadBytes) * Constants.MILLSEC_IN_SEC / (currentTime - lastTime);
                long avgSpeed = allReadBytes * MILLSEC_IN_SEC / (currentTime - initTime);
                System.out.println("File - {" + filename +"} has current speed = " + currentSpeed + ", avg speed = " + avgSpeed);
                System.out.println("File {" + filename + "} was transferred!");
                in.close();
                out.close();
                fileOutputStream.flush();
                fileOutputStream.close();
            }catch (IOException e){
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
