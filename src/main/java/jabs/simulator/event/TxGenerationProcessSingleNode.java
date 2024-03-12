package jabs.simulator.event;

import java.util.ArrayList;

import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.network.networks.Network;
import jabs.network.node.nodes.Node;
import jabs.simulator.randengine.RandomnessEngine;
import jabs.simulator.Simulator;

public class TxGenerationProcessSingleNode extends AbstractTxPoissonProcess {

    protected final Network network;

    EthereumTx tx;
    ArrayList<EthereumTx> allTxs = new ArrayList<>();


    public TxGenerationProcessSingleNode(Simulator simulator, RandomnessEngine randomnessEngine, Node node, double averageTimeBetweenTxs, Network network) {
        super(simulator, randomnessEngine, averageTimeBetweenTxs);
        this.network = network;
        this.node = node;
    }

    @Override
    public void generate() {
        this.node = network.getRandomNode();
        tx = node.generateNewPBFTTransaction();
        allTxs.add(tx);
        //System.out.println("NEW TX");
    }

    public EthereumTx getTx(){



        return tx;
    }

    public ArrayList<EthereumTx> getAllTxs(){

        return allTxs;
    }
}
