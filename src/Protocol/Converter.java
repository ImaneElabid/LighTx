package Protocol;


import java.util.ArrayList;
import java.util.Random;

import static java.lang.Math.round;

public class Converter {
    public static int PORT = 9000;

    public static final int nodesNbr = 10; // with ids from 0 to 9
    public static final double PROBE_SIZE = 273.; // with ids from 0 to 9
    public static final Double EPSILON = 0.1;
    public static final Double dampingFactor = 0.3;
    // Number of Byzantine nodes


    // Log(N) with N = Network size
    public static int log = round(log(nodesNbr, 2));

    // Expected value
    public final static int EXP = log;                   //====     EXP : Expected value for Poisson distribution   ====//

    // Different delivery thresholds
    public final static int E_HAT = log;           //====     Ȇ : Delivery threshold for <pcb>     ====//
    public final static int R_HAT = log;          //====      Ȓ : Ready threshold for <psb>       ====//
    public final static int D_HAT = log+1;         //====       Ď: Delivery threshold for <psb>    ====//

    // Samples sizes
    public final static int E_SIZE = log+1;                   //====           E: echo sample size           ====//
    public final static int R_SIZE = log+1; //log + 1;       //====           R: ready sample size          ====//
    public final static int D_SIZE = log+1;                 //====           D: delivery sample size       ====//
    public final static int P_SIZE = nodesNbr/2;              //====     size of the pool of trusted nodes    ====//
    public final static ArrayList<Integer> TRUSTED_POOL = sample(P_SIZE, nodesNbr);

    public static ArrayList<Integer> sample(int sampleSize, int bound) {
        ArrayList targets = new ArrayList();
        Random rand = new Random();
        while (targets.size() < sampleSize) {
            int candidate = rand.nextInt(bound);
            if (!targets.contains(candidate)) {
                targets.add(candidate);
            }
        }
        return targets; //returns ids of the samples
    }

    //TX folder
    public final static String TX_PATH = "PATH\\TO\\TRANSACTIONS";

    static int log(int x, int base) {
        return (int) (Math.log(x) / Math.log(base));
    }

//    public static byte[] objectToByte(Object o) throws IOException {
//        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
//        ObjectOutput oo = new ObjectOutputStream(bStream);
//        oo.writeObject(o);
//        oo.close();
//        return bStream.toByteArray();
//    }
}