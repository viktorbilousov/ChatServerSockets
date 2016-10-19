package com.company;

import ServerException.InitExeption;
import com.sun.deploy.util.SessionState;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by belldell on 17.10.16.
 */
public class Server {

    private final String serverName = "Server";

    private List<Message> messageLog;
    private List<ClientStr> clientList;
    private int maxClient = 10;
    private int maxLogMessage = 10;
    private String IP = "";
    private final int ATTEMPTS_SEND_MESSAGE = 10;
    private int port = 0;
    private ServerStatus status = ServerStatus.ERROR;
    private ServerSocket sSocket;
    private int clientID = 0;

    private Thread waitThr = null;
    private HashMap<Integer, Thread> clientThr; // ( ID client , Thread )


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

    public boolean start() {
        // все изменения статуса писать здесь, а не во вложеных функциях!
        setStatus(ServerStatus.LOAD);
        try {
            initServer();
            System.out.println("Init OK!");
            setStatus(ServerStatus.RUN);
            waitThr = new Thread(new WaitAndConnectClient());
            waitThr.start();
        } catch (InitExeption e) {
            setStatus(ServerStatus.ERROR);
            System.out.println(e);
            System.out.println("Init ERROR");
            return false;
        }
        return true;
    }

    public boolean close() {
        if (status != ServerStatus.RUN)
            return false;
        setStatus(ServerStatus.STOPING);

        try {
            if (!sSocket.isClosed())
                sSocket.close();

            waitThr.interrupt();
            waitThr.join();

            disconnectAllClient();

        } catch (Exception E) {
            System.out.println(E);
        }
        if (status == ServerStatus.STOPING)
            setStatus(ServerStatus.STOP);

        return true;
    }

    private void setStatus(ServerStatus status) {
        if (this.status == status)
            return;
        System.out.println("Change status: " + this.status + " -> " + status);
        this.status = status;
    }

    public ServerStatus getStThread() {
        return status;
    }

    private synchronized int getNewID() {
        return ++clientID;
    }

    private void initServer() throws InitExeption {
        disconnectAllClient();
        waitThr = null;
        messageLog = new ArrayList<>();
        clientList = new ArrayList<>();
        clientThr = new HashMap<>();

        try {
            sSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new InitExeption(e.toString());
        }
    }

    private void waitAndConnectClient() {

        // TODO: 20.10.16 do console output "'%client% connected to chat'"
        // TODO: 20.10.16 password client enter ?

        while (!waitThr.isInterrupted()) {
            System.out.println("wait new connect");
            try {
                Socket socket = sSocket.accept();
                ClientStr client = new ClientStr(socket, getNewID());
                System.out.println("Got a client :)");
                if (clientList.size() >= maxClient) {
                    sendMessage(client, getServerMessage("Client list is fool!"));
                    socket = null;
                } else {
                    synchronized (this) {
                        Thread clThr = new Thread(new ListenClient(client));
                        clientList.add(client);
                        clientThr.put(client.id, clThr);
                        clThr.start();
                    }
                }
            } catch (SocketException e) {
                if (e.getMessage().equals("Socket closed")) {
                    if (status != ServerStatus.STOPING) {
                        setStatus(ServerStatus.ERROR);
                        System.out.println("Need reconnect to socket");
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
                System.out.println("client error connect!");
            }
        }
        System.out.println("stop waiting!");
    }

    private void listenClient(ClientStr client) {
        try {
            // getNameFromClient
            if (!initClientProc(client) && !Thread.interrupted()) {
                System.out.println("Error init client");
                return;
            }
            sendMessage(client, getServerMessage("Welcome to chat, " + client.name + "!"));
            sendLog(client);
            sendMessageAllExept(client, getServerMessage(client.name + " enter to chat!"));

            Message m = null;
            while (!Thread.interrupted()) {
                try {
                    m = (Message) client.in.readObject();
                } catch (ClassNotFoundException E) {
                    System.out.println("Error input message from " + client);
                    continue;
                }
                if (Thread.interrupted())
                    break;

                addMessageToLog(m);
                sendMessageAllExept(client, m);
                System.out.println(m);
            }
        } catch (IOException E) {
            System.out.println("Client disconnected : " + client);

            if (status == ServerStatus.RUN)
                sendMessageAllExept(client, getServerMessage(client.name + " live from chat!"));
        } finally {
            removeClient(client);
            System.out.println("Client deleted from DB : " + client);
        }
    }

    private boolean initClientProc(ClientStr client) {
        sendMessage(client, ServerMessage.NAME_REQUEST); // send string !!!!!
        String inputName = "";

        try {
            while (true) {
                inputName = (String) client.in.readObject();
                boolean flag = true;
                for (ClientStr c : clientList) {
                    if (c.name.equals(inputName)) {
                        flag = false;
                        break;
                    }
                }
                if (!flag)
                    sendMessage(client, ServerMessage.NAME_REQUEST_ERROR);
                else {
                    sendMessage(client, ServerMessage.NAME_REQUEST_ASSEPT);
                    client.name = inputName;
                    break;
                }

            }
        } catch (IOException E) {
            System.out.println(E);
            return false;
        } catch (ClassNotFoundException E) {
            System.out.println(E);
            return false;
        }


        return true;
    }

    private void disconnectClient(ClientStr client) {
        Thread threadClient = clientThr.get(client.id);
        try {
            threadClient.interrupt();
            sendMessage(client, new Message(ServerMessage.DISCONNECT));
            client.out.close();
            client.in.close();

            if (threadClient.isAlive())
                threadClient.join();
        } catch (InterruptedException E) {
            System.out.println(E.toString());
        } catch (Exception E) {
            System.out.println(E);
        }
    }

    private void disconnectAllClient() {
        if (clientList == null)
            return;

        while (!clientList.isEmpty())
            disconnectClient(clientList.get(0));

        if (!clientThr.isEmpty())
            System.out.println("Error disconnected!");
    }

    private synchronized void removeClient(ClientStr client) {
        if (clientThr.containsKey(client.id)) {
            try {
                client.out.close();
                client.in.close();
            } catch (IOException E) {
                System.out.println(E);
            }
            clientThr.remove(client.id);
        }

        if (clientList.contains(client))
            clientList.remove(client);
    }

    private boolean sendMessage(ClientStr client, Object message) {
        try {
            //  System.out.println("Sending message to Client " + client);
            ObjectOutputStream out = client.out;
            out.writeObject(message); // отсылаем введенную строку текста серверу.
            out.flush(); // заставляем поток закончить передачу данных.
        } catch (IOException E) {
            System.out.println(E.toString());
            System.out.println("Error send message");
            return false;
        }
        //  System.out.println("Message sended");
        return true;

    }

    private boolean sendMessage(ClientStr clientStr, Object message, int attempts) {
        if (attempts <= 0) {
            System.out.println("ERROR ARG: attempts <= 0");
            throw new IllegalArgumentException("attehis.clientList: ClientStr clientmpts <= 0");
        }

        for (int i = 0; i < attempts; i++) {
            System.out.printf("Attempt " + (i + 1) + " :");
            if (sendMessage(clientStr, message))
                return true;
        }

        return false;
    }

    private synchronized void sendLog(ClientStr client) {
        for (int i = messageLog.size() - 1, cnt = 0; i >= 0 && cnt < maxLogMessage; i--, cnt++) {
            sendMessage(client, messageLog.get(i));
        }
    }

    private void sendMessageAllExept(ClientStr client, Message m) {
        for (ClientStr cl : clientList) {
            if (cl != client)
                sendMessage(cl, m);
        }
    }

    private void addMessageToLog(Message m) {
        while (messageLog.size() > maxLogMessage) { // а вдруг магия =)
            messageLog.remove(0);
        }
        messageLog.add(m);
    }

    private Message getServerMessage(String text) {
        return new Message(text, serverName, null);
    }

    private void soutAll() {

        System.out.println("Clients : ");
        System.out.println(clientList.size());
        for (ClientStr client : clientList) {
            System.out.println(client.toString());
        }
        System.out.println("\nThreads : ");
        for (Integer id : clientThr.keySet()) {
            System.out.println("Id=" + id + " Thr=" + clientThr.get(id));
        }
    }

    private class WaitAndConnectClient implements Runnable {
        @Override
        public void run() {
            waitAndConnectClient();
        }
    }

    private class ListenClient implements Runnable {
        private ClientStr clietnArg;

        public ListenClient(ClientStr clietnArg) {
            this.clietnArg = clietnArg;
        }

        @Override
        public void run() {
            listenClient(clietnArg);
        }
    }

    private class ClientStr {
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        String name = "null";
        String status = "nan";
        int id;

        public ClientStr(Socket socket, String name, int id) {
            try {
                this.out = new ObjectOutputStream(socket.getOutputStream());
                this.in = new ObjectInputStream(socket.getInputStream());
                this.name = name;
                this.id = id;
            } catch (Exception E) {
                System.out.println("Error init");
            }
        }

        public ClientStr(Socket socket, int id) {
            try {
                this.out = new ObjectOutputStream(socket.getOutputStream());
                this.in = new ObjectInputStream(socket.getInputStream());
                this.id = id;
            } catch (Exception E) {
                System.out.println("Error init");
            }
        }

        public void setStatus(String status) {
            this.status = status;
        }


        @Override
        public String toString() {
            return "ClientStr{" +
                    "name='" + name + '\'' +
                    ", id=" + id +
                    '}';
        }
    }

    private ClientStr getClient(String name) {
        for (ClientStr client : clientList) {
            if (client.name.equals(name))
                return client;
        }
        return null;
    }

    private ClientStr getClient(int id) {
        for (ClientStr client : clientList) {
            if (client.id == id)
                return client;
        }
        return null;
    }

    private static final class ServerMessage {
        public final static String DISCONNECT = "Disconnect message";
        public final static String NAME_REQUEST = "Wait name";
        public final static String NAME_REQUEST_ERROR = "Error name";
        public final static String NAME_REQUEST_ASSEPT = "Accepted name";
    }

    public enum ServerStatus {
        STOP,
        ERROR,
        LOAD,
        RUN,
        STOPING
    }
}