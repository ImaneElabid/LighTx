package Actors;

import Protocol.Converter;
import Protocol.Message;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.*;
import java.util.concurrent.atomic.AtomicReference;


import org.apache.commons.math3.distribution.PoissonDistribution;

/*

@param e ExpectedValue for pbBroadcast
@param d threshold for deliveryCheck
@sampleSize echoSample size E
@sampleSize readySample size R
@sampleSize deliverySample size D

 */
public class Node extends Thread {
    SSLServerSocket sslserversocket;
    ArrayList<SSLSocket> nodesSockets = new ArrayList();
    HashMap<Integer, Message> repliesEcho = new HashMap<Integer, Message>();
    HashMap<Integer, HashSet<HashMap>> readyReplies = new HashMap<Integer, HashSet<HashMap>>(); // indexes = processes, lists are messeges of process(ndex)[[readyMsg0, ReadyMsg1, ...], [readyMsg0, ReadyMsg1, ...], readyMsg0, ReadyMsg1, ...>
    HashMap<Integer, HashSet<HashMap>> deliveryReplies = new HashMap<Integer, HashSet<HashMap>>(); // <process, [deliveryMsg0, DeliveryMsg1, ...]>
    ArrayList<Message> ready = new ArrayList<Message>();
    ArrayList<Object> excluded = new ArrayList<Object>();

    int id;
    static AtomicInteger finished = new AtomicInteger(0);
    Boolean initiator = false; // defines the designated sender
    AtomicReference<Message> delivered = new AtomicReference<>(null);
    Message echo;
    private AtomicBoolean pcbDelivered = new AtomicBoolean(false);
    private AtomicBoolean pbDelivered = new AtomicBoolean(false);  // variable indicates that <pb> delivered
    private AtomicBoolean psbDelivered = new AtomicBoolean(false);
    int protocol;
    PublicKey pubKey;
    private PrivateKey privateKey;
    static HashMap nodesInfo = new HashMap<Integer, PublicKey>();
    HashSet<Integer> gossipSample = new HashSet<>();    // Ğ of size(G)

    ArrayList<Integer> echoSample = new ArrayList<>();      // ℰ of size(E)
    ArrayList<Integer> echoSubscriptionSample = new ArrayList<>();

    ArrayList<Integer> readySample = new ArrayList<>();     // ℛ of size(R)
    ArrayList<Integer> readySubscriptionSample = new ArrayList<>();

    ArrayList<Integer> deliverySample = new ArrayList<>();  // ⅅ of size(D)
    private AtomicBoolean alreadyExecuted = new AtomicBoolean(false);


    static int gossipCount, echoCount, readyCount;
    /***********************    Constructor    **********************/
    public Node(String keystore, int port, int id, int protocol) throws IOException, NoSuchAlgorithmException {
        this.id = id;
        this.protocol = protocol;
        initCommunicationChannel(keystore, port);

        generateKeys();
        nodesInfo.put(this.id, this.pubKey);
    }

    /*******************   Getters & Setters   *********************/
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public void setGossipSample(HashSet<Integer> gossipSample) {
        this.gossipSample = gossipSample;
    }

    public void setEchoSample(ArrayList<Integer> echoSample) {
        this.echoSample = echoSample;
    }

    public void setReadySample(ArrayList<Integer> readySample) {
        this.readySample = readySample;
    }

    public void setDeliverySample(ArrayList<Integer> deliverySample) {
        this.deliverySample = deliverySample;
    }

    public void setInitiator(Boolean initiator) {
        this.initiator = initiator;
    }

    public Boolean getInitiator() {
        return initiator;
    }

    public void setEcho(Message echo) {
        this.echo = echo;
    }

    public void setRepliesReady() {
        for (int rho : readySample) {
            readyReplies.put(rho, new HashSet<HashMap>());
        }
    }

    public void setRepliesDelivery() {
        for (int rho : deliverySample) {
            deliveryReplies.put(rho, new HashSet<HashMap>());
        }
    }

    /**************************Key gen + Signature**************************/
    public void generateKeys() throws NoSuchAlgorithmException {
        KeyPair keyPair = buildKeyPair();
        this.pubKey = keyPair.getPublic();
        this.setPrivateKey(keyPair.getPrivate());
    }

    public static KeyPair buildKeyPair() throws NoSuchAlgorithmException {
        final int keySize = 1024;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.genKeyPair();
    }

    public String sign(String plainText, PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(plainText.getBytes());
        byte[] signature = privateSignature.sign();
        return Base64.getEncoder().encodeToString(signature);
    }

    public boolean verify(String plainText, String signature, PublicKey publicKey) throws Exception {
        Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(publicKey);
        publicSignature.update(plainText.getBytes());
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        return publicSignature.verify(signatureBytes);
    }

    /************************************************************/
    public void initCommunicationChannel(String keystore, int port) throws IOException {
        System.setProperty("javax.net.ssl.keyStore", keystore);
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        this.sslserversocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(port);
    }

    public HashSet<Integer> omega(int sampleSize, int bound) {
        HashSet targets = new HashSet();
        Random rand = new Random();
        while (targets.size() < sampleSize) {
            int candidate = rand.nextInt(bound);
            if ((!targets.contains(candidate))) {
                targets.add(candidate);
//                System.out.println("[" + this.id + "] added : [" + candidate + "]");
            }
        }
        return targets; //returns ids of the samples
    }

    /**********************************Send & receive***********************************/
    public Object receive(SSLSocket sslsocket) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(sslsocket.getInputStream());
        return ois.readObject();
    }

    public void send(SSLSocket sslsocket, Message message) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(sslsocket.getOutputStream());
        oos.writeObject(message);
//        System.out.println("(" + this.id + ") sending \"" + message.getType() + "\" to recipient (" + (sslsocket.getPort() - Converter.PORT) + ") ................ at " + System.currentTimeMillis()); //meaningless

    }

    /**************************************<pb> specific FUNCTIONS****************************************/
    public int sampleSize(double expectedValue) {
        PoissonDistribution random = new PoissonDistribution(expectedValue); // p = expected value G
        int sampleSize;
        do {
            sampleSize = random.sample();
        } while (sampleSize == 0);
        return sampleSize;
    }

    public HashSet<Integer> pickGossipSample() {
        int sampleSize = sampleSize(Converter.EXPECTED_VALUE);
        return omega(sampleSize, Launcher.nodesNbr + 1);
    }

    public void dispatch(Object v, String signature, HashSet<Integer> destination, int sender) throws Exception {
        try {
            Message gossip = new Message(v, sender, this.id, signature);
            if (this.delivered.compareAndSet(null, gossip)) {
//                System.out.println("still null "+System.currentTimeMillis());
//                Message gossip = new Message(v, sender, this.id, signature);
//                setDelivered(gossip);
//                System.out.println("Delivery != null "+System.currentTimeMillis());
                for (Integer index : destination) {
                    this.send(this.nodesSockets.get(index), gossip);
                }
                if (pbDelivered.compareAndSet(false, true)) {
                    pbDeliver();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pbDeliver() throws Exception {
        System.out.println(this.id + " ------ <pb.Deliver> : " + System.currentTimeMillis());
        gotEnoughEchoes(Converter.E_HAT, this.echo);
        echoing(delivered.get());
    }

    /*******************************<pcb> specific FUNCTIONS*******************************************/
    public ArrayList<Integer> sample(Message m, int size) throws IOException {
        ArrayList<Integer> psi = new ArrayList<Integer>();
        int i = 0;
        while (i < size) {
            psi.add(omega(1, Launcher.nodesNbr + 1).iterator().next());
            i++;
        }
        for (int p : psi) {
            send(nodesSockets.get(p), m);
        }
        return psi;
    }

    public void byzantineBroadcast(HashSet<Integer> destination) throws Exception {
        Random rand = new Random();
        String valueZero = "Zero";
        String valueOne = "One";
        if (this.delivered.get() == (null)) {
            for (Integer index : destination) {
                int candidate = rand.nextInt(2);
                String signature = sign(String.valueOf(this.id), getPrivateKey());
                if (candidate == 0) {
                    Message msg1 = new Message(valueZero, id, id, signature);
                    this.delivered.compareAndSet(null, msg1);
                    this.send(this.nodesSockets.get(index), msg1);
                } else if (candidate == 1) {
                    Message msg2 = new Message(valueOne, id, id, signature);
                    this.delivered.compareAndSet(null, msg2);
                    this.send(this.nodesSockets.get(index), msg2);
                }
            }
        }
        pbDeliver();
    }

    public void echoing(Message delivered) throws Exception {
        if (this.protocol == Converter.pcb || this.protocol == Converter.psb) {
            if (this.verify(String.valueOf(delivered.getSenderID()), delivered.getSignature(), (PublicKey) this.nodesInfo.get(delivered.getSenderID()))) {
                Message echo = delivered;
                echo.setType("Echo");
                echo.setForwarderID(this.id);
                this.setEcho(echo);
                for (Integer p : this.echoSubscriptionSample) {
                    this.send(this.nodesSockets.get(p), this.echo);
                }
            }
            gotEnoughEchoes(Converter.E_HAT, this.echo);
        }
    }

    public int count(Message m) {
        int cmpt = 0;
        for (Message value : repliesEcho.values()) {
            if (value != null && m != null)
                if (value.getMessage().equals(m.getMessage()))
                    if (value.getSignature().equals(m.getSignature())) {
                        cmpt++;
                    }
        }
        return cmpt;
    }

    public void gotEnoughEchoes(int e, Message message) throws Exception { // Ȇ
        if (count(message) >= e && pcbDelivered.compareAndSet(false, true)) {
            pcbDeliver();
        }
    }

    public void pcbDeliver() throws Exception {
        System.out.println(this.id + " ----- <pcb.Deliver> : " + System.currentTimeMillis());
        readySend(echo);
    }

    public void waitTillFinished(int delay) throws InterruptedException {
        while (finished.get() <= Launcher.nodesNbr)
            sleep(delay);
        finished.set(0);
    }


    /*******************************<psb> specific FUNCTIONS******************************************/
    public void readySend(Message deliveredFromPcb) throws Exception {
        if (this.protocol == Converter.psb) {
            if (this.verify(String.valueOf(deliveredFromPcb.getSenderID()), deliveredFromPcb.getSignature(), (PublicKey) this.nodesInfo.get(deliveredFromPcb.getSenderID()))) {
                Message ready = deliveredFromPcb;
                ready.setType("Ready");
                ready.setForwarderID(this.id);
                this.ready.add(ready);
                for (Integer p : this.readySubscriptionSample) {
                    this.send(this.nodesSockets.get(p), ready);
                }
            }
        }
    }

    public int counter(HashMap<Integer, HashSet<HashMap>> map, Message m) {
        int cmpt = 0;
        for (HashSet<HashMap> set : map.values()) {
            if (!set.isEmpty())
                for (Iterator<HashMap> it = set.iterator(); it.hasNext(); )
                    if (it.next().equals(m.getPair())) {
                        cmpt++;
                    }
        }
        return cmpt;
    }

    public void gotEnoughReady(int r, Message msg) throws IOException { // Ȓ
        if (counter(readyReplies, msg) >= r) {
            if (alreadyExecuted.compareAndSet(false, true)) { //method executed only once
                Message readyMsg = msg;
                readyMsg.setForwarderID(this.id);
                this.ready.add(readyMsg);
                for (int elm : readySubscriptionSample) {
                    send(nodesSockets.get(elm), readyMsg);
                }
            }
        }
    }

    public void gotEnoughDelivery(int d, Message msg) { // Ď
        if (counter(deliveryReplies, msg) >= d && psbDelivered.compareAndSet(false, true)) {
            psbDeliver();
        }

    }

    private void psbDeliver() {
            System.out.println(this.id + " ---- <psb.Deliver> : "+System.currentTimeMillis());
    }


    /*************************************************************************************************/
    public void run() {
        while (true) {
            try {
                SSLSocket sslsocket = (SSLSocket) sslserversocket.accept();
                new ServerThread(sslsocket, this).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}