package jabs.scenario;

import jabs.consensus.config.PBFTConsensusConfig;
import jabs.ledgerdata.pbft.PBFTPrePrepareVote;
import jabs.network.message.VoteMessage;
import jabs.ledgerdata.BlockFactory;
import jabs.network.networks.Network;
import jabs.network.networks.pbft.PBFTLocalLANNetwork;
import jabs.network.node.nodes.Node;
import jabs.network.node.nodes.pbft.PBFTNode;

import static jabs.network.node.nodes.pbft.PBFTNode.PBFT_GENESIS_BLOCK;

public class PBFTLANScenario extends AbstractScenario {
    protected int numNodes;
    protected double simulationStopTime;

    public PBFTLANScenario(String name, long seed, int numNodes, double simulationStopTime) {
        super(name, seed);
        this.numNodes = numNodes;
        this.simulationStopTime = simulationStopTime;
    }

    @Override
    public void createNetwork() {
        network = new PBFTLocalLANNetwork(randomnessEngine);
        network.populateNetwork(this.simulator, this.numNodes, new PBFTConsensusConfig());
        PBFTNode node = (PBFTNode) network.getAllNodes().get(0);
        //node.startTxGen();
        //node.generateNewTransaction();
       
    }

    @Override
    protected void insertInitialEvents() {
            /*
             * 
                Node node = (Node) network.getAllNodes().get(0);
                node.broadcastMessage(
                        new VoteMessage(
                                new PBFTPrePrepareVote<>(node,
                                        BlockFactory.samplePBFTBlock(simulator, network.getRandom(),
                                                (PBFTNode) network.getAllNodes().get(0), PBFT_GENESIS_BLOCK)
                                )
                        )
                );
             * 
             */
            PBFTNode nodePBFT = (PBFTNode) network.getAllNodes().get(0);
            nodePBFT.generateNewTransaction();
            //nodePBFT.createBlock();
            //nodePBFT.timeLoop();
            setPBFTNetwork(network);
            
            
        
    }

    @Override
    public boolean simulationStopCondition() {
        return (simulator.getSimulationTime() > this.simulationStopTime);
    }

    public void blockCreation(){
        PBFTNode pbftNode = (PBFTNode) network.getAllNodes();
        pbftNode.createBlock();
       
        pbftNode.broadcastMessage(
                new VoteMessage(
                        new PBFTPrePrepareVote<>(pbftNode,
                                BlockFactory.samplePBFTBlock(simulator, network.getRandom(),
                                        (PBFTNode) network.getAllNodes().get(0), PBFT_GENESIS_BLOCK)
                        )
                )
        );
    }

     public Network getPBNetwork(){
        
        return network;
    }

  
}
