package Actors;

import Protocol.Converter;


import java.util.ArrayList;
import java.util.Scanner;

public class Launcher {

    static int nodesNbr;

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        ArrayList<Node> nodes = new ArrayList();
        System.out.print("Number of nodes : ");
        nodesNbr = sc.nextInt();
        System.out.print("Designated sender id :");
        int sender = sc.nextInt();

        // Start Nodes
        for (int i = 0; i <= nodesNbr; i++) {
            Node n = new Node("C:/Program Files/Java/jdk-12/bin/server" + i + ".jks", Converter.PORT + i, i);
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
    }
}
