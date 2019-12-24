package Actors;

import Protocol.Converter;
import Protocol.Message;

import java.io.IOException;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author EMS
 */
public class pb {
    private AtomicBoolean pbDelivered = new AtomicBoolean(false);  // variable indicates that <pb> delivered
    AtomicReference<Message> delivered = new AtomicReference<>(null);
    HashSet<Integer> gossipSample = new HashSet<>();

    /*************************************************************/
    public void setGossipSample(HashSet<Integer> gossipSample) {
        this.gossipSample = gossipSample;
    }
    /*************************************************************/


    public void pbInit(){};
    public void pbBroadcast(){};
    public HashSet<Integer> pickGossipSample() {return null;}
    public void dispatch(Object v, String signature, HashSet<Integer> destination, int sender){}

    public void rcvGossipSub(Message msg){}
    public void rcvGossip(Message msg) {}

    public void pbDeliver(){}
    public void byzantineBroadcast(HashSet<Integer> destination){}
}
