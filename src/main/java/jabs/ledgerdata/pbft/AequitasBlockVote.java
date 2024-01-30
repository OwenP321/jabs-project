package jabs.ledgerdata.pbft;

import jabs.ledgerdata.Block;
import jabs.ledgerdata.Vote;
import jabs.network.node.nodes.Node;

public abstract class AequitasBlockVote<B extends Block<B>> extends Vote {
    private final B block;
    private final VoteType voteType;

    public static final int PBFT_VOTE_SIZE_OVERHEAD = 10;

    public enum VoteType {
        GOSSIP,
        AGREEMENT,
        COMMIT
    }

    protected AequitasBlockVote(int size, Node voter, B block, VoteType voteType) {
        super(size, voter);
        this.block = block;
        this.voteType = voteType;
    }

    public VoteType getVoteType() {
        return this.voteType;
    }
    public B getBlock() {
        return this.block;
    }
}
