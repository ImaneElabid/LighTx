package Actors;

import Protocol.Converter;


import java.util.*;

public class Launcher {

    public static final int  nodesNbr = 9; // from 0 to 9 = 10

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        ArrayList<Node> nodes = new ArrayList();
        System.out.print("Protocol choice :\n(1) for pb: \n(2) for pcb: \n(3) for psb: ");
        int protocol = sc.nextInt();
        System.out.print("Designated sender id :");
        int sender = sc.nextInt();
        // Start Nodes
        for (int i = 0; i <= nodesNbr; i++) {
            Node n = new Node("C:/Program Files/Java/jdk-12/bin/server" + i + ".jks", Converter.PORT + i, i, protocol);
            nodes.add(n);
            n.start();
        }

        for (int i = 0; i <= nodesNbr; i++) {
            Node n = nodes.get(i);
            if (i == sender) {
                n.setInitiator(true);
            }
            new ClientThread(n, nodesNbr).start();
        }


        Thread.sleep(4000);
        System.out.println("Gossip count : "+stats(nodes)[0]/(nodesNbr+1));
        System.out.println("Echo count : "+stats(nodes)[1]/(nodesNbr+1));
        System.out.println("Ready count : "+stats(nodes)[2]/(nodesNbr+1));

    }
    public static int[] stats(ArrayList<Node> nodes){
        int[] msgcount = new int[3];
        Arrays.fill(msgcount,0);

        for (int i = 0; i <= nodesNbr; i++) {
            Node n = nodes.get(i);
            msgcount[0]+=n.gossipCount;
            msgcount[1]+=n.echoCount;
            msgcount[2]+=n.readyCount;
        }
        return msgcount;
    }

    static int log(int x, int base)
    {
        return (int) (Math.log(x) / Math.log(base));
    }
}
