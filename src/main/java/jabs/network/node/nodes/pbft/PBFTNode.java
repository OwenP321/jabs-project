package jabs.network.node.nodes.pbft;
import jabs.consensus.blockchain.LocalBlockTree;

import static org.apache.commons.math3.util.FastMath.sqrt;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import jabs.consensus.algorithm.PBFT;
import jabs.consensus.algorithm.PBFT.PBFTPhase;
import jabs.ledgerdata.Block;
import jabs.ledgerdata.BlockFactory;
import jabs.ledgerdata.Recipt;
import jabs.ledgerdata.TransactionFactory;
import jabs.ledgerdata.Vote;
import jabs.ledgerdata.ethereum.EthereumBlock;
import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.ledgerdata.pbft.PBFTBlock;
import jabs.ledgerdata.pbft.PBFTTransactionVote;
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
import jabs.simulator.event.BlockConfirmationEvent;
import jabs.simulator.event.BlockCreationEvent;
import jabs.simulator.event.BlockLeaderCreationEvent;
import jabs.simulator.event.TxGenerationProcessSingleNode;


public class PBFTNode extends PeerBlockchainNode<PBFTBlock, EthereumTx> {
                        public static final PBFTBlock PBFT_GENESIS_BLOCK = new PBFTBlock(0, 0, 0, null, null);

            private ArrayList<EthereumTx> mempool;
            private HashMap<EthereumTx, Node> txToSender;
            static final long MAXIMUM_BLOCK_GAS = 1250;

            private int timeBetweenTxs;
            protected Simulator.ScheduledEvent txGenPro;
            protected Simulator.ScheduledEvent blockGenPro;

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            private static boolean stopFlag = false;

            long blockCreationTimeInterval;

            double blockGenTime = 50;
            
            List<PBFTBlock> commitedBlocks;

            ArrayList<EthereumTx> allTxBlock = new ArrayList<>();
            ArrayList<ArrayList<EthereumTx>> allTxAllBlocks = new ArrayList<>();
            
            
            
            
        public PBFTNode(Simulator simulator, Network network, int nodeID, long downloadBandwidth, long uploadBandwidth, int numAllParticipants, int timeBetweenTxs) {
                super(simulator, network, nodeID, downloadBandwidth, uploadBandwidth,
                new PBFTP2P(),
                new PBFT<>(new LocalBlockTree<>(PBFT_GENESIS_BLOCK), numAllParticipants)
                );
                this.consensusAlgorithm.setNode(this);
                this.mempool = new ArrayList<>();
                this.txToSender = new HashMap<>();
                this.timeBetweenTxs = timeBetweenTxs;
                
                PBFT<PBFTBlock, EthereumTx> pbftInstance = new PBFT<>(localBlockTree, numAllParticipants);
                List<PBFTBlock> commitedBlocks = pbftInstance.getCommitedBlocks();
        
        
    }

    @Override
    protected void processNewTx(EthereumTx tx, Node from) {
        // nothing for now
        this.mempool.add(tx);
        this.txToSender.put(tx, from);
    }

   
    
   
    @Override
    protected void processNewBlock(PBFTBlock block) {
        System.out.println("processNewBlock");

        if (block.getNode().getNodeID() == 0) {
            System.out.println("LEADER BLOCK ENGAGED");
            ArrayList<EthereumTx> txList = new ArrayList<>(block.getTransactions());
            allTxAllBlocks.add(txList);
            //System.out.println(txList);

            
        }

        //this.consensusAlgorithm.newIncomingBlock(block);
        //this.broadcastNewBlockAndBlockHashes(block);

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

    protected void processNewTxVote(PBFTTransactionVote tx)
    {
        ((PBFT<PBFTBlock, EthereumTx>) this.consensusAlgorithm).newIncomingVote(tx);
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
        System.out.println("REMOVE MEMPOOL");
        //System.out.println(block.getTransactions());
        for(EthereumTx tx: block.getTransactions()){
            this.mempool.remove(tx);
            //System.out.println("REMOVED " + tx);
        }
    }
    
    protected void broadcastTransaction(EthereumTx tx) {

        //ALL SEND TO ALL 

        for (Node neighbor:this.p2pConnections.getNeighbors()) {
            
        this.networkInterface.addToUpLinkQueue(
                new Packet(this, neighbor,
                        new DataMessage(tx)
                    )
                );
                //System.out.println(tx + " TO NODE " + neighbor);
            }
            //System.out.println("BROADCAST TRANSACTIONS");
            //addToMempool(tx);
        }


    protected void broadcastBlock(PBFTBlock b) {

        //ALL SEND TO ALL 

        //System.out.println("TIME FOR BLOCK TO BE BROADCASTED");
        for (Node neighbor:this.p2pConnections.getNeighbors()) {
            
        this.networkInterface.addToUpLinkQueue(
                new Packet(this, neighbor,
                        new DataMessage(b)
                    )
                );
                //System.out.println(b + " TO NODE " + neighbor);
            }
            System.out.println("BROADCAST BLOCKS");
        }


    //BROADCAST BLOCK()
    //Check blocks vs blocks 
    //majority 


    //ADD PREPREPAIR TO BLOCK 


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
    public EthereumTx generateNewPBFTTransaction() {
        EthereumTx tx = TransactionFactory.sampleEthereumTransaction(network.getRandom());
        broadcastTransaction(tx);
        
        //System.out.println("Transactions being made in Transaction Factory");
        return getTxs(tx);
        //addToMempool(tx);
        
        
    }
    public EthereumTx getTxs(EthereumTx tx){

        return tx;
    }

    public void startTxGen(){
        TxGenerationProcessSingleNode txGenPro = new TxGenerationProcessSingleNode(this.simulator, this.network.getRandom(), this, timeBetweenTxs, network);
        this.txGenPro = this.simulator.putEvent(txGenPro, txGenPro.timeToNextGeneration());

        //System.out.println("TRANSACTIONS MADE");
    }
    //public void blockEvent(){
    //    BlockCreationEvent blockGenPro = new BlockCreationEvent(simulator, this.network.getRandom(), this, network);
    //    this.blockGenPro = this.simulator.putEvent(blockGenPro, blockGenTime);
    //}

    protected void fillMempool(int numTxs){
        for (int i = 0; i < numTxs; i++){
            EthereumTx tx = TransactionFactory.sampleEthereumTransaction(network.getRandom());
            mempool.add(tx);
        }

        System.out.println("Mempool size: " + this.mempool.size());

    }


    public void createBlockEvent(){
        double timeBlock = 10;
        BlockConfirmationEvent blockGenPro = new BlockConfirmationEvent(timeBlock, this, this.network);
        double timeInSec = 40;
        this.blockGenPro = this.simulator.putEvent(blockGenPro, (double)timeInSec);

    }

    public void createLeadBlockEvent(){
        double timeBlock = 10;
        BlockLeaderCreationEvent blockGenPro = new BlockLeaderCreationEvent(timeBlock, this, this.network);
        double timeInSec = 60;
        this.blockGenPro = this.simulator.putEvent(blockGenPro, timeInSec);

    }
    public PBFTBlock createLeaderBlock(){

        System.out.println("LEADER BLOCK CREATE BLOCK");

        ArrayList<EthereumTx> finorder = new ArrayList<>();

        System.out.println(allTxAllBlocks);

        finorder = this.consensusAlgorithm.validateTransactionsLeader(allTxAllBlocks);
        int size = 1000;

        System.out.println(finorder);


        PBFTBlock block = new PBFTBlock(size, this.consensusAlgorithm.getCanonicalChainHead().getHeight()+ 1, simulator.getSimulationTime(), this, this.consensusAlgorithm.getCanonicalChainHead());

        //block.setTransactions(finorder);

        if(allTxAllBlocks != null){
            System.out.println(allTxAllBlocks.get(0));
            block.setTransactions(allTxAllBlocks.get(0));

        }
        this.consensusAlgorithm.newIncomingBlock(block);

        return null;

    }

    public PBFTBlock createBlock(){
        
        ArrayList<EthereumTx> txs = new ArrayList<EthereumTx>();
        int gas = 0;
        int size = 0;
        int txAmount = 0;

        if(this.mempool.size() ==0){
            System.out.println("Mempool is empty");

        }
        int i = 0;

        while(gas < MAXIMUM_BLOCK_GAS && this.mempool.size() > i){
            EthereumTx tx = this.mempool.get(i);
            txs.add(tx);
            i++;
            //System.out.println("TRANSACTIONS IN THIS BLOCK" + tx);
            txAmount++;
            //System.out.println("STUCK HERE MAYBE");

        }

        size = size + 1000;

        int newSize = BlockFactory.sampleBitcoinBlockSize(this.network.getRandom());
        PBFTBlock block = new PBFTBlock(size, this.consensusAlgorithm.getCanonicalChainHead().getHeight()+ 1, simulator.getSimulationTime(), this, this.consensusAlgorithm.getCanonicalChainHead());
        //System.out.println("_________________" + txs);
        block.setTransactions(txs);
        removeFromMempool(block);

        broadcastBlock(block);
        System.out.println(block);
        System.out.println("THE AMOUNT OF TX: " + txAmount);
        //System.out.println(mempool);

        //this.consensusAlgorithm.newIncomingBlock(block);

        return block;

        //NEED to make sure mempool is empty 

    }

    public void timeLoop(){

        /*
         * 
            Runnable task = new Runnable() {
                
                public void run(){
                    createBlock();
                    if(stopFlag){
                        scheduler.shutdown();
                    }
                }
            };

            scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
         * 
         */

         this.blockCreationTimeInterval = TimeUnit.SECONDS.toNanos(500);
    }

    public void stopTime(){
        stopFlag = true;
    }

    @Override
    public void generateNewTransaction() {
        // TODO Auto-generated method stub
        generateNewPBFTTransaction();
    }

    public void checkBlocks(){

        PBFT<PBFTBlock, EthereumTx> pbftInstance =  (PBFT<PBFTBlock, EthereumTx>) this.consensusAlgorithm;
        List<PBFTBlock> commitedBlocks = pbftInstance.getCommitedBlocks();
          
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%");
        for(PBFTBlock block : commitedBlocks){
            System.out.println("GOT HERE");
            System.out.println(block);
        }

        ArrayList<PBFTBlock> cbs = pbftInstance.getCB();
        System.out.println("^^^^^^^^ FINAL BLOCKS ^^^^^^^^^^^^^ ");
        System.out.println(cbs);

     

        ArrayList<PBFTBlock> pbftFinBlocks = new ArrayList<>();
        pbftFinBlocks = pbftInstance.matchBlocks();

        int totalTxs =0;
        for(int i =0; i < pbftFinBlocks.size(); i++)
        {  
            ArrayList<EthereumTx> tx = pbftFinBlocks.get(i).getTransactions();
            if(tx != null){
                totalTxs = totalTxs + tx.size();
            }
        }
        System.out.println("THE TOTAL TXS IS " + totalTxs);

        System.out.println("TOTAL TXs: " + pbftInstance.getTotalTx());




    }

    

    


}
