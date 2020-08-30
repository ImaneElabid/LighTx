package Actors;

import Protocol.Content;
import Protocol.Message;
import Tools.Router;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static Protocol.Converter.PROBE_SIZE;
import static Protocol.Converter.nodesNbr;


/*
@param e ExpectedValue for pbBroadcast
@param d threshold for deliveryCheck
@sampleSize echoSample size E
@sampleSize readySample size R
@sampleSize deliverySample size D
@sampleSize PretrustedSet size T
 */

public class Node extends Thread {
    private ServerSocket socket;
    ArrayList<Socket> nodesSockets = new ArrayList();
    int id;
    Eigen eigen;
    /**************************************************************************************/
    PublicKey pubKey;
    private PrivateKey privateKey;
    static HashMap nodesInfo = new HashMap<Integer, PublicKey>();
    HashMap<Integer, ArrayList<Long>> delayMap = new HashMap<>();
    HashMap<Integer, ArrayList<Double>> bwMap = new HashMap<>();

    // Label to detect whither initialization is finished or not to start broadcast
    static AtomicInteger finished = new AtomicInteger(0);

    // Router
    Router router = new Router(this);

    //transactions queue
    Queue<Content> content = new LinkedList<Content>();

    /***********************    Constructor    **********************/
    public Node(int port, int id) throws Exception {
        this.id = id;
        this.socket = new ServerSocket(port);
        generateKeys();
        nodesInfo.put(this.id, this.pubKey);
         eigen = new Eigen(this);
    }

    /*******************   Getters & Setters   *********************/
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
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

    public void probing(int destination) { //create and send probe message
        try {
            Random rand = new Random();
            int tag = rand.nextInt();
            send(nodesSockets.get(destination), new Message(System.currentTimeMillis(), this.id, "Probe", tag));
            send(nodesSockets.get(destination), new Message(System.currentTimeMillis(), this.id, "Probe", tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void rcvProbe(Message msg) {
        bwMap.put(msg.getSenderID(), new ArrayList<>());
        if (delayMap.keySet().contains(msg.getTag())) {
            delayMap.get(msg.getTag()).add(System.currentTimeMillis() - msg.getTime());
        } else {
            delayMap.put(msg.getTag(), new ArrayList<>(Arrays.asList(System.currentTimeMillis() - msg.getTime())));
        }
        if (delayMap.get(msg.getTag()).size() == 2) {
            System.out.println(id + " -> " + msg.getSenderID() + " delays = " + delayMap.get(msg.getTag()));
            measureBW(delayMap.get(msg.getTag()), msg.getSenderID());
            System.out.println("BW : " + bwMap);
        }
    }

    public void measureBW(ArrayList<Long> values, int key) {
        for (Long t : values) {
            bwMap.get(key).add(Double.valueOf(t / PROBE_SIZE));
        }
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