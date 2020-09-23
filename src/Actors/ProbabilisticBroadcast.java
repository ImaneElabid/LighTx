package Actors;

import org.apache.commons.math3.distribution.PoissonDistribution;
import Protocol.Content;
import Protocol.Converter;
import Protocol.Message;

import java.io.IOException;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static Protocol.Converter.nodesNbr;

/**
 * @author EMS
 */
public class ProbabilisticBroadcast implements BroadcastInterface {
    AtomicBoolean pbDelivered = new AtomicBoolean(false);  // variable indicates that <ProbabilisticBroadcast> delivered
    AtomicReference<Message> delivered = new AtomicReference<>(null);
    ProbabilisticConsistentBroadcast pcbUpper;
    HashSet<Integer> gossipSample = new HashSet<>();
    Node node;
    int brLabel;

    /*************************************************************/
    public ProbabilisticBroadcast(Node node, ProbabilisticConsistentBroadcast pcb, int brLabel) {
        this.node = node;
        this.brLabel = brLabel;
        this.pcbUpper = pcb;
        init(Converter.EXP, 0);
    }

    /*************************************************************/
    public void setGossipSample(HashSet<Integer> gossipSample) {
        this.gossipSample = gossipSample;
    }

    /*************************************************************/
    public int sampleSize(int expectedValue) {
        PoissonDistribution random = new PoissonDistribution(expectedValue); // p = expected value G
        int sampleSize;
        do {
            sampleSize = random.sample();
        } while (sampleSize == 0);
        return sampleSize;
    }

    public HashSet<Integer> pickGossipSample(int expectedValue) {
        int sampleSize = sampleSize(expectedValue);
        return node.omega(sampleSize, nodesNbr);
    }

    public void dispatch(Content content, HashSet<Integer> destination, int sender) {
        try {

            Message gossip = new Message(content, "Gossip", sender, node.id, brLabel);
            if (this.delivered.compareAndSet(null, gossip)) {
                synchronized (node) {
                    for (Integer index : destination) {
                        node.send(node.nodesSockets.get(index), gossip);
                    }
                }
                if (pbDelivered.compareAndSet(false, true)) {
                    this.deliver(delivered.get());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void rcvGossipSub(Message msg) throws IOException {
        if (delivered.get() != null) {
            Message redirected = delivered.get();
            redirected.setType("Gossip");
            redirected.setForwarderID(node.id);
            node.send(node.nodesSockets.get(msg.getSenderID()), redirected);
        }
        gossipSample.add(msg.getSenderID());
    }

    public void rcvGossip(Message gossip) {
        try {
            if (node.verify(String.valueOf(gossip.getContent().getValue() + gossip.getContent().getRecipientID()),
                    gossip.getContent().getSignature(), (PublicKey) Node.nodesInfo.get(gossip.getSenderID()))) {
                synchronized (delivered) {
                    dispatch(gossip.getContent(), gossipSample, gossip.getSenderID());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getLabel() {
        return brLabel;
    }

    @Override
    public void init(int expectedValue, int useless) {
        setGossipSample(pickGossipSample(expectedValue));
        Message gossipSub = new Message("GossipSub", node.id, brLabel);
        for (Integer index : gossipSample) {
            try {
                //TODO when to send probe message?
                node.pob.probing(index); // send probe messages
                node.send(node.nodesSockets.get(index), gossipSub);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void broadcast() {
        // TODO test if not null
        Content content = node.content.poll();
        dispatch(content, gossipSample, node.id);
    }

    @Override
    public void deliver(Message msgToDeliver) throws Exception {
//        String display = delivered.get().getContent().getValue() + ":" + delivered.get().getContent().getRecipientID();
//        System.out.println(node.id + ") ---- <pb.Deliver> : " + display+ " From ("+msgToDeliver.getSenderID()+")");
        pcbUpper.sendEcho(delivered.get());
        pcbUpper.gotEnoughEchoes(Converter.E_HAT, pcbUpper.echo);
    }
}
