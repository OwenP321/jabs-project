package jabs.consensus.algorithm;

import jabs.consensus.blockchain.LocalBlockTree;
import jabs.ledgerdata.*;
import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.ledgerdata.pbft.*;
import jabs.ledgerdata.pbft.AequitasBlockVote.VoteType;
import jabs.network.message.Message;
import jabs.network.message.VoteMessage;
import jabs.network.node.nodes.Node;
import jabs.network.node.nodes.pbft.PBFTNode;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.math3.util.Pair;

// based on: https://sawtooth.hyperledger.org/docs/pbft/nightly/master/architecture.html
// another good source: http://ug93tad.github.io/pbft/

public class PBFT<B extends SingleParentBlock<B>, T extends Tx<T>> extends AbstractChainBasedConsensus<B, T>
        implements VotingBasedConsensus<B, T >, DeterministicFinalityConsensus<B, T > {
    private final int numAllParticipants;
    private final HashMap<B, HashMap<Node, Vote>> prepareVotes = new HashMap<>();
    private final HashMap<B, HashMap<Node, Vote>> commitVotes = new HashMap<>();
    private final HashSet<B> preparedBlocks = new HashSet<>();
    private final HashSet<B> committedBlocks = new HashSet<>();
    private final HashSet<PBFTBlock> comBlock = new HashSet<>();
    private int currentViewNumber = 0;

    private ArrayList<B> addedBlocks =  new ArrayList<>();
    private ArrayList<PBFTBlock> allBlocksFromNodes = new ArrayList<>();

    // TODO: View change should be implemented

    private PBFTMode pbftMode = PBFTMode.NORMAL_MODE;
    private PBFTPhase pbftPhase = PBFTPhase.PRE_PREPARING;

    ArrayList<EthereumTx> txOrder = new ArrayList<EthereumTx>();
    ArrayList<EthereumTx> finalOrder = new ArrayList<EthereumTx>();

    int blockCount =0;
    private PBFTBlock proposedBlock;

    @Override
    public boolean isBlockFinalized(B block) {
        return committedBlocks.contains(block);
    }

    @Override
    public boolean isTxFinalized(T tx) {
        for(B block : committedBlocks){
        }
        return false;
    }

    @Override
    public int getNumOfFinalizedBlocks() {
        return 0;
    }

    @Override
    public int getNumOfFinalizedTxs() {
        return 0;
    }

    public enum PBFTMode {
        NORMAL_MODE,
        VIEW_CHANGE_MODE
    }

    public enum PBFTPhase {
        PRE_PREPARING,
        PREPARING,
        COMMITTING
    }

    public enum VoteType {
        PRE_PREPARE,
        PREPARE,
        COMMIT
    }

    public PBFT(LocalBlockTree<B> localBlockTree, int numAllParticipants) {
        super(localBlockTree);
        this.numAllParticipants = numAllParticipants;
        this.currentMainChainHead = localBlockTree.getGenesisBlock();
    }

    public void newIncomingVote(Vote vote) {

        if(vote instanceof PBFTTransactionVote){
            PBFTTransactionVote txVote = (PBFTTransactionVote) vote;
            Tx transaction = txVote.getTransaction();
            Node voter = txVote.getVoterNode();


            System.out.println("***CONSENSUS "+transaction + " FROM " + voter);
            //System.out.print("****************************************************");
            //System.out.print("WE MADE IT HERE ");
            //System.out.print("****************************************************");

            switch (pbftPhase) {
                case PRE_PREPARING:
                    break;
                case PREPARING:
                    break;
                case COMMITTING:
                    break;
            }

            }

            //After Tx votes 
            //CreateBlock 

        //else if (vote instanceof PBFTPrePrepareVote) { // for the time being, the view change votes are not supported
          //  PBFTPrePrepareVote<B> blockVote = (PBFTPrePrepareVote<B>) vote;
            //B block = blockVote.getBlock();
        
        else if (vote instanceof PBFTBlockVote) { // for the time being, the view change votes are not supported
            PBFTBlockVote<B> blockVote = (PBFTBlockVote<B>) vote;
            B block = blockVote.getBlock();

            //System.out.print("**********************");
            //System.out.print("WE MADE IT HERE BLOCK");
            //System.out.print("**********************");

            //System.out.println("THE BLOCK IS " + block + " VOTE IS " + blockVote.getVoteType());
            

            switch (blockVote.getVoteType()) {
                case PRE_PREPARE :
                    if (!this.localBlockTree.contains(block)) {
                        this.localBlockTree.add(block);
                        //System.out.println("HERE 1");
                    }
                    if (this.localBlockTree.getLocalBlock(block).isConnectedToGenesis) {
                        this.pbftPhase = PBFTPhase.PREPARING;
                        //System.out.println("HERE 2");    
                        this.peerBlockchainNode.broadcastMessage(
                                new VoteMessage(
                                        new PBFTPrepareVote<>(this.peerBlockchainNode, blockVote.getBlock())
                                        )
                                    );
                                        //System.out.println("PRE_PREPARE");
                                        
                    }
                    break;
                case PREPARE:
                    //System.out.println("HERE 3");
                    checkVotes(blockVote, block, prepareVotes, preparedBlocks, PBFTPhase.COMMITTING);
                    //System.out.println("PRRPARE");
                    break;
                case COMMIT:
                    //checkVotes(blockVote, block, commitVotes, committedBlocks, PBFTPhase.PRE_PREPARING);
                    //System.out.println("COMMIT");
                    checkVotesBlock(blockVote, block, commitVotes, comBlock, pbftPhase);
                    break;
            }
        }
    }

    private void checkVotes(PBFTBlockVote<B> vote, B block, HashMap<B, HashMap<Node, Vote>> votes, HashSet<B> blocks, PBFTPhase nextStep) {

        //System.out.println("GETS HERE IN CHECK VOTE");

        if (!blocks.contains(block)) {
            if (!votes.containsKey(block)) { // this the first vote received for this block
                votes.put(block, new HashMap<>());
            }
            votes.get(block).put(vote.getVoter(), vote);
            if (votes.get(block).size() > (((numAllParticipants / 3) * 2) + 1)) {
                blocks.add(block);
                this.pbftPhase = nextStep;
                switch (nextStep) {
                    case PRE_PREPARING:
                        this.currentViewNumber += 1;
                        this.currentMainChainHead = block;
                        updateChain();
                        if (this.peerBlockchainNode.nodeID == this.getCurrentPrimaryNumber()){
                            this.peerBlockchainNode.broadcastMessage(
                                    new VoteMessage(
                                            new PBFTPrePrepareVote<>(this.peerBlockchainNode, block
                                            )
                                    )
                            );
                        }
                        break;
                    case COMMITTING:
                        this.peerBlockchainNode.broadcastMessage(
                                new VoteMessage(
                                        new PBFTCommitVote<>(this.peerBlockchainNode, block)
                                )
                        );
                        //txOrder.clear();
                        //finalOrder.clear();
                        break;
                }
            }
        }
    }

    private void checkVotesBlock(PBFTBlockVote<B> vote, PBFTBlock block, HashMap<B, HashMap<Node, Vote>> votes, HashSet<B> blocks, PBFTPhase nextStep) {
        if (!blocks.contains(block)) {
            if (!votes.containsKey(block)) {
                votes.put(block, new HashMap<>());
            }
            votes.get(block).put(vote.getVoter(), vote);
            if (votes.get(block).size() > (((numAllParticipants / 3) * 2) + 1)) {
                blocks.add(block);
                this.pbftPhase = nextStep;
    
                //Get the block from the leader
                if(block.getCreator().getNodeID() == 0){
                    System.out.println("LEAD NODE" + block);
                    // Check if the block is valid
                    if (isBlockValid(block)) {
                        // Finalize the block and add it to the blockchain
                        this.localBlockTree.add(block);
                        this.currentMainChainHead = block;
    
                        //System.out.println("£££££££££££");
                        System.out.println("IS BLOCK VALID");
                        System.out.print(block);
        
                        // Update the chain and broadcast the commit vote
                        updateChain();
                        addedBlocks.add(block);
                        this.peerBlockchainNode.broadcastMessage(new VoteMessage(new PBFTCommitVote<>(this.peerBlockchainNode, block)));

                        PBFTBlock finBlock = (PBFTBlock) block;
                        writeBlockToCSV("output/Test.csv", finBlock);
                        //System.out.println("******************************************");
                        //System.out.println(this.committedBlocks);
                    }
                }
            }
        }
    }


    public PBFTTransactionVote newIncomingTx(EthereumTx tx){
        PBFTTransactionVote txVote = new PBFTTransactionVote(10, this.peerBlockchainNode, tx);

        this.peerBlockchainNode.broadcastMessage(new VoteMessage(txVote));

        return txVote;
    }
    
    @Override
    
    public void newIncomingBlock(B block) {
        if(block instanceof PBFTBlock){
            PBFTBlock pbftBlock = (PBFTBlock) block;
            //PBFTBlockVote pbftVoteBlock = new PBFTBlockVote<>(10,peerBlockchainNode,pbftBlock, VoteType.PRE_PREPARE) 
            
            if(!isBlockConfirmed(block) && !isBlockFinalized(block)){
                
                /* 
                for (EthereumTx tx : pbftBlock.getTransactions()) {
                    System.out.println(peerBlockchainNode + "***********************************");
                    System.out.println(currentMainChainHead + "-----------------");
                    PBFTTransactionVote<EthereumTx> txVote =  new PBFTTransactionVote<>(10, peerBlockchainNode, tx);
                    
                    this.peerBlockchainNode.broadcastMessage(new VoteMessage(txVote));
                }*/
                
                
                
                
                //PBFTPrePrepareVote<PBFTBlock> vote = new PBFTPrePrepareVote<PBFTBlock>(peerDLTNode, pbftBlock);
                //this.peerBlockchainNode.broadcastMessage(new VoteMessage(vote));
                
                Boolean validBlock = validateTransactions(pbftBlock);
                if(validBlock == true){
                    this.peerBlockchainNode.broadcastMessage(
                                    new VoteMessage(
                                            new PBFTPrePrepareVote<>(this.peerBlockchainNode, pbftBlock.getBlock())
                                            )
                                        );
                }

                if (isLeaderNode()) {
                    // Propose the block to the network
                    this.proposedBlock = pbftBlock;
                } else {
                    // Compare the received block with the proposed block
                    compareBlocks(pbftBlock);
                }


                
            }
            
        }
    }

    private void compareBlocks(PBFTBlock receivedBlock) {
        if (proposedBlock != null) {
            // Validate transactions and verify if the received block matches the proposed block
            if (validateTransactions(receivedBlock) && receivedBlock.equals(proposedBlock)) {
                // Node votes to commit the received block
                //PBFTBlockVote<B> vote = new PBFTBlockVote<>(this.peerBlockchainNode, receivedBlock, VoteType.COMMIT);
                //PBFTBlockVote<B> vote = new PBFTPrePrepareVote<>(this.peerBlockchainNode, receivedBlock, VoteType.COMMIT);
                //this.peerBlockchainNode.broadcastMessage(new VoteMessage(vote));

                Boolean validBlock = validateTransactions(receivedBlock);
                if(validBlock == true){
                    this.peerBlockchainNode.broadcastMessage(
                                    new VoteMessage(
                                            new PBFTPrePrepareVote<>(this.peerBlockchainNode, receivedBlock.getBlock())
                                            )
                                        );
                }

            }
        }
    }

    private boolean isLeaderNode() {
        // Determine if this node is the leader node based on some criteria
        // For example, using a round-robin selection method
        return this.peerBlockchainNode.getNodeID() == getCurrentPrimaryNumber();
    }

/*
public void newIncomingBlock(B block) {
    if (block instanceof PBFTBlock) {
        PBFTBlock pbftBlock = (PBFTBlock) block;

        // Store the received block
        allBlocksFromNodes.add(pbftBlock);

        // Check if all expected blocks for the round have been received
        if (addedBlocks.size() == numAllParticipants) {
            // Select the main block for this round
            PBFTBlock mainBlock = selectMainBlock(allBlocksFromNodes);

            Block mainBlockFin = (Block) mainBlock;

            // Add the selected main block to the chain
            if (!isBlockConfirmed(mainBlockFin) && !isBlockFinalized(mainBlockFin)) {
                // Validate the main block before adding to the chain
                if (validateBlock(mainBlock)) {
                    localBlockTree.add(mainBlock);
                    currentMainChainHead = mainBlock;
                    updateChain();
                }
            }

            // Clear the list for the next round
            allBlocksFromNodes.clear();
        }
    }
}*/

/*
private PBFTBlock selectMainBlock(List<PBFTBlock> blocks) {
    //Get the leader node 

    for (PBFTBlock block : blocks) {
        if (block.getNode().getNodeID() == 0) {
            return block;
        }
    }
    // If no block is proposed by the leader node, return null or handle the case as needed
    return null;
}*/

private boolean validateTransactions(PBFTBlock block) {
        ArrayList<EthereumTx> txOrderVal = block.getTransactions();
    
    // Counters to track the number of votes for each transaction and pair ordering
    HashMap<EthereumTx, Integer> txVotesCount = new HashMap<>();
    HashMap<Pair<EthereumTx, EthereumTx>, Integer> pairOrderVotesCount = new HashMap<>();
    
    // Iterate through transactions to count votes
    for (int i = 0; i < txOrderVal.size(); i++) {
        EthereumTx tx = txOrderVal.get(i);
        txVotesCount.put(tx, txVotesCount.getOrDefault(tx, 0) + 1);

        // Check relative order with other transactions
        for (int j = i + 1; j < txOrderVal.size(); j++) {
            EthereumTx nextTx = txOrderVal.get(j);
            Pair<EthereumTx, EthereumTx> pair = new Pair<>(tx, nextTx);

            // If the current tx should come before nextTx, increase its pair order votes
            if (txVotesCount.getOrDefault(tx, 0) > txVotesCount.getOrDefault(nextTx, 0)) {
                pairOrderVotesCount.put(pair, pairOrderVotesCount.getOrDefault(pair, 0) + 1);
            } else {
                // If nextTx should come before the current tx, increase its pair order votes
                pairOrderVotesCount.put(pair, pairOrderVotesCount.getOrDefault(pair, 0) - 1);
            }
        }
    }

    // Check if each transaction has enough votes for inclusion
    ArrayList<EthereumTx> finalOrder = new ArrayList<>();
    for (EthereumTx tx : txVotesCount.keySet()) {
        if (txVotesCount.get(tx) >= (2 * numAllParticipants / 3 + 1)) {
            finalOrder.add(tx);
        }
    }

    // Reorder transactions based on the super-majority agreement on pair order
    for (int i = 0; i < finalOrder.size(); i++) {
        for (int j = i + 1; j < finalOrder.size(); j++) {
            EthereumTx tx1 = finalOrder.get(i);
            EthereumTx tx2 = finalOrder.get(j);
            Pair<EthereumTx, EthereumTx> pair = new Pair<>(tx1, tx2);
            
            // If more than half of the nodes agree on the relative order, swap tx1 and tx2
            if (pairOrderVotesCount.getOrDefault(pair, 0) > 0) {
                finalOrder.set(i, tx2);
                finalOrder.set(j, tx1);
            }
        }
    }

    // Check if the block contains valid transactions and ordering
    boolean validBlock = blockValid(block, finalOrder);
    return validBlock;
}

private boolean blockValid(PBFTBlock block, ArrayList<EthereumTx> finalOrder) {
    // Check if the finalOrder matches the block's transactions
    if (finalOrder.equals(block.getTransactions())) {
        return true;
    }
    return false;
}

/*
public void writeFinalBlocksToCSV(String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.append("Block Height,Block Hash,Transactions\n");

            
            // Iterate through finalized blocks
            for (B block : confirmedBlocks) {
                if(block instanceof PBFTBlock){
                    PBFTBlock pbftBlock = (PBFTBlock) block;
                // Write block details
                writer.append(String.valueOf(block.getHeight())).append(",");
                writer.append((CharSequence) block.getHash()).append(",");
                ArrayList<EthereumTx> transactions = ((PBFTBlock) block).getTransactions();
                StringBuilder transactionString = new StringBuilder();
                for (EthereumTx tx : transactions) {
                    transactionString.append(tx.toString()).append(";");
                }
                // Remove the last semicolon
                if (transactionString.length() > 0) {
                    transactionString.setLength(transactionString.length() - 1);
                }
                writer.append(transactionString.toString()).append("\n");
            }}
            System.out.println("CSV file written successfully at " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }
    */
    public ArrayList<PBFTBlock> getCommitedBlocks(){
        ArrayList<PBFTBlock> commitedBlocksList = new ArrayList<PBFTBlock>(comBlock);
        return commitedBlocksList;
    }

    public ArrayList<B> getCB(){
        return addedBlocks;
    }

    /*
     * 
     * 
     Gets all the final blocks
    public void writeFinalBlocksToCSV(String filePath) { 

        try (FileWriter writer = new FileWriter(filePath)) { 
    
            // Write header 
    
            writer.append("Block Height,Block Hash,Transactions,Votes\n"); 
            // Iterate through confirmed blocks 
    
            for (B block : confirmedBlocks) { 
    
                if (block instanceof PBFTBlock) { 
                    PBFTBlock pbftBlock = (PBFTBlock) block; 
                    // Write block details 
                    writer.append(String.valueOf(block.getHeight())).append(","); 
                    writer.append(String.valueOf(block.getHash())).append(","); 
                    // Get transactions and count votes 
                    //ArrayList<EthereumTx> transactions = pbftBlock.getTransactions();
                    //System.out.println(transactions); 
                    int numVotes = getNumVotesForBlock(block); 

                    // Write transactions and votes count
                    StringBuilder transactionString = new StringBuilder(); 
                    //for (EthereumTx tx : transactions) { 
                    //    transactionString.append(tx.toString()).append(";"); 
                    //}
    
                    //if (transactionString.length() > 0) { 
                    //    transactionString.setLength(transactionString.length() - 1); 
                    //} 
    
                    //writer.append(transactionString.toString()).append(","); 
                    writer.append(String.valueOf(numVotes)).append("\n"); 
                } 
    
            } 
    
            //System.out.println("CSV file written successfully at " + filePath); 
    
        } catch (IOException e) { 
    
            System.err.println("Error writing CSV file: " + e.getMessage()); 
    
        } 
    
    } 
    */

    public void writeBlockToCSV(String filePath, PBFTBlock block) {

        try (FileWriter writer = new FileWriter(filePath)) {
    
            // Write header
            writer.append("Block Height,Block Hash,Transactions,Votes\n");
    
            // Write block details
            writer.append(String.valueOf(block.getHeight())).append(",");
            writer.append(String.valueOf(block.getHash())).append(",");
            // Get transactions and count votes
            ArrayList<EthereumTx> transactions = block.getTransactions();
            int numTxs = 0;
            if(transactions != null)
            {
                numTxs = transactions.size();
            } 
            //System.out.println(transactions);
            int numVotes = getNumVotesForBlock(block);
    
            // Write transactions and votes count
            StringBuilder transactionString = new StringBuilder();
            //for (EthereumTx tx : transactions) {
            //    transactionString.append(tx.toString()).append(";");
            //}
    
            //if (transactionString.length() > 0) {
            //    transactionString.setLength(transactionString.length() - 1);
            //}
    
            writer.append(String.valueOf(numTxs)).append(",");
            writer.append(String.valueOf(numVotes)).append("\n");
    
            //System.out.println("CSV file written successfully at " + filePath);
    
        } catch (IOException e) {
    
            System.err.println("Error writing CSV file: " + e.getMessage());
    
        }
    
    }
      
    
    private int getNumVotesForBlock(PBFTBlock block) { 
    
        int numVotes = 0; 
        HashMap<B, HashMap<Node, Vote>> votes = null; 
        if (committedBlocks.contains(block)) { 
            votes = commitVotes;
    
        } else if (preparedBlocks.contains(block)) { 
            votes = prepareVotes;     
        } 

        if (votes != null && votes.containsKey(block)) { 
            numVotes = votes.get(block).size(); 
        } 
        return numVotes; 
    } 




    /*
     * 
     private boolean validateTransactions(PBFTBlock block){
         
         //The first block will decive upon the transactions
         System.out.println("_______HERE______");
         System.out.println(blockCount);
         blockCount = blockCount + 1;
         System.out.println(blockCount);
 
         if(blockCount == 2){
             txOrder.clear();
             finalOrder.clear();
             System.out.println("***********CLEAR ARRAYS**********************");
             blockCount =0;
         }
         
         ArrayList<EthereumTx> txOrderVal = new ArrayList<>();
         txOrderVal = block.getTransactions();
 
         if(txOrder.isEmpty())
         {
             txOrder = block.getTransactions();
         }
 
         for(int i=0; i< txOrder.size(); i++)
         {
             if(txOrder.get(i) == txOrderVal.get(i)) //if the tx is the same 
             {
                 finalOrder.add(txOrder.get(i));
             }
         }
         System.out.println(finalOrder);
         Boolean validBlock = blockValid(block);
         return validBlock;
     }
 
     private boolean blockValid(PBFTBlock block){
 
         if (finalOrder.equals(block.getTransactions())) {
             return true;
         };
         return false;
     }
     */

    /*
     * 
     public void newIncomingVoteTx(Data tx){
 
 
         if(tx instanceof PBFTTx){
             txB = this.newIncomingTx(tx);
             PBFTTransactionVote<T> txVote = (PBFTTransactionVote<T>) txB;
             T transaction = txVote.getTransaction();
             System.out.print("****************************************************");
             System.out.print("WE MADE IT HERE ");
             System.out.print("****************************************************");
 
             switch (pbftPhase) {
                 case PRE_PREPARING:
                   
                     break;
                 case PREPARING:
                     
                     break;
                 case COMMITTING:
                     
                     break;
             }
 
             }
 
 
     }
     * 
     */

    /**
     * @param block
     * @return
     */
    @Override
    public boolean isBlockConfirmed(B block) {
        return confirmedBlocks.contains(block);
    }

    /**
     * @param block
     * @return
     */
    @Override
    public boolean isBlockValid(B block) {
        return true;
    }

    public int getCurrentViewNumber() {
        return this.currentViewNumber;
    }


    public int getCurrentPrimaryNumber() {
        return (this.currentViewNumber % this.numAllParticipants);
    }

    public int getNumAllParticipants() {
        return this.numAllParticipants;
    }

    public PBFTPhase getPbftPhase() {
        return this.pbftPhase;
    
    }

    @Override
    protected void updateChain() {
        this.confirmedBlocks.add(this.currentMainChainHead);
        this.comBlock.add(this.currentMainChainHead);
        //writeFinalBlocksToCSV("output/finalBlocks.csv");
        
    }

}
