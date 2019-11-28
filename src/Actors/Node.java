package Actors;

import Protocol.Message;

import java.io.*;
import java.security.*;
import java.util.*;
import javax.net.ssl.*;

import org.apache.commons.math3.distribution.PoissonDistribution;

public class Node extends Thread {

    SSLServerSocket sslserversocket;
    ArrayList<SSLSocket> nodesSockets = new ArrayList();

    int id;
    Boolean initiator = false;
    Message delivered = null;

    PublicKey pubKey;
    private PrivateKey privateKey;
    static Dictionary nodesInfo = new Hashtable<Integer, PublicKey>();

    Set<Integer> gossipSample = new HashSet<>();

    /***********************    Constructor    **********************/
    public Node(String keystore, int port, int id) throws IOException, NoSuchAlgorithmException {
        this.id = id;
        initSocket(keystore,port);
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

    public void setGossipSample(Set<Integer> gossipSample) {
        this.gossipSample = gossipSample;
    }

    public void setInitiator(Boolean initiator) {
        this.initiator = initiator;
    }

    public Boolean getInitiator() {
        return initiator;
    }

    /************************************************************/
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
    public void initSocket(String keystore, int port) throws IOException{
        System.setProperty("javax.net.ssl.keyStore", keystore);
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        this.sslserversocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(port);
    }

    public int sampleSize(double expectedValue) {
        PoissonDistribution random = new PoissonDistribution(expectedValue); // p = expected value G
        int sampleSize;
        do {
            sampleSize = random.sample();
        } while (sampleSize == 0);
        System.out.println(this.id + "'s SAMPLE SIZE =====================> " + sampleSize);
        return sampleSize;
    }

    public Set<Integer> omega(int sampleSize, int bound) {
        Set targets = new HashSet();
        Random rand = new Random();
        while (targets.size() < sampleSize) {
            int candidate = rand.nextInt(bound + 1);
            System.out.print(candidate + "-");
            if ((!targets.contains(candidate)) && candidate != this.id) {
                targets.add(candidate);
                System.out.println("\n(" + this.id + ") ***** Initial gossip sample ****  " + candidate);
            }
        }
        return targets; //returns ids of the samples
    }


    public Set<Integer> pickGossipSample() {
        int max = Launcher.nodesNbr;
        int sampleSize = sampleSize(max / 2);
        return omega(sampleSize, max);
    }

    /************************************************************/
    public Object receive(SSLSocket sslsocket) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(sslsocket.getInputStream());
        return ois.readObject();
    }

    public void send(SSLSocket sslsocket, Message message) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(sslsocket.getOutputStream());
        oos.writeObject(message);
    }

    /************************************************************/

    public void dispatch(String v, String signature, Set<Integer> destination, int sender) throws Exception {
        if (this.delivered == (null)) {
            this.delivered = new Message(v, sender, this.id, signature);
            for (Integer index : destination) {
                System.out.println(this.id + " =========> " + index); //meaningless
                this.send(this.nodesSockets.get(index), delivered);
                // trigger <pb.Deliver |message>
            }
        }
        else System.out.println("Delivered already updated !!!!!!!!!!");
    }

    /************************************************************/
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