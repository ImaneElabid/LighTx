## LighTx



**LighTx**, a cost-effective and scalable transaction logging system. The system builds on the top of Probabilistic Byzantine Reliable broadcast primitive to concurrently and securely commit transactions without relying on consensus protocols.

A Proof-of-Bandwidth protocol in combination with a reputation system are included for public version of **LighTx**  to make it robust against Sybil attack.

This is a TCP-based version of **LighTx**.

*ProbabilisticBroadcast*, *ProbabilisticConsistentBroadcast* and *ProbabilisticSecureBroadcast* are the three building blocks of Probabilistic Byzantine Reliable broadcast the underlying mechanism for **LighTx**.

*PoB* is a basic method we use to measure bandwidth fluctuations for adversaries (Sybil nodes.)

ReputationScoreSystem is a scoring system that determines the global reputation score for peers of the network by taking into account the opinions of all other peers with whom they interact.

### Parameters.

To modify the algorithm parameters edit Converter.java. It includes the network size ($nodesNbr$), and the different samples sizes ($E\_SIZE, R\_SIZE, D\_SIZE.. $) and the path to transactions' folder ($TX\_PATH$), the convergence thresholds ($EPSILON, E\_HAT, R\_HAT, D\_HAT..$) and other parameters.

### Transactions file.

Each node have a file of transactions to submit. The application parses the file and broadcasts the transactions concurrently.

The transactions are in the form : < recipient ID : Amount >

The transaction file's name should be named after the transaction's sender ID.

Add transactions folder path to $TX\_PATH$ in *Converter.java*

### Run.

To run the project launch the main function in *Launcher.java* 