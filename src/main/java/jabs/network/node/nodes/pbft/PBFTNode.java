package jabs.network.node.nodes.pbft;
import jabs.consensus.blockchain.LocalBlockTree;

import static org.apache.commons.math3.util.FastMath.sqrt;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import jabs.consensus.algorithm.PBFT;
import jabs.consensus.algorithm.PBFT.PBFTPhase;
import jabs.ledgerdata.BlockFactory;
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
import jabs.simulator.event.TxGenerationProcessSingleNode;


public class PBFTNode extends PeerBlockchainNode<PBFTBlock, EthereumTx> {
                        public static final PBFTBlock PBFT_GENESIS_BLOCK = new PBFTBlock(0, 0, 0, null, null);

            private ArrayList<EthereumTx> mempool;
            private HashMap<EthereumTx, Node> txToSender;
            static final long MAXIMUM_BLOCK_GAS = 1250;

            private int timeBetweenTxs;
            protected Simulator.ScheduledEvent txGenPro;
           


            

    public PBFTNode(Simulator simulator, Network network, int nodeID, long downloadBandwidth, long uploadBandwidth, int numAllParticipants, int timeBetweenTxs) {
        super(simulator, network, nodeID, downloadBandwidth, uploadBandwidth,
                new PBFTP2P(),
                new PBFT<>(new LocalBlockTree<>(PBFT_GENESIS_BLOCK), numAllParticipants)
        );
        this.consensusAlgorithm.setNode(this);
        this.mempool = new ArrayList<>();
        this.txToSender = new HashMap<>();
        this.timeBetweenTxs = timeBetweenTxs;
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

        Set<EthereumTx> blockTxs = new HashSet<>();
        long totalGas = 0;
        for (EthereumTx ethereumTx:mempool) {
            if ((totalGas + ethereumTx.getGas()) > MAXIMUM_BLOCK_GAS) {
                break;
            }
            blockTxs.add(ethereumTx);
            totalGas += ethereumTx.getGas();
        }
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
        ((PBFT<PBFTBlock, EthereumTx>) this.consensusAlgorithm).newIncomingVote(vote);
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
    public void removeFromMempool(PBFTBlock block)
    {
        for(EthereumTx tx: block.getTransactions()){
            this.mempool.remove(tx);
        }
    }
    
    protected void broadcastTransaction(EthereumTx tx) {
        for (Node neighbor:this.p2pConnections.getNeighbors()) {
            
        this.networkInterface.addToUpLinkQueue(
                new Packet(this, neighbor,
                        new DataMessage(tx)
                    )
                );
                System.out.println(tx + " TO NODE " + neighbor);
            }
            System.out.println("BROADCAST");
        }

    /*
    protected void broadcastTransaction(EthereumTx tx) {
        broadcastTransaction(tx, null);
        System.out.println("Broadcast Transactions");
    }
     *
     * 
     *     protected void broadcastTransaction(EthereumTx tx, Node excludeNeighbor) {
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
     */

    @Override
    public void generateNewTransaction() {
        broadcastTransaction(TransactionFactory.sampleEthereumTransaction(network.getRandom()));
        System.out.println("Transactions being made in Transaction Factory" + TransactionFactory.sampleEthereumTransaction(network.getRandom()));
    }

    public void startTxGen(){
        TxGenerationProcessSingleNode txGenPro = new TxGenerationProcessSingleNode(this.simulator, this.network.getRandom(), this, timeBetweenTxs);
        this.txGenPro = this.simulator.putEvent(txGenPro, txGenPro.timeToNextGeneration());
        System.out.println("TRANSACTIONS MADE");
    }

    protected void fillMempool(int numTxs){
        for (int i = 0; i < numTxs; i++){
            EthereumTx tx = TransactionFactory.sampleEthereumTransaction(network.getRandom());
            mempool.add(tx);
        }

        System.out.println("Mempool size: " + this.mempool.size());

    }

    public PBFTBlock createBlock(){
        
        ArrayList<EthereumTx> txs = new ArrayList<EthereumTx>();
        int gas = 0;
        int size = 0;

        if(this.mempool.size() ==0){
            System.out.println("Mempool is empty");

        }
        int i = 0;

        while(gas < MAXIMUM_BLOCK_GAS && this.mempool.size() > i){
            EthereumTx tx = this.mempool.get(i);

        }

        size = size + 1000;

        int newSize = BlockFactory.sampleBitcoinBlockSize(this.network.getRandom());
        PBFTBlock block = new PBFTBlock(size, this.consensusAlgorithm.getCanonicalChainHead().getHeight()+ 1, simulator.getSimulationTime(), this, this.consensusAlgorithm.getCanonicalChainHead());
        block.setTransactions(txs);
        removeFromMempool(block);

        return block;
    }

}
