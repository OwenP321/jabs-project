package jabs.simulator.event;

import java.util.HashMap;

import jabs.ledgerdata.Block;
import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.ledgerdata.pbft.PBFTBlock;
import jabs.network.networks.Network;
import jabs.network.node.nodes.Node;


public class BlockLeaderCreationEvent extends AbstractLogEvent {
       /**
     * This is the node that confirms a block.
     */
    private Node node;
    /**
     * The block that gets confirmed
     */
    
    private  Block block;
    private  Network network;

    private HashMap<Block, Node> blocksNodes;


    public BlockLeaderCreationEvent(double time, Node node, Network network) {
        super(time);
        this.node = node;
        this.network = network;
        //this.blocksNodes = new HashMap<>();
    }

    public void generateBlocks(){
        this.node = network.getNode(0);
        PBFTBlock block = node.createLeaderBlock();
    }

    public PBFTBlock getBlock(){
        return (PBFTBlock) block;
    }

    
}
