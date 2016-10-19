package com.company;

import javax.xml.crypto.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by belldell on 17.10.16.
 */
public class Message implements Serializable{
    public String text;
    public Date time;
    public String nameSender = "null";
    public String status = "active";

     public Message(String text) {
        this.text = text;
        this.time = new Date();
    }

    public Message(String text, String nameSender, String status) {
        this.text = text;
        this.time = new Date();
        this.nameSender = nameSender;
        this.status = status;
    }

    public Message(String text, String nameSender, String status, Date time) {
        this.text = text;
        this.time = time;
        this.nameSender = nameSender;
        this.status = status;
    }


    @Override
    public String toString() {
        return  "[ " + time + "] " + nameSender + " ("+ status +")"+ " : " + text;
    }
}
