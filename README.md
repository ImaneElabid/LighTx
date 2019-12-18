# reliablebroadcast
## Implementation details:

SSL sockets for communication with self-signed certificates.

Java.Util.Random to implement the sampling oracle  Ω.

The value sent by the sender is an Object (String for test)

I implemented the two events `<pb.deliver>` to be called in the setter when the value of the variable `delivered` changed.

Delivery is translated by the value of `pbDelivered` = `true`

`waitTillFinished` method waits for all processes to start, then allows the sender to proceeds with send ad receive operations

`alreadyExecuted` variable is used to indicate that `gotEnoughReady` is executed only once for each different message it receives

Delivery indicator variables should be atomic, in order not to be thread-safe and not deliver many times

The method `byzantineBroadcast` imitate a Byzantine sender, to execute it we replace the `pbBroadcast` by this method
