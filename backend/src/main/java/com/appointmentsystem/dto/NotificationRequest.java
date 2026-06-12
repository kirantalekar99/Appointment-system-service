package com.appointmentsystem.dto;

public class NotificationRequest {

    private String channel;
    private String recipientGroup;
    private String recipient;
    private String message;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getRecipientGroup() {
        return recipientGroup;
    }

    public void setRecipientGroup(String recipientGroup) {
        this.recipientGroup = recipientGroup;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
