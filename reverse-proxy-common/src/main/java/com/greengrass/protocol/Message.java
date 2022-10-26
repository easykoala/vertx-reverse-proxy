package com.greengrass.protocol;

import java.util.Map;

/**
 * Created by easykoala on 2020/9/1.
 */
public class Message {

    private MessageType type;
    private Map<String, Object> metaData;
    private byte[] data;

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Map<String, Object> getMetaData() {
        return metaData;
    }

    public void setMetaData(Map<String, Object> metaData) {
        this.metaData = metaData;
    }


    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
