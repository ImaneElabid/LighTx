package Actors;

import Protocol.Content;
import Protocol.Message;
import Tools.PoB;
import Tools.Router;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static Protocol.Converter.*;


/*
@param e ExpectedValue for pbBroadcast
@param d threshold for deliveryCheck
@sampleSize echoSample size E
@sampleSize readySample size R
@sampleSize deliverySample size D
@sampleSize PretrustedSet size T
 */

public class Node extends Thread {
    int id;
    ServerSocket socket;
    ArrayList<Socket> nodesSockets = new ArrayList();
    PoB pob;
    ReputationScoreSystem reputationScore;
    /**************************************************************************************/
    PublicKey pubKey;
    private PrivateKey privateKey;
    static HashMap nodesInfo = new HashMap<Integer, PublicKey>();
    HashMap<Integer, ArrayList<Long>> delayMap = new HashMap<>();
    HashMap<Integer, ArrayList<Double>> bwMap = new HashMap<>();
    static AtomicInteger finished = new AtomicInteger(0);  // Label to detect whither initialization is finished or not to start broadcast
    Router router = new Router(this);
    Queue<Content> content = new LinkedList<Content>();     //transactions queue

    /***********************    Constructor    **********************/
    public Node(int port, int id) throws Exception {
        this.id = id;
        this.socket = new ServerSocket(port);
        generateKeys();
        nodesInfo.put(this.id, this.pubKey);
        this.pob=new PoB(this);
        reputationScore = new ReputationScoreSystem(this);
    }

    /*******************   Getters & Setters   *********************/
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public ArrayList<Socket> getNodesSockets() {
        return nodesSockets;
    }

    public int getNodeId() {
        return id;
    }

    public HashMap<Integer, ArrayList<Long>> getDelayMap() {
        return delayMap;
    }

    public HashMap<Integer, ArrayList<Double>> getBwMap() {
        return bwMap;
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

    /****************************Tools********************************/
    public HashSet<Integer> omega(int sampleSize, int bound) {
        HashSet targets = new HashSet();
        Random rand = new Random();
        while (targets.size() < sampleSize) {
            int candidate = rand.nextInt(bound);
            if (candidate != this.id && (!targets.contains(candidate))) {
                targets.add(candidate);
            }
        }
        return targets; //returns ids of the samples
    }

    public ArrayList<Integer> sample(Message m, int size, int nbr) throws IOException {
        ArrayList<Integer> psi = new ArrayList<Integer>();
        int i = 0;
        while (i < size) {
            psi.add(omega(nbr, nodesNbr).iterator().next());
            i++;
        }
        for (int p : psi) {
//            probing(p);
            send(nodesSockets.get(p), m);
        }
        return psi;
    }

    public void waitTillFinished(int delay) throws InterruptedException {
        while (finished.get() < nodesNbr)
            sleep(delay);
    }

    public int generateBrLabel(Object rnd) {
        return rnd.hashCode();
    }

    public Content createContent(double value, int recipient) throws Exception {
        return new Content(value, recipient, this.sign(String.valueOf(value + recipient), this.getPrivateKey()));
    }

    /**********************************Send & receive***********************************/
    public Object receive(Socket socket) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        return ois.readObject();
    }
    // if method not sync => corrupted stream exception. check later use one o5is for same socket

    public synchronized void send(Socket socket, Message message) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(message);
    }
    //Read content from file

    public synchronized void readContent(String path) throws Exception {
        String line = "";
        BufferedReader br = new BufferedReader(new FileReader(path));
        while ((line = br.readLine()) != null) {
            String[] transaction = line.split(";");
            content.add(createContent(Double.valueOf(transaction[1]), Integer.valueOf(transaction[0])));
        }
    }

    //Write content from file

    /*************************************************************************************************/
    public void run() {
        while (true) {
            try {
                Socket sock = socket.accept();
                new ServerThread(sock, this).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}