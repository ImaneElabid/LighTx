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
public class pcb {

    ArrayList<Integer> echoSample = new ArrayList<>();      // ℰ of size(E)
    ArrayList<Integer> echoSubscriptionSample = new ArrayList<>();
    private AtomicBoolean pcbDelivered = new AtomicBoolean(false);
    Message echo;
    HashMap<Integer, Message> repliesEcho = new HashMap<Integer, Message>();

    /*************************************************************/
    public void setEchoSample(ArrayList<Integer> echoSample) {
        this.echoSample = echoSample;
    }
    public void setEcho(Message echo) {
        this.echo = echo;
    }

    /*************************************************************/

    public void pcbInit(int size){}
    public void pcbBroadcast(){}
    public void echoing(Message delivered){}
    public void rcvEchoSub(Message msg){}
    public void rcvEcho(Message msg){}
    public int count(Message m) {return 0;}
    public void gotEnoughEchoes(int e, Message message){}
    public void pcbDeliver(){}


}
