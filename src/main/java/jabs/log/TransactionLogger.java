package jabs.log;

import java.io.IOException;
import java.nio.file.Path;

import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.simulator.event.Event;
import jabs.simulator.event.*;

public class TransactionLogger extends AbstractCSVLogger {

    public TransactionLogger(Path path) throws IOException {
        super(path);
        //TODO Auto-generated constructor stub
    }

    @Override
    protected String csvStartingComment() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected boolean csvOutputConditionBeforeEvent(Event event) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected boolean csvOutputConditionAfterEvent(Event event) {
        // TODO Auto-generated method stub
        return event instanceof TxGenerationProcessSingleNode;
    }

    @Override
    protected boolean csvOutputConditionFinalPerNode() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'csvOutputConditionFinalPerNode'");
    }

    @Override
    protected String[] csvHeaderOutput() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'csvHeaderOutput'");
    }

    @Override
    protected String[] csvEventOutput(Event event) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'csvEventOutput'");
    }
    
}
