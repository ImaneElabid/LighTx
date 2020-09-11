package Actors;

import Protocol.Content;
import Protocol.Converter;
import Protocol.Message;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author EMS
 */
public class ProbabilisticSecureBroadcast implements BroadcastInterface {

    private AtomicBoolean psbDelivered = new AtomicBoolean(false);
    HashMap<Integer, ArrayList<Content>> readyReplies = new HashMap<>(); // indexes = processes, lists are messeges of process(ndex)[[readyMsg0, ReadyMsg1, ...], [readyMsg0, ReadyMsg1, ...], readyMsg0, ReadyMsg1, ...>
    HashMap<Integer, ArrayList<Content>> deliveryReplies = new HashMap<>(); // <process, [deliveryMsg0, DeliveryMsg1, ...]>
    ArrayList<Message> ready = new ArrayList<Message>();
    ArrayList<Integer> readySample = new ArrayList<>();     // ℛ of size(R)
    ArrayList<Integer> deliverySample = new ArrayList<>();  // ⅅ of size(D)
    ArrayList<Integer> readySubscriptionSample = new ArrayList<>();
    AtomicBoolean alreadyExecuted = new AtomicBoolean(false);
    Node node;
    int brLabel;
    ProbabilisticConsistentBroadcast pcb;


    /*********************************************************************************************/
    public ProbabilisticSecureBroadcast(Node node, int brLabel) {
        this.brLabel = brLabel;
        this.node = node;
        this.pcb = new ProbabilisticConsistentBroadcast(node, this, brLabel);
        init(Converter.R_SIZE, Converter.D_SIZE);
    }

    /*********************************************************************************************/
    public void setRepliesDelivery() {
        for (int rho : deliverySample) {
            deliveryReplies.put(rho, new ArrayList<>());
        }
    }

    public void setReadySample(ArrayList<Integer> readySample) {
        this.readySample = readySample;
    }

    public void setDeliverySample(ArrayList<Integer> deliverySample) {
        this.deliverySample = deliverySample;
    }

    public void setRepliesReady() {
        for (int rho : readySample) {
            readyReplies.put(rho, new ArrayList<>());
        }
    }

    /*************************************************************/
    public void sendReadyMsg(Message pcbMsg) throws Exception {
        if (node.verify(String.valueOf(pcbMsg.getContent().getValue() + pcbMsg.getContent().getRecipientID()),
                pcbMsg.getContent().getSignature(), (PublicKey) node.nodesInfo.get(pcbMsg.getSenderID()))) {
            Message ready = pcbMsg;
            ready.setType("Ready");
            ready.setForwarderID(node.id);
            ready.setBroadcastLabel(brLabel);
            this.ready.add(ready);
            synchronized (node) {
                for (Integer p : readySubscriptionSample) {
                    node.send(node.nodesSockets.get(p), ready);
                }
            }
        }
    }

    public void rcvReadySub(Message msg) throws IOException {
        synchronized (node) {
            for (Message ready : ready) {
                ready.setType("Ready");
                ready.setForwarderID(node.id);
                ready.setBroadcastLabel(brLabel);
                node.send(node.nodesSockets.get(msg.getSenderID()), ready);
            }
        }
        readySubscriptionSample.add(msg.getSenderID());
    }

    public void rcvReady(Message ready) throws Exception {
        if (node.verify(String.valueOf(ready.getContent().getValue() + ready.getContent().getRecipientID()),
                ready.getContent().getSignature(), (PublicKey) node.nodesInfo.get(ready.getSenderID()))) {
            Content reply = ready.getContent();
            if (readySample.contains(ready.getForwarderID())) {
                readyReplies.get(ready.getForwarderID()).add(reply);
                gotEnoughReady(Converter.R_HAT, ready);
            }
            if (deliverySample.contains(ready.getForwarderID())) {
                deliveryReplies.get(ready.getForwarderID()).add(reply);
                gotEnoughDelivery(Converter.D_HAT, ready);
            }
        }
    }

    public synchronized int counter(HashMap<Integer, ArrayList<Content>> replies, Message m) {
        int cmpt = 0;
        for (ArrayList<Content> listOfMessages : replies.values()) {
            if (!listOfMessages.isEmpty())
                for (Content content : listOfMessages) {

                    if ((content.getValue()) == (m.getContent().getValue())
                            && (content.getRecipientID() == m.getContent().getRecipientID())
                            && (content.getSignature().equals(m.getContent().getSignature()))) {
                        cmpt++;
                    }
                }
        }
        return cmpt;
    }

    public void gotEnoughReady(int r, Message msg) throws IOException { // Ȓ
        if (counter(readyReplies, msg) >= r) {
            if (alreadyExecuted.compareAndSet(false, true)) { //method executed only once
                {
                    Message readyMsg = msg;
                    readyMsg.setForwarderID(node.id);
                    readyMsg.setBroadcastLabel(brLabel);
                    ready.add(readyMsg);
                    synchronized (node) {
                        for (int elm : readySubscriptionSample) {
                            node.send(node.nodesSockets.get(elm), readyMsg);
                        }
                    }
                }
            }
        }
    }

    public void gotEnoughDelivery(int d, Message msg) { // Ď
        if (counter(deliveryReplies, msg) >= d && psbDelivered.compareAndSet(false, true)) {
            this.deliver(msg);
        }

    }

    @Override
    public int getLabel() {
        return this.brLabel;
    }

    @Override
    public void init(int R, int D) {
        Message readySub = new Message("ReadySub", node.id, brLabel);
        try {
            setReadySample(node.sample(readySub, R, 1));
            setRepliesReady();
            setDeliverySample(node.sample(readySub, D, 1));
            setRepliesDelivery();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void broadcast() {
        this.pcb.broadcast();
    }

    @Override
    public void deliver(Message psbDelivredMsg) {
        String display = psbDelivredMsg.getContent().getValue() + ":" + psbDelivredMsg.getContent().getRecipientID();

        System.out.println(node.id + ") ---- <psb.Deliver> : " + display + " from (" + psbDelivredMsg.getSenderID() + ")");
    }
}
