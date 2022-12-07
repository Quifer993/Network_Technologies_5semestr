package ru.nsu.fit.zolotorevskii.networksTech.lab5;

import ru.nsu.fit.zolotorevskii.networksTech.lab5.proxy.ProxySocks;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws UnknownHostException {
        System.out.println(InetAddress.getLocalHost().getHostAddress());//
        List<Integer> handlerEnding = new ArrayList<>();
        ProxySocks proxy = new ProxySocks(InetAddress.getLocalHost(), 9605);
        proxy.run();
    }
}
