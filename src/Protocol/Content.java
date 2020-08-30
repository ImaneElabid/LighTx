package Protocol;

import java.io.Serializable;

/**
 * @author EMS
 */
public class Content implements Serializable {

    private double value;
    private int recipientID;
    private String signature;

    public Content(double value, int recipientID, String signature) {
        this.value = value;
        this.recipientID = recipientID;
        this.signature = signature;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getRecipientID() {
        return recipientID;
    }

    public String getSignature() {
        return signature;
    }
}
