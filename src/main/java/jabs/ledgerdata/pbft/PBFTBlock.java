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
   
    private int size;
    private int height;
    private double creationTime;
    private Node creator;
    private PBFTBlock parent;
    private int hashCode;

    private ArrayList<EthereumTx> finalTransactions;
    

    public PBFTBlock(int size, int height, double creationTime, Node creator, PBFTBlock parent) {
        super(size, height, creationTime, creator, parent, PBFT_BLOCK_HASH_SIZE);
        this.size = size;
        this.height = height;
        this.creationTime = creationTime;
        this.creator = creator;
        this.parent = parent;
        this.block =this;
        //transactions = new ArrayList<>();
    }

    public void addTransaction(EthereumTx tx)
    {
        this.transactions.add(tx);
    }

    public void setTransactions(ArrayList<EthereumTx> transactions)
    {
        this.transactions = transactions;
        finalTransactions = transactions;
        //System.out.println("***************** PROBLEM" +transactions);
    }

    public void setValidTxs(ArrayList<EthereumTx> tx)
    {
        this.transactions = tx;
    }
    
    public ArrayList<EthereumTx> getTransactions()
    {
        return this.finalTransactions;
    }

    public PBFTBlock getBlock(){
        return this.block;
    }
    public int getSize(){
        return size;
    }
    public int getHeight(){
        return height;
    }
    public double getCreationTime(){
        return creationTime;
    }
    public Node getCreator(){
        return creator;
    }
    public int getHashCode(){
        return hashCode;
    }


    




    

}
