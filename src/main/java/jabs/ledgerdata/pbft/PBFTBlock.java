package jabs.ledgerdata.pbft;

import java.util.ArrayList;

import jabs.ledgerdata.SingleParentBlock;
import jabs.network.node.nodes.Node;
import jabs.ledgerdata.ethereum.EthereumTx;

public class PBFTBlock extends SingleParentBlock<PBFTBlock> {
    public static final int PBFT_BLOCK_HASH_SIZE = 32;

    protected ArrayList<EthereumTx> transactions;
    

    public PBFTBlock(int size, int height, double creationTime, Node creator, PBFTBlock parent) {
        super(size, height, creationTime, creator, parent, PBFT_BLOCK_HASH_SIZE);
    }

    public void addTransaction(EthereumTx tx)
    {
        this.transactions.add(tx);
    }

    public void setTransactions(ArrayList<EthereumTx> transactions)
    {
        this.transactions = transactions;
    }
    
    public ArrayList<EthereumTx> getTransactions()
    {
        return this.transactions;
    }

    

}
