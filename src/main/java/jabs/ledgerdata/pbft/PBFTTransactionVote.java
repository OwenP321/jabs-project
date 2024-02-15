package jabs.ledgerdata.pbft;

import jabs.ledgerdata.Block;
import jabs.ledgerdata.Vote;
import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.network.node.nodes.Node;

public class PBFTTransactionVote  extends  Vote{

    protected PBFTTransactionVote(int size, Node voter) {
        super(size, voter);
        //TODO Auto-generated constructor stub
    }

    
    
}
