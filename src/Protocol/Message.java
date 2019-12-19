package Protocol;

import java.io.Serializable;
import java.util.HashMap;

public class Message implements Serializable {

    private Object message;
    private String type;
    private String signature;
    private int senderID;
    private int forwarderID;
    HashMap<Object, String> pair= new HashMap<>();

    public Message(String type, int id){
        this.type = type;                              //"GossipSub" or "EchoSub" or "ReadySub"
        this.senderID = id;
    }

    public Message(Object message, int senderId,
                   int forwarderID, String signature){
        this.type =  "Gossip";                              //  "Gossip" or "Echo" or "Ready";
        this.message = message;
        this.signature = signature;
        this.senderID = senderId;
        this.forwarderID = forwarderID;
        this.pair.put(message, signature);

    }

    public Object getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getSignature() {
        return signature;
    }

    public int getSenderID() {
        return senderID;
    }

    public int getForwarderID() {
        return forwarderID;
    }

    public void setType(String type) {
        this.type = type;
    }

    public HashMap<Object, String> getPair() {
        return pair;
    }

    public void setForwarderID(int forwarderID) {
        this.forwarderID = forwarderID;
    }
}
