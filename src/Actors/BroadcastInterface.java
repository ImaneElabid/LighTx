package Actors;

import Protocol.Message;

/**
 * @author EMS
 */
public interface BroadcastInterface {

    int getLabel();

    void init(int p1, int p2);

    void broadcast();

    void deliver(Message msgToDeliver) throws Exception;
}
