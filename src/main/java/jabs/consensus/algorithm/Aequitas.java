package jabs.consensus.algorithm;

import jabs.consensus.blockchain.LocalBlockTree;
import jabs.ledgerdata.*;
import jabs.ledgerdata.pbft.*;
import jabs.network.message.VoteMessage;
import jabs.network.node.nodes.Node;
import jabs.network.node.nodes.pbft.PBFTNode;

import java.util.HashMap;
import java.util.HashSet;

// based on: https://sawtooth.hyperledger.org/docs/pbft/nightly/master/architecture.html
// another good source: http://ug93tad.github.io/pbft/

public class Aequitas<B extends SingleParentBlock<B>, T extends Tx<T>> extends AbstractChainBasedConsensus<B, T>
        implements VotingBasedConsensus<B, T>, DeterministicFinalityConsensus<B, T> {

    private final int numAllParticipants;
    private final HashMap<B, HashMap<Node, Vote>> prepareVotes = new HashMap<>();
    private final HashMap<B, HashMap<Node, Vote>> commitVotes = new HashMap<>();
    private final HashSet<B> preparedBlocks = new HashSet<>();
    private final HashSet<B> committedBlocks = new HashSet<>();
    private int currentViewNumber = 0;

    // TODO: View change should be implemented

    private AequitasMode AequitasModeS = AequitasMode.NORMAL_MODE;
    private AequitasPhase AequitasPhaseS = AequitasPhase.GOSSIP;

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

    public enum AequitasMode {
        NORMAL_MODE,
        VIEW_CHANGE_MODE
    }

    public enum AequitasPhase {
        GOSSIP,
        AGREEMENT,
        FINALIZATION 
    }

    public Aequitas(LocalBlockTree<B> localBlockTree, int numAllParticipants) {
        super(localBlockTree);
        this.numAllParticipants = numAllParticipants;
        this.currentMainChainHead = localBlockTree.getGenesisBlock();
    }

    public void newIncomingVote(Vote vote) {
        if (vote instanceof AequitasBlockVote) { // for the time being, the view change votes are not supported
            AequitasBlockVote<B> blockVote = (AequitasBlockVote<B>) vote;
            B block = blockVote.getBlock();
            switch (blockVote.getVoteType()) {
                case GOSSIP :
                    if (!this.localBlockTree.contains(block)) {
                        this.localBlockTree.add(block);
                    }
                    if (this.localBlockTree.getLocalBlock(block).isConnectedToGenesis) {
                        this.AequitasPhaseS = AequitasPhaseS.GOSSIP;
                        this.peerBlockchainNode.broadcastMessage(
                                new VoteMessage(
                                        new PBFTPrepareVote<>(this.peerBlockchainNode, blockVote.getBlock())
                                )
                        );
                    }
                    break;
                case AGREEMENT:
                    checkVotesAequitas(blockVote, block, prepareVotes, preparedBlocks, AequitasPhaseS.AGREEMENT);
                    break;
                case FINALIZATION:
                    checkVotesAequitas(blockVote, block, commitVotes, committedBlocks, AequitasPhaseS.GOSSIP);
                    break;
            }
        }
    }

    private void checkVotesAequitas(AequitasBlockVote<B> vote, B block, HashMap<B, HashMap<Node, Vote>> votes, HashSet<B> blocks, AequitasPhase nextStep) {
        if (!blocks.contains(block)) {
            if (!votes.containsKey(block)) { // this the first vote received for this block
                votes.put(block, new HashMap<>());
            }
            votes.get(block).put(vote.getVoter(), vote);
            if (votes.get(block).size() > (((numAllParticipants / 3) * 2) + 1)) {
                blocks.add(block);
                this.AequitasPhaseS = nextStep;
                switch (nextStep) {
                    case AGREEMENT:
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
                    case FINALIZATION:
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

    @Override
    public void newIncomingBlock(B block) {

    }

    /**
     * @param block
     * @return
     */
    @Override
    public boolean isBlockConfirmed(B block) {
        return false;
    }

    /**
     * @param block
     * @return
     */
    @Override
    public boolean isBlockValid(B block) {
        return false;
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

    public AequitasPhase getPbftPhase() {
        return this.AequitasPhaseS;
    }

    @Override
    protected void updateChain() {
        this.confirmedBlocks.add(this.currentMainChainHead);
    }
}