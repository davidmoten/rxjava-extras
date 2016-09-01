package com.github.davidmoten.rx;

import rx.Notification;

public final class ConnectionNotification {

    private final String id;
    private final Notification<byte[]> notification;

    public ConnectionNotification(String id, Notification<byte[]> notification) {
        this.id = id;
        this.notification = notification;
    }

    public String id() {
        return id;
    }

    public Notification<byte[]> notification() {
        return notification;
    }

}