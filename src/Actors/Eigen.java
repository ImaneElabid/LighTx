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


public class Eigen {
    Node node;
    AtomicInteger roundSending = new AtomicInteger(0), roundOfComputation = new AtomicInteger(0);
    double[] p = new double[nodesNbr];
    double[] c_i = new double[nodesNbr];
    //    double[] globalTrust = new double[nodesNbr];
    double[][] C = new double[nodesNbr][nodesNbr]; //C initialized to zero

    AtomicBoolean alreadyExecuted = new AtomicBoolean(false);
    AtomicBoolean converged = new AtomicBoolean(false);

    ArrayList<Integer> preTrustedSet = new ArrayList<>();      // size(T) // from whom I receive
    ArrayList<Integer> preTrustedSubscriptionSet = new ArrayList<>(); //to whom i send
    HashMap<Integer, ArrayList<Message>> receivedScores = new HashMap<Integer, ArrayList<Message>>();

    /************************************************************************/

    public Eigen(Node n) {
        this.node = n;
        this.p = computePretrustVector();
        setLocalScore(nodesNbr);
    }

    /************************************************************************/
    public void setPreTrustedSet(ArrayList<Integer> preTrustedSet) {
        this.preTrustedSet = preTrustedSet;
    }

    public void setLocalScore(int size) {
        Random r = new Random(node.id);
        for (int j = 0; j < size; j++) {
            this.c_i[j] = r.nextInt(2);
        }
        initScoreMatrix(c_i);
    }

    public void initScoreMatrix(double[] myScore) {
        C[node.id] = myScore; // add local scoreMX to the row corresponding to node's id
    }

    public double[] computePretrustVector() {
        for (int k = 0; k < nodesNbr; k++) {
            p[k] = 1. / nodesNbr;
        }
        return p;
    }

    //subscription to trust set
    public void SubscribeToPreTrusted(int size) { //distanct elements
        Message preTrusted = new Message(node.id, "Pre-Trusted");
        try {
            HashSet<Integer> sample = node.omega(size, nodesNbr);
            setPreTrustedSet(new ArrayList<>(sample));
            for (int p : preTrustedSet) {
                node.send(node.nodesSockets.get(p), preTrusted);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void rcvPreTrustedSubscription(Message msg) {
        preTrustedSubscriptionSet.add(msg.getSenderID());
        try {
            sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendLocalScore(c_i); //send my scoreMX
    }

    public double[][] matrixNormalization(final double[][] matrix, double[] p) { //opinionCount = node.c_i
        double[][] C_new = new double[matrix.length][matrix.length];
        // normalize matrix column-wise
        for (int col = 0; col < C_new.length; col++) {
            int colSum = 0;

            for (int row = 0; row < C_new[col].length; row++)
                colSum += matrix[row][col];

            if (colSum > 0) {
                // at least someone has an opinion
                final double normalization = colSum;
                for (int row = 0; row < C_new[col].length; row++)
                    C_new[row][col] = matrix[row][col] / normalization;
            } else {
                // if a peer trusts/knows no-one
                for (int row = 0; row < C_new[col].length; row++)
                    C_new[row][col] = p[row];
            }
        }
        return C_new;
    }

    public void sendLocalScore(double[] score) {  // send node.c_i
        Message localScore = new Message(node.id, "score", score, roundSending.get());
        for (Integer index : preTrustedSubscriptionSet) {
            try {
                node.send(node.nodesSockets.get(index), localScore);
//                System.out.println(" Score sent from: " + node.id + " to: " + index +"  at iteration: " + roundSending.get());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(node.id + ") roundSending : " + roundSending.incrementAndGet());
    }

    public void computationProcess() {
        for (int i = 0; i < preTrustedSet.size(); i++)
            C[receivedScores.get(roundOfComputation.get()).get(i).getSenderID()] = receivedScores.get(roundOfComputation.get()).get(i).getLocalScore();
//        receivedScores.remove(roundOfComputation);
        ComputeGlobalScoreNew(p, C);
    }

    public void StoreReceivedScores(Message msg) {
        if (receivedScores.containsKey(msg.getRound()) && receivedScores.get(msg.getRound()).size() < 2) {
            receivedScores.get(msg.getRound()).add(msg);
        } else {
            receivedScores.put(msg.getRound(), new ArrayList<Message>());
        }
        receivedScores.entrySet().forEach(entry -> {
            System.out.println(node.id + ") " + entry.getKey() + " : " + entry.getValue());
        });
    }

    public void rcvScore(Message msg) {
        if (!converged.get()) {
            StoreReceivedScores(msg);
            System.out.println(node.id + ") received score [" + msg.getRound() + "] from " + msg.getSenderID());
            if (receivedScores.containsKey(roundOfComputation.get())) {
                if (receivedScores.get(roundOfComputation.get()).size() < preTrustedSet.size()) { //preTrustedSet.size()
                    try {
                        sleep(100);
                        System.out.println(node.id + ") waiting for .. " + roundOfComputation.get());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (alreadyExecuted.compareAndSet(false, true))
                        computationProcess();
                }
            }
        }
    }

    public void ComputeGlobalScoreNew(double[] p, double[][] scoreMatrix) {
        // normalized matrix C
        double[][] C = matrixNormalization(scoreMatrix, p);
        // execute algorithm
        double[] t_new = new double[p.length];
        double[] t_old = new double[p.length];
        // t_new = p
        System.arraycopy(p, 0, t_new, 0, p.length);
        // t_old = t_new
        System.arraycopy(C[node.id], 0, t_old, 0, C[node.id].length);
        // t_new = C * t_old
        for (int i = 0; i < p.length; i++) {
            double sum = 0;
            for (int j = 0; j < p.length; j++)
                sum += C[node.id][j] * C[j][i];
            t_new[i] = sum;
        }
        // t_new = (1 - weight) * t_new + weight * p
        for (int i = 0; i < t_old.length; i++)
            t_new[i] = (dampingFactor) * t_new[i] + (1 - dampingFactor) * p[i];
        System.out.println("(" + node.id + ") t_new of iteration [" + roundSending.get() + "//" + roundOfComputation.get() + "] is " + Arrays.toString(t_new));

        if (hasConverged(t_new, t_old, EPSILON)) {
            final Map<Integer, Double> trust = new LinkedHashMap<Integer, Double>();
            for (int i = 0; i < t_old.length; i++)
                trust.put(i, t_new[i]);
            System.out.println("(" + node.id + ") finale c_i : " + trust);
            converged.set(true);
        } else {
            this.c_i = t_new;
            C[node.id] = t_new;
            System.out.println(node.id + ") roundOfComputation : " + roundOfComputation.incrementAndGet());
            sendLocalScore(c_i);
        }
    }


    public boolean hasConverged(double[] t_new, double[] t_old, double epsilon) {
        double sum = 0;
        for (int i = 0; i < t_old.length; i++)
            sum += (t_new[i] - t_old[i]) * (t_new[i] - t_old[i]);
        return Math.sqrt(sum) < epsilon;
    }
}
