package jabs.scenario;

import jabs.consensus.algorithm.PBFT;
import jabs.ledgerdata.BlockFactory;
import jabs.ledgerdata.pbft.PBFTPrePrepareVote;
import jabs.log.AbstractLogger;
import jabs.network.message.VoteMessage;
import jabs.network.networks.Network;
import jabs.network.node.nodes.pbft.PBFTNode;
import jabs.simulator.event.Event;
import jabs.simulator.randengine.RandomnessEngine;
import jabs.simulator.Simulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static jabs.network.node.nodes.pbft.PBFTNode.PBFT_GENESIS_BLOCK;

/**
 * An abstract class for defining a scenario.
 *
 */
public abstract class AbstractScenario {
    /**
     * network which is being used for simulation
     */
    protected Network network;
    protected Simulator simulator;
    protected RandomnessEngine randomnessEngine;
    protected List<AbstractLogger> loggers = new ArrayList<>();
    long progressMessageIntervals;
    final String name;

    double blockCreationIntervals;
    double txCreationTime;
    double leadBlock;

    PBFTNode nodePBFT; 
    ArrayList<PBFTNode> nodes = new ArrayList<PBFTNode>();

    /**
     * Returns the network of the scenario. This can be used for accessing nodes inside the network.
     * @return network of this scenario
     */
    public Network getNetwork() {
        return this.network;
    }

    /**
     * Returns the simulator object that the scenario is using. This can be used to access the events in simulator.
     * @return simulator object of the scenario
     */
    public Simulator getSimulator() {
        return this.simulator;
    }

    /**
     * @return simulator the name of this simulation.
     */
    public String getName() {
        return this.name;
    }

    public void setPBFTNetwork(Network network){
        this.network = network;
        nodePBFT = (PBFTNode) network.getAllNodes().get(0);
        runOnAllNodes(network);
    }

    public void runOnAllNodes(Network network){
        //nodePBFT = (PBFTNode) network.getAllNodes().get(0);
        for(int i =0; i< network.getAllNodes().size(); i++)
        {
            nodePBFT = (PBFTNode) network.getAllNodes().get(i);
            nodes.add(nodePBFT);
            System.out.println("NODE ADDED :" + i);
        }
        System.out.println("ALL NODES ADDED");
    }   


    /**
     * Create the network and set up the simulation environment.
     */
    abstract protected void createNetwork();

    /**
     * Insert initial events into the event queue.
     */
    abstract protected void insertInitialEvents();

    /**
     * runs before each event and checks if simulation should stop.
     * @return true if simulation should not continue to execution of next event.
     */
    abstract protected boolean simulationStopCondition();

    /**
     * creates an abstract scenario with a user defined name
     * @param name scenario name string
     * @param seed this value gives the simulation a randomnessEngine seed
     */
    public AbstractScenario(String name, long seed) {
        this.randomnessEngine = new RandomnessEngine(seed);
        this.name = name;
        simulator = new Simulator();
        this.progressMessageIntervals = TimeUnit.SECONDS.toNanos(2);

        this.blockCreationIntervals = 50;
        this.txCreationTime = 2;
        this.leadBlock =60;

        //nodePBFT = (PBFTNode) network.getAllNodes().get(0);
    }

    public void setBlockCreationInterval(long blockProgressionInterval){
        this.blockCreationIntervals = blockProgressionInterval;
    }
    public void setTxTime(long txTime)
    {
        this.txCreationTime = txTime;
    }
    public void setLeaderBlock(long leaderBlockTime)
    {
        this.leadBlock = leaderBlockTime;
    }


    /**
     * Adds a new logger module to the simulation scenario
     * @param logger the logger module
     */
    public void AddNewLogger(AbstractLogger logger) {
        this.loggers.add(logger);
    }

    /**
     * Sets the interval between two in progress messages
     * @param progressMessageIntervals the progress message interval described in nanoseconds
     */
    public void setProgressMessageIntervals(long progressMessageIntervals) {
        this.progressMessageIntervals = progressMessageIntervals;
    }

    public void finalStop(){
        PBFTNode nodePBFT = (PBFTNode) network.getAllNodes().get(0);
        //nodePBFT = (PBFTNode) network.getAllNodes().get(0);
        nodePBFT.stopTime();
    }

    /**
     * When called starts the simulation and runs everything to the end of simulation. This also
     * logs events using the logger object.
     * @throws IOException
     */
    public void run() throws IOException {
        System.err.printf("Staring %s...\n", this.name);
        this.createNetwork();
        this.insertInitialEvents();
       

        for (AbstractLogger logger:this.loggers) {
            logger.setScenario(this);
            logger.initialLog();
        }
        long simulationStartingTime = System.nanoTime();
        long lastProgressMessageTime = simulationStartingTime;


        double lastTxGenTime = this.simulator.getSimulationTime();
        double lastBlockCreation = this.simulator.getSimulationTime();
        double lastLeader = this.simulator.getSimulationTime();

        while (simulator.isThereMoreEvents() && !this.simulationStopCondition()) {
            Event event = simulator.peekEvent();
            for (AbstractLogger logger:this.loggers) {
                logger.logBeforeEachEvent(event);
            }
            simulator.executeNextEvent();
            for (AbstractLogger logger:this.loggers) {
                logger.logAfterEachEvent(event);
            }
            if (System.nanoTime() - lastProgressMessageTime > this.progressMessageIntervals) {
                double realTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - simulationStartingTime);
                double simulationTime = this.simulator.getSimulationTime();
                System.err.printf(
                        "Simulation in progress... " +
                        "Elapsed Real Time: %d:%02d:%02d, Elapsed Simulation Time: %d:%02d:%02d\n",
                        (long)(realTime / 3600), (long)((realTime % 3600) / 60), (long)(realTime % 60),
                        (long)(simulationTime / 3600), (long)((simulationTime % 3600) / 60), (long)(simulationTime % 60)
                        );
                        lastProgressMessageTime = System.nanoTime();
                    }
                    
                    if (this.simulator.getSimulationTime() - lastTxGenTime > this.txCreationTime)
                    {
                        lastLeader =simulator.getSimulationTime();
                        for(int x =0; x<nodes.size(); x++)
                        {
                            //System.out.println("****TX GEN*****");
                            //nodes.get(x).generateNewTransaction();
                            nodes.get(x).startTxGen();
                            
                        }
                        //nodePBFT.generateNewTransaction();
                        
                        lastTxGenTime = this.simulator.getSimulationTime();
                    }
                
                if(this.simulator.getSimulationTime() - lastBlockCreation > this.blockCreationIntervals) {
                    
                System.out.println("******** SCENARIO BROADCAST TXS ***********");
                        
                System.out.println(simulator.getSimulationTime());
                for(int x =0; x<nodes.size(); x++)
                {
                    System.out.println("**** BLOCK GEN *****");
                    //nodes.get(x).createBlockEvent(x);


                    //FOR THE BROADCAST OF BLOCKS
                    nodes.get(x).createBlock();

                    //nodePBFT.broadcastMessage(
                    //new VoteMessage(
                    //    new PBFTPrePrepareVote<>(nodePBFT,
                    //            BlockFactory.samplePBFTBlock(simulator, network.getRandom(),
                    //                    (PBFTNode) network.getAllNodes().get(0), PBFT_GENESIS_BLOCK)
                    //    )
                    //));
                }



                
                //if (this.simulator.getSimulationTime() - lastLeader > 2.0)
                //{
                    
                    System.out.println("****LEAD NODE BLOCK GEN*****");
                    nodes.get(0).createLeaderBlock();
                        //nodes.get(x).generateNewTransaction();
                        //nodes.get(0).createLeaderBlock();
                        
                    
    
                    lastLeader = this.simulator.getSimulationTime();
                    System.out.println(simulator.getSimulationTime());
                //}
                //nodePBFT.createBlock();

                //nodePBFT.broadcastMessage(
                //new VoteMessage(
                //        new PBFTPrePrepareVote<>(nodePBFT,
                //                BlockFactory.samplePBFTBlock(simulator, network.getRandom(),
                //                       (PBFTNode) network.getAllNodes().get(0), PBFT_GENESIS_BLOCK)
                //        )
                //));

                double realTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - simulationStartingTime);
                double simulationTime = this.simulator.getSimulationTime();
                System.err.printf(
                        "Simulation in progress... BLOCK CREATION ACTIVE" +
                                "Elapsed Real Time: %d:%02d:%02d, Elapsed Simulation Time: %d:%02d:%02d\n",
                        (long)(realTime / 3600), (long)((realTime % 3600) / 60), (long)(realTime % 60),
                        (long)(simulationTime / 3600), (long)((simulationTime % 3600) / 60), (long)(simulationTime % 60));

                lastBlockCreation = this.simulator.getSimulationTime();
            }





        }
        for (AbstractLogger logger:this.loggers) {
            logger.finalLog();
        }

        //PBFT.writeFinalBlocksToCSV("output/FinalBlocks.csv");

        nodePBFT.checkBlocks();
        
        finalStop();
        System.err.printf("Finished %s.\n", this.name);
    }
}
