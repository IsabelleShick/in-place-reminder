package com.example.inplacereminder;

import java.io.Serializable;

public class Reminder implements Serializable {
    public long id;
    public String title;
    public String description;
    public long dateMillis;

    public Reminder(long id, String title, String description, long dateMillis) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dateMillis = dateMillis;
    }

    @Override
    public String toString() {
        return title != null ? title : "";
    }
}