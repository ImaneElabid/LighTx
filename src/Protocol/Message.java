package Protocol;

import java.io.Serializable;

public class Message implements Serializable {

    private String message;
    private String type;
    private String signature;
    private int senderID;
    private int forwarderID;

    public Message(int id){                        // GossipSubscribe
        this.type = "GossipSub";
        this.senderID = id;
    }

    public Message(String message, int id, int forwarderID, String signature){         // GossipMessage
        this.type = "Gossip";
        this.message = message;
        this.senderID = id;
        this.forwarderID = forwarderID;
        this.signature = signature;
    }

    public String getMessage() {
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
}
