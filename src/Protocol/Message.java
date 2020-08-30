package Protocol;

import java.io.Serializable;

public class Message implements Serializable {

    private Content content;
    private String type;
    private int senderID;
    private int forwarderID;
    private int broadcastLabel;  // HashCode(broadcastInstance)
    private Long time;
    int tag, round;
    double[] localScore;
            //PROBE
    public Message (Long time,int senderId, String type, int tag){
        this.time = time;
        this.type = type;
        this.senderID = senderId;
        this.tag = tag;
    }

        //PRE-TRUSTED SUBSCRIPTION
    public Message (int senderId, String type){
        this.senderID = senderId;
        this.type = type;   // pre-trusted
    }

        //Score exchange
    public Message (int senderId, String type, double[] score, int round){
        this.type = type;   // scoreMX
        this.senderID = senderId;
        this.localScore=score;
        this.round = round;
    }

        //BROADCAST SUBSCRIPTION MESSAGE
    public Message(String type, int id, int broadcastLabel) {
        this.type = type;                              //New constructor sub
        this.senderID = id;
        this.broadcastLabel=broadcastLabel;
    }

        //BROADCAST PAYLOAD
    public Message(Content content, String type, int senderId,
                   int forwarderID, int broadcastLabel) {
        this.content = content;
        this.type = type;                              //  New constructor
        this.senderID = senderId;
        this.forwarderID = forwarderID;
        this.broadcastLabel = broadcastLabel;
    }

    public Content getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public int getSenderID() {
        return senderID;
    }

    public int getForwarderID() {
        return forwarderID;
    }

    public int getBroadcastLabel() {
        return broadcastLabel;
    }

    public Long getTime(){
        return time;
    }

    public int getTag(){return tag;}

    public int getRound() {
        return round;
    }

    public double[] getLocalScore() {
        return localScore;
    }

    public void setBroadcastLabel(int broadcastLabel) {
        this.broadcastLabel = broadcastLabel;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setForwarderID(int forwarderID) {
        this.forwarderID = forwarderID;
    }

}
