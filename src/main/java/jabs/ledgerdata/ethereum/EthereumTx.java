package jabs.ledgerdata.ethereum;

import jabs.ledgerdata.Tx;

public class EthereumTx extends Tx<EthereumTx> {
    final long gas;
    private EthereumAccount sender;


    public EthereumTx(int size, long gas) {
        super(size, 0); // Ethereum does not use transaction hashes in network communication
        this.gas = gas;
    }

    public void setCreationTime(Double time){

    }

    public long getGas() {
        return gas;
    }
}
