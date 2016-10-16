package com.company;

import java.io.Serializable;
import java.sql.Time;
import java.util.Date;

/**
 * Created by belldell on 17.10.16.
 */
public class Message implements Serializable{
    public String text;
    public Time time;
    public String status;

    public Message(String text, Time time, String status) {
        this.text = text;
        this.time = time;
        this.status = status;
    }
}
