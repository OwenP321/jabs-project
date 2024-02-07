package jabs.network.node.nodes.pbft;
import jabs.consensus.blockchain.LocalBlockTree;
import jabs.consensus.algorithm.PBFT;
import jabs.ledgerdata.Vote;
import jabs.ledgerdata.pbft.PBFTBlock;
import jabs.ledgerdata.pbft.PBFTTx;
import jabs.network.networks.Network;
import jabs.network.node.nodes.PeerBlockchainNode;
import jabs.network.node.nodes.Node;
import jabs.network.p2p.PBFTP2P;
import jabs.simulator.Simulator;

public class PBFTNode extends PeerBlockchainNode<PBFTBlock, PBFTTx> {
                        public static final PBFTBlock PBFT_GENESIS_BLOCK =
            new PBFTBlock(0, 0, 0, null, null);

    public PBFTNode(Simulator simulator, Network network, int nodeID, long downloadBandwidth, long uploadBandwidth, int numAllParticipants) {
        super(simulator, network, nodeID, downloadBandwidth, uploadBandwidth,
                new PBFTP2P(),
                new PBFT<>(new LocalBlockTree<>(PBFT_GENESIS_BLOCK), numAllParticipants)
        );
        this.consensusAlgorithm.setNode(this);
    }

    @Override
    protected void processNewTx(PBFTTx tx, Node from) {
        // nothing for now
        if(this.consensusAlgorithm.getPhase() = PBFT.getPhase.REQUEST)
            
        this.broadcastMessage(tx);
        else {
            this.processNewTx(tx, from);
        }
    }

    @Override
    protected void processNewBlock(PBFTBlock block) {
        // nothing for now
        if (this.consensusAlgorithm.getPhase() == PBFT.getPhase.PRE_PREPARE)||
            this.consensusAlgorithm.getPhase() == PBFT.getPhase.PREPARE ||
            this.consensusAlgorithm.getPhase() == PBFT.getPhase.COMMIT {
                this.addToBlockQueue(block)
            } else {
                this.consensusAlgorithm.receiveBlock(block)
            }
    }

    @Override
    protected void processNewVote(Vote vote) {
        ((PBFT<PBFTBlock, PBFTTx>) this.consensusAlgorithm).newIncomingVote(vote);
    }

    @Override
    protected void processNewQuery(jabs.ledgerdata.Query query) {

    }

    @Override
    public void generateNewTransaction() {
        // nothing for now
        PBFTTx newTx = new PBFTTx(16,32);
        this.broadcastMessage(newTx);
    }

    public void addToTxpool(PBFTTx tx){

    }
    public void addToBlockQueue(PBFTBlock block){
        
    }
}
