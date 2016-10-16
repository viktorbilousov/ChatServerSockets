package com.company;

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
    private int port = 0;

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
    public boolean start()
    {
        try {
            ServerSocket ss = new ServerSocket(port); // создаем сокет сервера и привязываем его к вышеуказанному порту
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
                System.out.println("The dumb client just sent me this line : " + line);
                System.out.println("I'm sending it back...");
                System.out.println(line);
                out.writeUTF(line); // отсылаем клиенту обратно ту самую строку текста.
                out.flush(); // заставляем поток закончить передачу данных.
                System.out.println("Waiting for the next line...");
                System.out.println();
                break;
            }
        } catch(Exception x) {
            x.printStackTrace();
            return false;
        }
        return true;
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
    }
}
