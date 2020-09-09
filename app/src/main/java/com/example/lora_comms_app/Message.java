package com.example.lora_comms_app;

public abstract class Message {

    protected String messageType;
    protected String messageData;

    abstract String getMessageType();
    abstract void setMessageData(final String messageData);
    abstract String getMessageData();
}
