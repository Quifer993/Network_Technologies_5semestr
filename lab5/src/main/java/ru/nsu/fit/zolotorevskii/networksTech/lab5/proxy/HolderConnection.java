package ru.nsu.fit.zolotorevskii.networksTech.lab5.proxy;

import ru.nsu.fit.zolotorevskii.networksTech.lab5.util.TypeWork;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class HolderConnection {
    TypeWork type;
    ByteBuffer in;
    ByteBuffer out;
    SelectionKey peer;
    int port;
}
