package com.greengrass.protocol;


import com.greengrass.exception.ProxyException;

public enum MessageType {

    REGISTER(1),
    REGISTER_RESULT(2),
    CONNECTED(3),
    DISCONNECTED(4),
    DATA(5),
    KEEPALIVE(6);

    private int code;

    MessageType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MessageType valueOf(int code) throws ProxyException {
        for (MessageType item : MessageType.values()) {
            if (item.code == code) {
                return item;
            }
        }
        throw new ProxyException("MessageType code error: " + code);
    }
}
