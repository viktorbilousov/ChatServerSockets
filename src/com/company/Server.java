package com.company;

import ServerException.InitExeption;

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
    private List<Message> messagLog;
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
    private HashMap<Integer, Thread> clientThr; // ( ID client , futurThread

    public ServerStatus getStThread() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        if(this.status == status)
            return;
        System.out.println("Change status: " + this.status + " -> " + status );
        this.status = status;
    }


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

    public boolean testDisconnect()
    {
        if(clientList.size() == 0 || clientList == null)
            return false;
        if(messagLog.size() == 0 || messagLog == null)
            return false;

        disconnectClient(clientList.get(0));
        return true;
    }

    private void initServer() throws InitExeption
    {
        disconnectAllClient();
        waitThr = null;
        messagLog = new ArrayList<>();
        clientList = new ArrayList<>();
        clientThr = new HashMap<>();

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
            System.out.println("Init OK!");
            setStatus(ServerStatus.RUN);
            waitThr = new Thread(new WaitAndConnectClient());
            waitThr.start();
        }
        catch (InitExeption e)
        {
            setStatus(ServerStatus.ERROR);
            System.out.println(e);
            System.out.println("Init ERROR");
            return false;
        }
        return true;
    }

    public boolean close()
    {
        if(status != ServerStatus.RUN)
            return false;

        try {
        setStatus(ServerStatus.STOPING);
        disconnectAllClient();

        if(!sSocket.isClosed())
            sSocket.close();

        waitThr.interrupt();
        waitThr.join();

        }catch (Exception E) {
            System.out.println(E);
        }
        if(status == ServerStatus.STOPING)
            setStatus(ServerStatus.STOP);
        return true;
    }

    private void waitAndConnectClient()
    {
        while(!waitThr.isInterrupted()) {
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
                        Thread clThr = new Thread(new ListenClient(client));
                        clientList.add(client);
                        clientThr.put(client.id, clThr);
                        clThr.start();
                    }
                }
            }catch (SocketException e)
            {
                if(e.getMessage().equals("Socket closed")){
                    if(status != ServerStatus.STOPING) {
                        setStatus(ServerStatus.ERROR);
                        System.out.println("Need reconnect to socket");
                    }
                }
            }
            catch (IOException e) {
                System.out.println(e);
                System.out.println("client error connect!");
            }
        }
        System.out.println("stop waiting!");
    }

    private void listenClient(ClientStr client)
    {
 //     soutAll();
        try{
            InputStream sin = client.socket.getInputStream();
            DataInputStream in = new DataInputStream(sin);
            String line = null;
            Message m = null;
            while(!Thread.interrupted()  && status == ServerStatus.RUN) {
                line = in.readUTF(); // ожидаем пока клиент пришлет строку текста.
                if(Thread.interrupted())
                   break;
                m = new Message(line);
                messagLog.add(m);
                
                for (ClientStr cl: clientList ) {
                    if(cl!=client)
                        sendMessage(cl,m.toString());
                }
                System.out.println(line);
            }
        } catch(IOException E) {
            System.out.println("Client disconnected!");
        }
        finally {
            //if(!client.socket.isClosed())
            sendMessage(client, ServerMessage.DISCONNECT, ATTEMPTS_SEND_MESSAGE);
            removeClient(client);
            System.out.println("Client disconect : " + client ) ;
//            soutAll();
        }
    }

    private void disconnectClient(ClientStr client){
        Thread threadClient = clientThr.get(client.id);

        try{
            threadClient.interrupt();
            client.socket.shutdownInput();

            if(threadClient.isAlive())
                threadClient.join();
        }
        catch (InterruptedException E){
            System.out.println(E.toString());
        }
        catch (Exception E){
            System.out.println(E);
        }
    }

    private void disconnectAllClient()
    {
        if(clientList == null)
            return;
        
        while(!clientList.isEmpty())
            disconnectClient(clientList.get(0));
        if(!clientThr.isEmpty())
            System.out.println("Error disconnected!");
    }
    private synchronized void removeClient(ClientStr client){
        if(clientThr.containsKey(client.id))
            clientThr.remove(client.id);

        if(clientList.contains(client))
            clientList.remove(client);
    }

    private boolean sendMessage(ClientStr client, String message)
    {
        //// TODO: 18.10.16 rewrite with param Message(class) 
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
            throw new IllegalArgumentException("attehis.clientList: ClientStr clientmpts <= 0");
        }
        boolean sending = false;

        for (int i = 0; i < attempts; i++) {
            System.out.printf("Attempt " + (i+1) + " :" );
             if (sendMessage(clientStr, message))
                 return true;
        }

        return false;
    }
    
    private ClientStr getClient(int id)
    {
        for (ClientStr client: clientList) {
            if(client.id == id)
                return client;
        }
        return null;
    }
    private void soutAll()
    {

        System.out.println("Clients : ");
        System.out.println(clientList.size());
        for (ClientStr client: clientList) {
            System.out.println(client.toString());
        }
        System.out.println("\nThreads : ");
        for (Integer id :clientThr.keySet()) {
            System.out.println("Id=" + id + " Thr="+clientThr.get(id));
        }
    }

    private class WaitAndConnectClient implements Runnable{
        @Override
        public void run() {
            waitAndConnectClient();
        }
    }
    private class ListenClient implements Runnable{
        private ClientStr clietnArg;

        public ListenClient(ClientStr clietnArg) {
            this.clietnArg = clietnArg;
        }

        @Override
        public void run() {listenClient(clietnArg);}
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
    private ClientStr containClient(String name)
    {
        for (ClientStr client : clientList) {
            if(client.name.equals(name))
                return client;
        }
        return null;
    }
    private static final class ServerMessage
    {
        public final static String DISCONNECT = "Disconnect message";
    }

    public enum ServerStatus {
        STOP,
        ERROR,
        LOAD,
        RUN,
        STOPING
    }
}
