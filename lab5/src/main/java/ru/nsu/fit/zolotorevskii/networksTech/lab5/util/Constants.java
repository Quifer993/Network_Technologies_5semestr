package ru.nsu.fit.zolotorevskii.networksTech.lab5.util;

public class Constants {
    public static final byte VERSION = 0x05;
    public static final byte NOT_AUTH = 0x00;
    public static final byte CONNECTION_COMMAND = 0x01;

    public static final byte[] NOT_AUTH_REPLY = new byte[]{VERSION, NOT_AUTH};

    public static final byte ADDR_IPV4 = 0x01;
    public static final byte ADDR_HOST = 0x03;

    public static final byte STATUS_CODE_SUCCESS = 0x00;

    public static final byte[] CONNECTION_SUCCESS_REPLY = new byte[]{
            VERSION,
            STATUS_CODE_SUCCESS,
            0x00,
            ADDR_IPV4,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00
    };
}
