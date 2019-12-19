package Actors;

import Protocol.Converter;
import Protocol.Message;

import javax.net.ssl.*;
import java.util.ArrayList;
import java.util.Set;


public class ClientThread extends Thread {
    Node node;
    int nodesNbr;
    SSLSocket sslsocket;


    public ClientThread(Node node, int nodesNbr) throws Exception {
        this.node = node;
        this.nodesNbr = nodesNbr;
        initCommunicationChannel();
    }

    /*************************************************************************/

    public void initCommunicationChannel() throws Exception {
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
        Node.finished.getAndIncrement();
    }

    public void protocolInit(int protocol) throws Exception {
        pbInit();
        if (protocol == Converter.pcb)
            pcbInit(Converter.E_SIZE);             // =======Parameter E : size of Echo sample======= //
        else if (protocol == Converter.psb) {
            pcbInit(Converter.E_SIZE);            // =======Parameter E : size of Echo sample======= //
            psbInit(Converter.R_SIZE, Converter.D_SIZE);      // =======Parameter R : size of Ready sample======= //  // =======Parameter D : size of Delivery sample======= //
        }
    }

    /*************************************************************************/

    public void pbInit() throws Exception {
        node.setGossipSample(node.pickGossipSample());
        Message gossipSub = new Message("GossipSub", node.id);
        for (Integer index : node.gossipSample) {
            node.send(node.nodesSockets.get(index), gossipSub);
        }
//        node.delivered.set(null);
    }

    public void pbBroadcast() throws Exception {
        Object value = (String) "Hello";
        String signature = node.sign(String.valueOf(node.id), node.getPrivateKey());
        node.dispatch(value, signature, node.gossipSample, node.id);
//        node.byzantineBroadcast(node.gossipSample);
    }

    /*************************************************************************/

    public void pcbInit(int size) throws Exception {
        Message echoSub = new Message("EchoSub", node.id);
        node.setEchoSample(node.sample(echoSub, size));
    }

    public void pcbBroadcast() throws Exception {
        pbBroadcast();
    }

    /*************************************************************************/

    public void psbInit(int R, int D) throws Exception {
        Message readySub = new Message("ReadySub", node.id);
        node.setReadySample(node.sample(readySub, R));
        for (int rho: node.readySample) {
//            System.out.println("("+node.id+") added ("+rho+") to its ready sample");
        }
        node.setRepliesReady();
        node.setDeliverySample(node.sample(readySub, D));
        for (int rho: node.deliverySample) {
//        System.out.println("("+node.id+") added ("+rho+") to its delivery sample");
        }
        node.setRepliesDelivery();
    }

    public void psbBroadcast() throws Exception {
        pcbBroadcast();
    }

    /*************************************************************************/
    public void run() {
        try {
            protocolInit(node.protocol);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (node.getInitiator()) {
            try {
                node.waitTillFinished(200);
                switch (node.protocol) {
                    case Converter.pb:
                        pbBroadcast();
                        break;
                    case Converter.pcb:
                        pcbBroadcast();
                        break;
                    case Converter.psb:
                        psbBroadcast();
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}