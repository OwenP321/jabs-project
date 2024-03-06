package jabs.ledgerdata.pbft;

import jabs.ledgerdata.Block;
import jabs.ledgerdata.Tx;
import jabs.ledgerdata.Vote;
import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.network.node.nodes.Node;

public class PBFTTransactionVote<T extends Tx<T>> extends Vote{

    private final T transaction;
    Node voter;
    

    public PBFTTransactionVote(int size, Node voter, T transaction){
        super(size, voter);
        this.transaction = transaction;
    }

    
    public T getTransaction(){
        return transaction;
    }

    public Node getVoterNode(){
        return voter;
    }
    
}
