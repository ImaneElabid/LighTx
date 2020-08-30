package Actors;

import Protocol.Converter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import static Protocol.Converter.*;


public class ClientThread extends Thread {
    Node node;
    Socket socket;
    static AtomicBoolean alreadyExecuted = new AtomicBoolean(false);


    public ClientThread(Node node) throws Exception {
        this.node = node;
        initCommunicationChannel();
    }

    /*************************************************************************/

    public void initCommunicationChannel() throws Exception {
        for (int i = 0; i < nodesNbr; i++) {
            int port = Converter.PORT + i;
            this.socket = new Socket(InetAddress.getLocalHost().getHostAddress(),port);
            node.nodesSockets.add(socket); //list of connections
        }
        Node.finished.getAndIncrement();
    }

    public boolean fileIsEmpty(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line = br.readLine();
        if (line.isEmpty())
            return true;
        else
            return false;
    }

    /*************************************************************************/

    public void run() {
        try {
            node.waitTillFinished(250); //wait until all node are ON and connection is established
            node.eigen.SubscribeToPreTrusted(T_SIZE);
            String path = TX_PATH + node.id + ".csv";
//            if (new File(path).isFile()) {
//                if (!fileIsEmpty(path)) {
//                    node.readContent(path);
////                    synchronized (node.content){
//                        while (!node.content.isEmpty()) {
//                        // detects the start time of the broadcast operations
//                        int label = node.generateBrLabel(node.getId() + System.nanoTime());
//                        node.router.selectBrChannel(label).broadcast();
//                    }}
//                }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}