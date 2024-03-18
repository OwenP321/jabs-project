package jabs.simulator.event;

import java.util.HashMap;

import jabs.ledgerdata.Block;
import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.network.networks.Network;
import jabs.network.node.nodes.Node;

public class BlockConfirmationEvent extends AbstractLogEvent {
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


    public BlockConfirmationEvent(double time, Node node, Network network) {
        super(time);
        this.node = node;
        this.network = network;
        //this.blocksNodes = new HashMap<>();
    }

    public void genBlock(){
        
        System.out.println("DOING THIS");
        this.node = network.getRandomNode();
        block = node.createBlock();
        blocksNodes.put(block, node);
    }

    public Node getNode() {
        return node;
    }

    public Block getBlock() {
        return block;
    }
    public HashMap getAllBlocks(){

        return blocksNodes;
    }
}
