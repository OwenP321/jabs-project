package jabs.ledgerdata.pbft;

import java.util.ArrayList;

import jabs.ledgerdata.SingleParentBlock;
import jabs.network.node.nodes.Node;
import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.ledgerdata.Recipt;

public class PBFTBlock extends SingleParentBlock<PBFTBlock> {
    public static final int PBFT_BLOCK_HASH_SIZE = 32;

    private final PBFTBlock block;
    protected ArrayList<EthereumTx> transactions;
   
    private Node creator;

    public PBFTBlock(int size, int height, double creationTime, Node creator, PBFTBlock parent) {
        super(size, height, creationTime, creator, parent, PBFT_BLOCK_HASH_SIZE);
        this.block =this;
        this.creator = creator;
    }

    public void addTransaction(EthereumTx tx)
    {
        this.transactions.add(tx);
    }

    public void setTransactions(ArrayList<EthereumTx> transactions)
    {
        this.transactions = transactions;
        //System.out.println("***************** PROBLEM" +transactions);
    }

    public void setValidTxs(ArrayList<EthereumTx> tx)
    {
        this.transactions = tx;
    }
    
    public ArrayList<EthereumTx> getTransactions()
    {
        return this.transactions;
    }

    public PBFTBlock getBlock(){
        return this.block;
    }

    public Node getNode(){
        return creator;
    }
    

}