package jabs.consensus.algorithm;

import jabs.consensus.blockchain.LocalBlockTree;
import jabs.ledgerdata.*;
import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.ledgerdata.pbft.*;
import jabs.network.message.VoteMessage;
import jabs.network.node.nodes.Node;
import jabs.network.node.nodes.pbft.PBFTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class PBFTTxVote<B extends SingleParentBlock<B>> extends AbstractChainBasedConsensus<B, EthereumTx>
        implements VotingBasedConsensus<B, EthereumTx >, DeterministicFinalityConsensus<B, EthereumTx > {
            private final int numAllParticipants;
            private final HashMap<B, HashMap<Node, Vote>> prepareVotes = new HashMap<>();
            private final HashMap<B, HashMap<Node, Vote>> commitVotes = new HashMap<>();
            private final HashSet<B> preparedBlocks = new HashSet<>();
            private final HashSet<B> committedBlocks = new HashSet<>();
            private int currentViewNumber = 0;
            
            
    public PBFTTxVote(LocalBlockTree<B> localBlockTree, int numAllParticipants) {
        super(localBlockTree);
        this.numAllParticipants = numAllParticipants;
        this.currentMainChainHead = localBlockTree.getGenesisBlock();
    }

    @Override
    public void newIncomingBlock(B block) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'newIncomingBlock'");
    }
    @Override
    public boolean isBlockFinalized(B block) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isBlockFinalized'");
    }
    @Override
    public boolean isTxFinalized(EthereumTx tx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isTxFinalized'");
    }
    @Override
    public int getNumOfFinalizedBlocks() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getNumOfFinalizedBlocks'");
    }
    @Override
    public int getNumOfFinalizedTxs() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getNumOfFinalizedTxs'");
    }
    @Override
    public void newIncomingVote(Vote vote) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'newIncomingVote'");
    }
    @Override
    protected void updateChain() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateChain'");
    }



}