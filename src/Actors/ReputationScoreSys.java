package Actors;

import Protocol.Message;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static Protocol.Converter.*;
import static java.lang.Thread.sleep;

/**
 * @author EMS
 */

public class EigenReputation {
    Node node;
    AtomicInteger sendingRound = new AtomicInteger(0), roundOfComputation = new AtomicInteger(0);
    double[] p = new double[nodesNbr];
    double[] c_i = new double[nodesNbr];
    double globalTrust = 0;

    AtomicBoolean converged = new AtomicBoolean(false);

    ArrayList<Integer> a_i = new ArrayList<>();      // size(T) // from whom I receive
    ArrayList<Integer> b_i = new ArrayList<>(); //to whom i send
    ArrayList<Integer> preTrustedSet = new ArrayList<>(); //to whom i send
    HashMap<Integer, ArrayList<Message>> received = new HashMap<Integer, ArrayList<Message>>();

    /************************************************************************/
    public EigenReputation(Node n) {
        this.node = n;
        SubscribeToPreTrusted(T_SIZE);
        setScore(nodesNbr);
    }
    /************************************************************************/
    private void setPreTrustedSet(ArrayList<Integer> preTrustedSet) {
        this.preTrustedSet = preTrustedSet;
    }

    public void SubscribeToPreTrusted(int size) { //distinct elements
        setPreTrustedSet(new ArrayList<>(TRUSTED_POOL));
        this.p = computePretrustVector();
    }

    public double[] computePretrustVector() {
        for (int k = 0; k < nodesNbr; k++) {
            if (preTrustedSet.contains(k))
                p[k] = 1. / T_SIZE;
            else
                p[k] = 0.;
        }
//        System.out.println(Arrays.toString(p));
        return p;
    }

    public void setScore(int size) {
        Random r = new Random();
        for (int j = 0; j < size; j++) {
            this.c_i[j] = r.nextInt(2);
        }
//        System.out.println(node.id+" : "+Arrays.toString(c_i));
    }

    public void setA_iSet() {
        Message query = new Message(node.id, "Query");
        for (int peer = 0; peer < nodesNbr; peer++) {
            if (c_i[peer] != 0.)
                a_i.add(peer);
        }
        for (int p : a_i) {
            try {
                node.send(node.nodesSockets.get(p), query);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void rcvQuery(Message msg) {
        b_i.add(msg.getSenderID());
        sendLocalScore(c_i, p[node.id]); //send my scoreMX
    }

    public void sendLocalScore(double[] c_i, double t_k) {  // send node.c_ij*ti
        for (Integer index : b_i) {
            double score = c_i[index] * t_k;
            Message localScore = new Message(node.id, "score", score, sendingRound.get());
            try {
                node.send(node.nodesSockets.get(index), localScore);
//                System.out.println("(" + node.id +") sent score to: (" + index + ") at iteration[" + sendingRound.get()+"]");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void rcvScore(Message msg) {
        if (!converged.get()) {
            StoreReceivedScores(msg);
//            System.out.println(node.id + ") received score [" + msg.getRound() + "] from (" + msg.getSenderID()+")");
            if (received.containsKey(roundOfComputation.get())) {
                if (received.get(roundOfComputation.get()).size() < a_i.size()) { //a_i.size()
                    try {
                        sleep(150);
                        System.out.println(node.id + ") waiting for round.. " + roundOfComputation.get());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    computationProcess();
                }
            }
        }
    }

    public void StoreReceivedScores(Message msg) {
        if (!received.containsKey(msg.getRound())) {
            received.put(msg.getRound(), new ArrayList<>(Arrays.asList(msg)));
        } else if (received.get(msg.getRound()).size() < a_i.size()) {
            received.get(msg.getRound()).add(msg);
        }
//        received.forEach((key, value) -> System.out.println(node.id+") "+key + ":" + value));
    }

    public void computationProcess() {
        double t_new = 0;
        for (int i = 0; i < a_i.size(); i++) { //sum received scores
            t_new += received.get(roundOfComputation.get()).get(i).getLocalScore();
        }
        ComputeGlobalScore(p, t_new);
    }

    public void ComputeGlobalScore(double[] p, double t_new) {
        double t_old = t_new;

        t_new = (1 -dampingFactor) * t_new + ( dampingFactor) * p[node.id];
        System.out.println("(" + node.id + ") t_new of iteration [" + sendingRound.get() + "//" + roundOfComputation.get() + "] is " + t_new);
        roundOfComputation.incrementAndGet();
        sendingRound.incrementAndGet();
        this.globalTrust = t_new;

        if (hasConverged(t_new, t_old, EPSILON)) {
            System.out.println("(" + node.id + ") final globalTrust : " + t_new);
            sendLocalScore(c_i, globalTrust);
            converged.set(true);
        } else {
            sendLocalScore(c_i, globalTrust);
        }
    }

    public boolean hasConverged(double t_new, double t_old, double epsilon) {
        return Math.abs(t_new - t_old) < epsilon;
    }
}
