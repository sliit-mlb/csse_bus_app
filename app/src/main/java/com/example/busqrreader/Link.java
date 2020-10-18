package com.example.busqrreader;

public class Link {
    public String id;
    public String startTime;
    public String departure;
    public String date;
    public Link next;

    public Link(String id, String startTime, String departure, String date) {
        this.id = id;
        this.startTime = startTime;
        this.departure = departure;
        this.date = date;
        next = null;
    }
}
