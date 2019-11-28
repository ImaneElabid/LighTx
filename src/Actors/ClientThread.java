package Actors;

import Protocol.Converter;
import Protocol.Message;

import javax.net.ssl.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;

public class ClientThread extends Thread {
    Node node;
    int nodesNbr;
    SSLSocket sslsocket;


    public ClientThread(Node node, int nodesNbr) throws Exception {
        this.node = node;
        this.nodesNbr = nodesNbr;
        initSocket();
        initComm();
    }

    public void initSocket() throws Exception {
        for (int i = 0; i <= this.nodesNbr; i++) {
            String s = "C:/Program Files/Java/jdk-12/bin/server" + i + ".jks";
            System.setProperty("javax.net.ssl.trustStore", s);
            System.setProperty("javax.net.ssl.trustStorePassword", "123456");
            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            int port = Converter.PORT + i;
            sslsocket = (SSLSocket) sslsocketfactory.createSocket("localhost", port);
            sslsocket.startHandshake();
            node.nodesSockets.add(sslsocket); //list of connections
        }
    }

    public void initComm() throws Exception {
        node.setGossipSample(node.pickGossipSample());
        for (Integer index : node.gossipSample) {
            node.send(node.nodesSockets.get(index), new Message(node.id));
        }
        node.delivered = null;
    }

    public void run() {

        try {
            if (node.getInitiator()) {
                sleep(500);
                String value = "Hello";
                String signature = node.sign(value, node.getPrivateKey());
                node.dispatch(value, signature, node.gossipSample, node.id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}