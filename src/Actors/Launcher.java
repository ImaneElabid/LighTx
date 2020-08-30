package Actors;

import Protocol.Converter;

import static Protocol.Converter.nodesNbr;

import java.util.ArrayList;

public class Launcher {

    public static void main(String[] args) throws Exception {
        ArrayList<Node> nodes = new ArrayList();
        // Start server nodes
        for (int i = 0; i < nodesNbr; i++) {
            Node n = new Node(Converter.PORT + i, i);
            nodes.add(n);
            n.start();
        }

        // Launch client nodes
        for (int i = 0; i < nodesNbr; i++) {
            Node n = nodes.get(i);
            new ClientThread(n).start();
        }

    }
}