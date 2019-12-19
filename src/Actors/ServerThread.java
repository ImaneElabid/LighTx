package Actors;

import Protocol.Converter;
import Protocol.Message;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.PublicKey;
import java.util.HashMap;

public class ServerThread extends Thread {
    SSLSocket sslsocket;
    Node node;


    public ServerThread(SSLSocket sslsocket, Node node) {
        this.sslsocket = sslsocket;
        this.node = node;
    }

    /**********************************************************************/

    public void rcvGossipSub(Message msg) throws IOException {
//        System.out.println("(" + node.id + ") Received GOSSIPSUBSCRIBE from (" + msg.getSenderID()+") aat : "+System.currentTimeMillis());
        if (node.delivered.get() != null) {
            Message redirected = node.delivered.get();
            redirected.setType("Gossip");
            redirected.setForwarderID(node.id);
            node.send(node.nodesSockets.get(msg.getSenderID()), redirected);
//            System.out.println(node.id + " redirected \"GOSSIP\" to [" + msg.getSenderID() + "] at " + System.currentTimeMillis());
        }
        node.gossipSample.add(msg.getSenderID());
    }

    /**********************************************************************/

    public void rcvGossip(Message msg) {
        try {
            if (node.verify(String.valueOf(msg.getSenderID()), msg.getSignature(), (PublicKey) node.nodesInfo.get(msg.getSenderID()))) {
                synchronized (node.delivered) {
//                    if (node.delivered.get() == null) {
//                        System.out.println("(" + node.id + ")has delivered : \"" + msg.getMessage() + "\" from (" + (msg.getForwarderID()) + ") : " + System.currentTimeMillis());
//                    }
                    node.dispatch(msg.getMessage(), msg.getSignature(), node.gossipSample, msg.getSenderID());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**********************************************************************/

    public void rcvEchoSub(Message msg) throws IOException {
//        System.out.println("(" + node.id + ") Received ECHOSUBSCRIBE from (" + msg.getSenderID() + ")");
        if (node.echo != null) {
            Message redirected = node.echo;
            redirected.setType("Echo");
            redirected.setForwarderID(node.id);
            node.send(node.nodesSockets.get(msg.getSenderID()), redirected);
//            System.out.println(node.id + " redirected \"ECHO\" msg to [" + msg.getSenderID() + "] at " + System.currentTimeMillis());
        }
        node.echoSubscriptionSample.add(msg.getSenderID());
    }

    /**********************************************************************/

    public void rcvEcho(Message msg) throws Exception {
        if (node.echoSample.contains(msg.getForwarderID())
                && node.repliesEcho.get(msg.getForwarderID()) == null
                && node.verify(String.valueOf(msg.getSenderID()), msg.getSignature(),
                (PublicKey) node.nodesInfo.get(msg.getSenderID()))) {
            node.repliesEcho.put(msg.getForwarderID(), msg);
//            System.out.println(node.id +" got an echo reply ++ from == "+msg.getForwarderID());
        }
        node.gotEnoughEchoes(Converter.E_HAT, node.echo);

    }

    /**********************************************************************/

    public void rcvReadySub(Message msg) throws IOException {
//        System.out.println("(" + node.id + ") Received READYSUBSCRIBE from (" + msg.getSenderID() + ") at : "+System.currentTimeMillis());
        for (Message ready : node.ready) {
            ready.setType("Ready");
            ready.setForwarderID(node.id);
            node.send(node.nodesSockets.get(msg.getSenderID()), ready);
        }
        node.readySubscriptionSample.add(msg.getSenderID());
    }

    /**********************************************************************/

    public void rcvReady(Message msg) throws Exception {
//        System.out.println("(" + node.id + ") Received READY from (" + msg.getForwarderID() + ") : " + System.currentTimeMillis());
        if (node.verify(String.valueOf(msg.getSenderID()),
                msg.getSignature(), (PublicKey) node.nodesInfo.get(msg.getSenderID()))) {
            HashMap<Object, String> reply = msg.getPair();
            if (node.readySample.contains(msg.getForwarderID())) {
                node.readyReplies.get(msg.getForwarderID()).add(reply);
                node.gotEnoughReady(Converter.R_HAT, msg);
            }
            if (node.deliverySample.contains(msg.getForwarderID())) {
                node.deliveryReplies.get(msg.getForwarderID()).add(reply);
                node.gotEnoughDelivery(Converter.D_HAT, msg);
            }
        }
    }

    /**********************************************************************/
    public void run() {
        while (true) {
            try {
                sslsocket.startHandshake();
                Message msg = (Message) node.receive(sslsocket);

                switch (msg.getType()) {
                    case "GossipSub":
                        rcvGossipSub(msg);
                        break;

                    case "EchoSub":
                        rcvEchoSub(msg);
                        break;

                    case "ReadySub":
                        rcvReadySub(msg);
                        break;

                    case "Gossip":
                        rcvGossip(msg);
                        break;

                    case "Echo":
                        rcvEcho(msg);
                        break;

                    case "Ready":
                        rcvReady(msg);
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
