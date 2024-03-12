package jabs.simulator.event;

import java.util.ArrayList;

import org.apache.commons.math3.analysis.solvers.NewtonRaphsonSolver;

import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.ledgerdata.pbft.PBFTBlock;
import jabs.network.networks.Network;
import jabs.network.node.nodes.Node;
import jabs.simulator.randengine.RandomnessEngine;
import jabs.simulator.Simulator;


public class BlockCreationEvent{

    protected final Network network;
    Node node;

    PBFTBlock block;

    public BlockCreationEvent(Simulator simulator, RandomnessEngine randomnessEngine, Node node, Network network) {
        this.network = network;
        this.node = node;
        
    }

    public void generateBlocks(){
        this.node = network.getRandomNode();
        PBFTBlock block = node.createBlock();
    }

    public PBFTBlock getBlock(){
        return block;
    }

    
}
