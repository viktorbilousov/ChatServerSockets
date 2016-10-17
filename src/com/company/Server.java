package com.company;

import ServerException.InitExeption;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by belldell on 17.10.16.
 */
public class Server {
    private ArrayList<Message> messagLog;
    private HashSet<ClientStr> clientList;
    private int maxClient = 10;
    private int maxLogMessage = 10;
    private String IP = "";
    private final int ATTEMPTS_SEND_MESSAGE = 10;
    private int port = 0;
    private ServerStatus status = ServerStatus.ERROR;
    private ServerSocket sSocket;
    private int clientID = 0;

    //region Set\Get
    public ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        if(this.status == status)
            return;
        System.out.println("Change status: " + this.status + " -> " + status );
        this.status = status;
    }

    //endregion

    //region constructots
    public Server(int maxClient, int maxLogMessage, String IP, int port) {
        this.maxClient = maxClient;
        this.maxLogMessage = maxLogMessage;
        this.IP = IP;
        this.port = port;
    }

    public Server(int port) {
        this.port = port;
    }
   //todo write\read setting to\from XML file

    //endregion


    private void initServer() throws InitExeption
    {
        messagLog = new ArrayList<>();
        clientList = new HashSet<>();

        try { sSocket = new ServerSocket(port); }
        catch (IOException e){
            throw new InitExeption(e.toString());
        }
     }

    public boolean start()
    {
        // все изменения статуса писать здесь, а не во вложеных функциях!
        setStatus(ServerStatus.LOAD);
        try {
            initServer();
            //todo Closing
            System.out.println("Init OK!");
            setStatus(ServerStatus.WORK);
            //todo в другой поток
            waitAndConnectClient();

        }
        catch (InitExeption e)
        {
            setStatus(ServerStatus.ERROR);
            System.out.println(e);
            System.out.println("Init ERROR");
            return false;
        }


     /*  try {
            System.out.println("Waiting for a client...");
            Socket socket = ss.accept(); // заставляем сервер ждать подключений и выводим сообщение когда кто-то связался с сервером
            System.out.println("Got a client :) ... Finally, someone saw me through all the cover!");
            System.out.println();

            // Берем входной и выходной потоки сокета, теперь можем получать и отсылать данные клиенту.
            InputStream sin = socket.getInputStream();
            OutputStream sout = socket.getOutputStream();

            // Конвертируем потоки в другой тип, чтоб легче обрабатывать текстовые сообщения.
            DataInputStream in = new DataInputStream(sin);
            DataOutputStream out = new DataOutputStream(sout);

            String line = null;
            while(true) {
                line = in.readUTF(); // ожидаем пока клиент пришлет строку текста.
                System.out.println("The dumb clienthrows InitExeptiont just sent me this line : " + line);
                System.out.println("I'm sending it back...");
                System.out.println(line);
                out.writeUTF(line); // отсылаем клиенту обратно ту самую строку текста.
                out.flush(); // заставляем поток закончить передачу данных.
                System.out.println("Waiting for the next line...");
                System.out.println();
                break;
            }
        } catch(ServerException x) {
            x.printStackTrace();
            return false;clienthrows
        }*/
        return true;
    }

    private void waitAndConnectClient()
    {
        while(status == ServerStatus.WORK) {
            System.out.println("wait new connect");
            try {
                Socket socket = sSocket.accept();
                ClientStr client = new ClientStr(socket, getNewID());
                System.out.println("Got a client :)");
                if (clientList.size() >= maxClient) {
                    sendMessage(client, "Client list is fool!", ATTEMPTS_SEND_MESSAGE);
                    socket = null;
                } else {
                    synchronized (this) {
                        clientList.add(client);
                        // todo listen client
                    }
                }

            } catch (IOException e) {
                System.out.println(e);
                System.out.println("client error connect!");
            }
        }
    }

    private boolean sendMessage(ClientStr client, String message)
    {
        //todo sendMessage
        try {
            System.out.println("Sending message to Client " + client);
            DataOutputStream out = new DataOutputStream(client.socket.getOutputStream());
            out.writeUTF(message); // отсылаем введенную строку текста серверу.
            out.flush(); // заставляем поток закончить передачу данных.
        }catch (IOException E){
            System.out.println(E.toString());
            System.out.println("Error send message" );
            return false;
        }
        System.out.println("Message sended" );
        return true;

    }

    private boolean sendMessage(ClientStr clientStr, String message, int attempts)
    {
        if(attempts <= 0 ){
            System.out.println("ERROR ARG: attempts <= 0");
            throw new IllegalArgumentException("attempts <= 0");
        }
        boolean sending = false;

        for (int i = 0; i < attempts; i++) {
            System.out.printf("Attempt " + (i+1) + " :" );
             if (sendMessage(clientStr, message))
                 return true;
        }

        return false;
    }

    private class ClientStr    {
        public Socket socket;
        String name;
        int id;

        public ClientStr(Socket socket, String name, int id) {
            this.socket = socket;
            this.name = name;
            this.id = id;
        }
        public ClientStr(Socket socket, int id)
        {
            this.socket = socket;
            name = null;
            this.id = id;
        }

        @Override
        public String toString() {
            return "[socket=" + socket +
                    ", name='" + name + '\'' +
                    ", id=" + id + ']';
        }
    }

    private synchronized int getNewID(){
        return ++clientID;
    }

    public enum ServerStatus {
        STOP,
        ERROR,
        LOAD,
        WORK,
        STOPING
    }
}
