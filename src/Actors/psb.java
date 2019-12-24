package Actors;

import Protocol.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author EMS
 */
public class psb {

    private AtomicBoolean psbDelivered = new AtomicBoolean(false);
    HashMap<Integer, HashSet<HashMap>> readyReplies = new HashMap<Integer, HashSet<HashMap>>(); // indexes = processes, lists are messeges of process(ndex)[[readyMsg0, ReadyMsg1, ...], [readyMsg0, ReadyMsg1, ...], readyMsg0, ReadyMsg1, ...>
    HashMap<Integer, HashSet<HashMap>> deliveryReplies = new HashMap<Integer, HashSet<HashMap>>(); // <process, [deliveryMsg0, DeliveryMsg1, ...]>
    ArrayList<Message> ready = new ArrayList<Message>();

    ArrayList<Integer> readySample = new ArrayList<>();     // ℛ of size(R)
    ArrayList<Integer> readySubscriptionSample = new ArrayList<>();

    ArrayList<Integer> deliverySample = new ArrayList<>();  // ⅅ of size(D)
    private AtomicBoolean alreadyExecuted = new AtomicBoolean(false);

    /*************************************************************/

    public void setRepliesDelivery() {
        for (int rho : deliverySample) {
            deliveryReplies.put(rho, new HashSet<HashMap>());
        }
    }
    public void readySend(Message deliveredFromPcb){}
    public void setReadySample(ArrayList<Integer> readySample) {
        this.readySample = readySample;
    }
    public void setDeliverySample(ArrayList<Integer> deliverySample) {
        this.deliverySample = deliverySample;
    }

    /*************************************************************/
    public void setRepliesReady() {
        for (int rho : readySample) {
            readyReplies.put(rho, new HashSet<HashMap>());
        }
    }
    public void rcvReadySub(Message msg) {}
    public void rcvReady(Message msg){}
    public int counter(HashMap<Integer, HashSet<HashMap>> map, Message m) {return 0;}
    public void gotEnoughReady(int r, Message msg){}
    public void gotEnoughDelivery(int d, Message msg) {}
    private void psbDeliver(){}
    }
