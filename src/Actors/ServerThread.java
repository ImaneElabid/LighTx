package Actors;

import Protocol.Message;
import java.net.Socket;

public class ServerThread extends Thread {
    Socket socket;
    Node node;

    public ServerThread(Socket socket, Node node) {
        this.socket = socket;
        this.node = node;
    }

    public void run() {
        while (true) {
            try {
                Message msg = (Message) node.receive(socket);
                BroadcastInterface br = node.router.selectBrChannel(msg.getBroadcastLabel());

                switch (msg.getType()) {
                    case "Probe" :
                        node.rcvProbe(msg);
                        break;

                    case "Query" :
                        node.eigenReputation.rcvQuery(msg);
                        break;

                    case "score" :
                        node.eigenReputation.rcvScore(msg);
                        break;

                    case "GossipSub":
                        ((ProbabilisticSecureBroadcast) br).pcb.pb.rcvGossipSub(msg);
                        break;

                    case "EchoSub":
                        ((ProbabilisticSecureBroadcast) br).pcb.rcvEchoSub(msg);
                        break;

                    case "ReadySub":
                        ((ProbabilisticSecureBroadcast) br).rcvReadySub(msg);
                        break;

                    case "Gossip":
                        ((ProbabilisticSecureBroadcast) br).pcb.pb.rcvGossip(msg);
                        break;

                    case "Echo":
                        ((ProbabilisticSecureBroadcast) br).pcb.rcvEcho(msg);
                        break;

                    case "Ready":
                        ((ProbabilisticSecureBroadcast) br).rcvReady(msg);
                        break;
                }
//                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
