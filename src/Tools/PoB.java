package Tools;

import Actors.Node;
import Protocol.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static Protocol.Converter.PROBE_SIZE;

/**
 * @author EMS
 */
public class PoB {
    Node node;
    public PoB(Node node) {
        this.node=node;
    }

    public void probing(int destination) { //create and send probe message
        try {
            Random rand = new Random();
            int tag = rand.nextInt();
            node.send(node.getNodesSockets().get(destination), new Message(System.currentTimeMillis(), node.getNodeId(), "Probe", tag));
            node.send(node.getNodesSockets().get(destination), new Message(System.currentTimeMillis(), node.getNodeId(), "Probe", tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void measureBW(ArrayList<Long> values, int key) {
        for (Long t : values) {
            node.getBwMap().get(key).add(Double.valueOf(t / PROBE_SIZE));
        }
    }

    public void rcvProbe(Message msg) {
        node.getBwMap().put(msg.getSenderID(), new ArrayList<>());
        if (node.getDelayMap().keySet().contains(msg.getTag())) {
            node.getDelayMap().get(msg.getTag()).add(System.currentTimeMillis() - msg.getTime());
        } else {
            node.getDelayMap().put(msg.getTag(), new ArrayList<>(Arrays.asList(System.currentTimeMillis() - msg.getTime())));
        }
        //TODO determine bounds
        if (node.getDelayMap().get(msg.getTag()).size() == 2) {
            System.out.println(node.getId() + " -> " + msg.getSenderID() + " delays = " + node.getDelayMap().get(msg.getTag()));
            measureBW(node.getDelayMap().get(msg.getTag()), msg.getSenderID());
            System.out.println("BW : " +  node.getBwMap());
        }
    }


}
