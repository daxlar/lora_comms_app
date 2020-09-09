package com.example.lora_comms_app;

public class AppMessage extends Message{

    public static final String MESSAGE_TYPE = "APP_MESSAGE_TYPE";

    @Override
    String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    void setMessageData(final String messageData) {
        this.messageData = messageData;
    }

    @Override
    String getMessageData() {
        return messageData;
    }
}
