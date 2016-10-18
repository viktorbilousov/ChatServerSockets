package com.company;

import java.io.*;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
	    Server s = new Server(1994);
        System.out.println("Start status ="+s.start());
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        StopThread.waitM(1);
        s.close();
    }

    public static class StopThread implements Runnable{
        BufferedReader reader = null;

        public StopThread(BufferedReader reader) {
            this.reader = reader;
        }

        public static void waitM(int sec)
        {
            try{
            for (int i = 0; i < sec; i++) {
                Thread.sleep(1000);
                System.out.println(i+1);
            }
        }
        catch (Exception E){
            System.out.println(E);
        }
        }
        @Override
        public void run() {
           waitM(5);
            /*reader.close();
            reader.reset()*/
        }
    }
}
