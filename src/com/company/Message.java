package com.company;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by belldell on 17.10.16.
 */
public class Message implements Serializable{
    public String text;
    public Date time;
    public String nameSender = "null";
    public String status = "active";

    public Message(String text, Date time, String status) {
        this.text = text;
        this.time = time;
        this.status = status;
    }

    public Message(String text) {
        this.text = text;
        this.time = new Date();
    }

    @Override

    public String toString() {
        return  "[ " + time + "] " + nameSender + " ("+ status +")"+ " : " + text;
    }
}
