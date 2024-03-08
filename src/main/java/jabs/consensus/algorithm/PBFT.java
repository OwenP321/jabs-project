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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

// based on: https://sawtooth.hyperledger.org/docs/pbft/nightly/master/architecture.html
// another good source: http://ug93tad.github.io/pbft/

public class PBFT<B extends SingleParentBlock<B>, T extends Tx<T>> extends AbstractChainBasedConsensus<B, T>
        implements VotingBasedConsensus<B, T >, DeterministicFinalityConsensus<B, T > {
    private final int numAllParticipants;
    private final HashMap<B, HashMap<Node, Vote>> prepareVotes = new HashMap<>();
    private final HashMap<B, HashMap<Node, Vote>> commitVotes = new HashMap<>();
    private final HashSet<B> preparedBlocks = new HashSet<>();
    private final HashSet<B> committedBlocks = new HashSet<>();
    private int currentViewNumber = 0;

    // TODO: View change should be implemented

    private PBFTMode pbftMode = PBFTMode.NORMAL_MODE;
    private PBFTPhase pbftPhase = PBFTPhase.PRE_PREPARING;

    ArrayList<EthereumTx> txOrder = new ArrayList<EthereumTx>();
    ArrayList<EthereumTx> finalOrder = new ArrayList<EthereumTx>();

    @Override
    public boolean isBlockFinalized(B block) {
        return false;
    }

    @Override
    public boolean isTxFinalized(T tx) {
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

        else if (vote instanceof PBFTBlockVote) { // for the time being, the view change votes are not supported
            PBFTBlockVote<B> blockVote = (PBFTBlockVote<B>) vote;
            B block = blockVote.getBlock();

            //System.out.print("**********************");
            //System.out.print("WE MADE IT HERE BLOCK");
            //System.out.print("**********************");

            //System.out.println("THE BLOCK IS " + block);

            

            switch (blockVote.getVoteType()) {
                case PRE_PREPARE :
                    if (!this.localBlockTree.contains(block)) {
                        this.localBlockTree.add(block);
                    }
                    if (this.localBlockTree.getLocalBlock(block).isConnectedToGenesis) {
                        this.pbftPhase = PBFTPhase.PREPARING;
                        this.peerBlockchainNode.broadcastMessage(
                                new VoteMessage(
                                        new PBFTPrepareVote<>(this.peerBlockchainNode, blockVote.getBlock())
                                        )
                                    );
                                        //System.out.println("PRE_PREPARE");
                                        
                    }
                    break;
                case PREPARE:
                    checkVotes(blockVote, block, prepareVotes, preparedBlocks, PBFTPhase.COMMITTING);
                    //System.out.println("PRRPARE");
                    break;
                case COMMIT:
                    checkVotes(blockVote, block, commitVotes, committedBlocks, PBFTPhase.PRE_PREPARING);
                    //System.out.println("COMMIT");
                    break;
            }
        }
    }

    private void checkVotes(PBFTBlockVote<B> vote, B block, HashMap<B, HashMap<Node, Vote>> votes, HashSet<B> blocks, PBFTPhase nextStep) {
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
                                            new PBFTPrePrepareVote<>(this.peerBlockchainNode,
                                                    BlockFactory.samplePBFTBlock(peerBlockchainNode.getSimulator(),
                                                            peerBlockchainNode.getNetwork().getRandom(),
                                                            (PBFTNode) this.peerBlockchainNode, (PBFTBlock) block)
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
                        break;
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
            
            if(!isBlockConfirmed(block) && !isBlockFinalized(block)){
                
                /*
                    for (EthereumTx tx : pbftBlock.getTransactions()) {
                        System.out.println(peerBlockchainNode + "***********************************");
                        System.out.println(currentMainChainHead + "-----------------");
                        PBFTTransactionVote<EthereumTx> txVote =  new PBFTTransactionVote<>(10, peerBlockchainNode, tx);
                        
                        this.peerBlockchainNode.broadcastMessage(new VoteMessage(txVote));
                    }
                 * 
                 */
                

                 PBFTPrePrepareVote<PBFTBlock> vote = new PBFTPrePrepareVote<PBFTBlock>(peerDLTNode, pbftBlock);
                 this.peerBlockchainNode.broadcastMessage(new VoteMessage(vote));
            }
            
        }
    }

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
    }

}
