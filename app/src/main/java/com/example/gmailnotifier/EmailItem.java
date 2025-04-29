package com.example.gmailnotifier;

public class EmailItem {
    private String id;
    private String subject;
    private String sender;
    private String date;
    private String snippet;

    public EmailItem(String id, String subject, String sender, String date, String snippet) {
        this.id = id;
        this.subject = subject;
        this.sender = sender;
        this.date = date;
        this.snippet = snippet;
    }

    public String getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getSender() {
        return sender;
    }

    public String getDate() {
        return date;
    }

    public String getSnippet() {
        return snippet;
    }
}