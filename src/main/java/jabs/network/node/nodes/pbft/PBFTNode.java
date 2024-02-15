package jabs.network.node.nodes.pbft;
import jabs.consensus.blockchain.LocalBlockTree;

import static org.apache.commons.math3.util.FastMath.sqrt;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import jabs.consensus.algorithm.PBFT;
import jabs.consensus.algorithm.PBFT.PBFTPhase;
import jabs.ledgerdata.Recipt;
import jabs.ledgerdata.TransactionFactory;
import jabs.ledgerdata.Vote;
import jabs.ledgerdata.ethereum.EthereumBlock;
import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.ledgerdata.pbft.PBFTBlock;
import jabs.ledgerdata.pbft.PBFTTx;
import jabs.network.message.DataMessage;
import jabs.network.message.InvMessage;
import jabs.network.message.Packet;
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

   

   
    @Override
    protected void processNewBlock(PBFTBlock block) {
        this.consensusAlgorithm.newIncomingBlock(block);
        this.broadcastNewBlockAndBlockHashes(block);
    }

    protected void broadcastNewBlockAndBlockHashes(PBFTBlock block){
        for (int i = 0; i < this.p2pConnections.getNeighbors().size(); i++) {
            Node neighbor = this.p2pConnections.getNeighbors().get(i);
            if (i < sqrt(this.p2pConnections.getNeighbors().size())){
                this.networkInterface.addToUpLinkQueue(
                        new Packet(this, neighbor,
                                new DataMessage(block)
                        )
                );
            } else {
                this.networkInterface.addToUpLinkQueue(
                        new Packet(this, neighbor,
                                new InvMessage(block.getHash().getSize(), block.getHash())
                        )
                );
            }
        }
    }

    @Override
    protected void processNewVote(Vote vote) {
        ((PBFT<PBFTBlock>) this.consensusAlgorithm).newIncomingVote(vote);
    }

    @Override
    protected void processNewQuery(jabs.ledgerdata.Query query) {

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
    
    protected void broadcastTransaction(EthereumTx tx, Node excludeNeighbor) {
        for (Node neighbor:this.p2pConnections.getNeighbors()) {
            if (neighbor != excludeNeighbor){
                this.networkInterface.addToUpLinkQueue(
                        new Packet(this, neighbor,
                                new DataMessage(tx)
                        )
                );
            }
        }
    }

    protected void broadcastTransaction(EthereumTx tx) {
        broadcastTransaction(tx, null);
    }

    @Override
    public void generateNewTransaction() {
        broadcastTransaction(TransactionFactory.sampleEthereumTransaction(network.getRandom()));
    }

}
