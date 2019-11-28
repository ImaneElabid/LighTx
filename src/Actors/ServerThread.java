package Actors;

import Protocol.Message;

import javax.net.ssl.SSLSocket;
import java.security.PublicKey;

public class ServerThread extends Thread {
    SSLSocket sslsocket;
    Node node;


    public ServerThread(SSLSocket sslsocket, Node node) {
        this.sslsocket = sslsocket;
        this.node = node;
    }

    public void run() {
        while (true) { //!node.deliveryConfirmed
            try {
                sslsocket.startHandshake();
                Message msg = (Message) node.receive(sslsocket);

                if (msg.getType().equals("GossipSub")) {
                    if (node.delivered != null) {
                        node.send(node.nodesSockets.get(msg.getSenderID()), node.delivered);
                        System.out.println("Redirect");
                    }
                    node.gossipSample.add(msg.getSenderID());

                } else if (msg.getType().equals("Gossip")) {

                    if (node.delivered == (null))
                        System.out.println("(" + node.id + "): received > " + msg.getMessage() + " from (" + (msg.getForwarderID()) + ")");


                    if (node.verify(msg.getMessage(), msg.getSignature(), (PublicKey) node.nodesInfo.get(msg.getSenderID()))) {
                        node.dispatch(msg.getMessage(), msg.getSignature(), node.gossipSample, msg.getSenderID());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
