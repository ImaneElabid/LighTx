package Tools;

import Actors.BroadcastInterface;
import Actors.Node;
import Actors.ProbabilisticSecureBroadcast;

import java.util.HashMap;

/**
 * @author EMS
 */
public class Router {


    HashMap<Integer, BroadcastInterface> routingTable = new HashMap<Integer, BroadcastInterface>();
    Node node;


    public Router(Node node) {
        this.node = node;
    }

    public synchronized BroadcastInterface selectBrChannel(int label) {
        if (routingTable.containsKey(label)) {
            return routingTable.get(label);

        } else {
            routingTable.put(label, new ProbabilisticSecureBroadcast(this.node, label));
            // TODO double creation of psb of the sender
            return routingTable.get(label);
        }
    }
}
