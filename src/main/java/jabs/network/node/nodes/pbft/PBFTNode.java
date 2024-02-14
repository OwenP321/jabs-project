package jabs.network.node.nodes.pbft;
import jabs.consensus.blockchain.LocalBlockTree;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import jabs.consensus.algorithm.PBFT;
import jabs.consensus.algorithm.PBFT.PBFTPhase;
import jabs.ledgerdata.Recipt;
import jabs.ledgerdata.Vote;
import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.ledgerdata.pbft.PBFTBlock;
import jabs.ledgerdata.pbft.PBFTTx;
import jabs.network.networks.Network;
import jabs.network.node.nodes.PeerBlockchainNode;
import jabs.network.node.nodes.PeerDLTNode;
import jabs.network.node.nodes.Node;
import jabs.network.p2p.PBFTP2P;
import jabs.simulator.Simulator;


public class PBFTNode extends PeerBlockchainNode<PBFTBlock, EthereumTx> {
                        public static final PBFTBlock PBFT_GENESIS_BLOCK = new PBFTBlock(0, 0, 0, null, null);

            private ArrayList<EthereumTx> mempool;
            private HashMap<EthereumTx, Node> txToSender;
            protected ArrayList<Recipt> recipts;


            

    public PBFTNode(Simulator simulator, Network network, int nodeID, long downloadBandwidth, long uploadBandwidth, int numAllParticipants) {
        super(simulator, network, nodeID, downloadBandwidth, uploadBandwidth,
                new PBFTP2P(),
                new PBFT<>(new LocalBlockTree<>(PBFT_GENESIS_BLOCK), numAllParticipants)
        );
        this.consensusAlgorithm.setNode(this);
        this.mempool = new ArrayList<>();
        this.txToSender = new HashMap<>();
    }

    @Override
    protected void processNewTx(EthereumTx tx, Node from) {
        // nothing for now
        this.mempool.add(tx);
        this.txToSender.put(tx, from);
    }

    public void processNewRecipt(Recipt recipt, Node from)
    {
        if(!this.recipts.contains(recipt));
        System.out.println("NODE: "+ this.nodeID + "RECIVED RECIPT " + "FROM NODE " + from.getNodeID());
        this.recipts.add(recipt);
        EthereumTx tx = recipt.getTx();
        EthereumTx newTx = new EthereumTx(tx.getSize(), tx.getGas());
        newTx.setSender(null);
        newTx.setReceiver(tx.getReciver());
        this.mempool.add(0, newTx);
    }

    @Override
    protected void processNewBlock(PBFTBlock block) {
        // nothing for now
        if (this.consensusAlgorithm.getPhase() == PBFT.getPhase.PRE_PREPARE)||
            this.consensusAlgorithm.getPhase() == PBFT.getPhase.PREPARE ||
            this.consensusAlgorithm.getPhase() == PBFT.getPhase.COMMIT {
                this.addToBlockQueue(block)
            } else {
                this.consensusAlgorithm.receiveBlock(block);
            }
    }

    @Override
    protected void processNewVote(Vote vote) {
        ((PBFT<PBFTBlock>) this.consensusAlgorithm).newIncomingVote(vote);
    }

    @Override
    protected void processNewQuery(jabs.ledgerdata.Query query) {

    }

    @Override
    public void generateNewTransaction() {
        // nothing for now
        EthereumTx newTx = new EthereumTx(16,32);
        this.broadcastTransaction(newTx);
    }

    public void addToTxpool(EthereumTx tx){

    }
    public void addToBlockQueue(PBFTBlock block){
        
    }
    private void addToMempool(EthereumTx tx)
    {
        this.mempool.add(tx);
    }
    public void removeFromMempool(EthereumTx tx)
    {
        this.mempool.remove(tx);
    }
    public void broadcastTransaction(EthereumTx tx)
    {
        

    }

}
