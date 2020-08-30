package Actors;

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
public class ProbabilisticConsistentBroadcast implements BroadcastInterface {

    ArrayList<Integer> echoSample = new ArrayList<>();      // ℰ of size(E)
    ArrayList<Integer> echoSubscriptionSample = new ArrayList<>();
    AtomicBoolean pcbDelivered = new AtomicBoolean(false);
    HashMap<Integer, Message> echoReplies = new HashMap();
    Message echo;
    Node node;
    int brLabel;
    ProbabilisticBroadcast pb;
    ProbabilisticSecureBroadcast psbUpper;

    /*************************************************************/

    public ProbabilisticConsistentBroadcast(Node node, ProbabilisticSecureBroadcast psb, int brLabel) {
        this.brLabel = brLabel;
        this.node = node;
        this.psbUpper = psb;
        this.pb = new ProbabilisticBroadcast(node, this, brLabel);
        init(Converter.E_SIZE, 0);
    }

    /*************************************************************/
    public void setEchoSample(ArrayList<Integer> echoSample) {
        this.echoSample = echoSample;
    }

    private void setEcho(Message echo) {
        this.echo = echo;
    }

    /*************************************************************/
    public synchronized void sendEcho(Message pbMsg) throws Exception {
        if (node.verify(String.valueOf(pbMsg.getContent().getValue() + pbMsg.getContent().getRecipientID()),
                pbMsg.getContent().getSignature(), (PublicKey) node.nodesInfo.get(pbMsg.getSenderID()))) {
            synchronized (this.node) {
                Message echo = pbMsg;
                echo.setType("Echo");
                echo.setForwarderID(node.id);
                echo.setBroadcastLabel(brLabel);
                this.setEcho(echo);
            }
            for (Integer p : this.echoSubscriptionSample) {
                node.send(node.nodesSockets.get(p), this.echo);
            }
        }
        gotEnoughEchoes(Converter.E_HAT, this.echo);
    }

    public void rcvEchoSub(Message msg) throws IOException {
        if (this.echo != null) {
            Message redirected = echo;
            redirected.setType("Echo");
            redirected.setForwarderID(node.id);
            node.send(node.nodesSockets.get(msg.getSenderID()), redirected);
        }
        echoSubscriptionSample.add(msg.getSenderID());
    }

    public void rcvEcho(Message echo) throws Exception {
        if (echoSample.contains(echo.getForwarderID())
                && echoReplies.get(echo.getForwarderID()) == null
                && node.verify(String.valueOf(echo.getContent().getValue() + echo.getContent().getRecipientID()),
                echo.getContent().getSignature(), (PublicKey) node.nodesInfo.get(echo.getSenderID()))) {
            echoReplies.put(echo.getForwarderID(), echo);
        }
        gotEnoughEchoes(Converter.E_HAT, echo);
    }

    public int count(Message m) {
        int cmpt = 0;
        synchronized (echoReplies) {
            for (Message msg : echoReplies.values()) {
                if (msg != null && m != null) {
                    if ((msg.getContent().getValue()) == (m.getContent().getValue())
                            && (msg.getContent().getRecipientID() == m.getContent().getRecipientID())
                            && (msg.getContent().getSignature().equals(m.getContent().getSignature())))
                        cmpt++;
                }
            }
        }
        return cmpt;
    }

    public synchronized void gotEnoughEchoes(int e, Message message) throws Exception { // Ȇ
        if (count(message) >= e && pcbDelivered.compareAndSet(false, true)) {
            this.deliver(message);
        }
    }

    @Override
    public int getLabel() {
        return brLabel;
    }

    @Override
    public void init(int size, int useless) {
        Message echoSub = new Message("EchoSub", node.id, brLabel);
        try {
            setEchoSample(node.sample(echoSub, size,1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void broadcast() {
        this.pb.broadcast();
    }

    @Override
    public void deliver(Message pcbDeliveredMsg) throws Exception {
//        String display = pcbDeliveredMsg.getContent().getValue() + ":" + pcbDeliveredMsg.getContent().getRecipientID();
//        System.out.println(node.id + ") ---- <pcb.Deliver> : " + display+ " from ("+pcbDeliveredMsg.getSenderID()+")");
//        psbUpper.sendReadyMsg(this.echo);
        psbUpper.sendReadyMsg(pcbDeliveredMsg);
    }
}
