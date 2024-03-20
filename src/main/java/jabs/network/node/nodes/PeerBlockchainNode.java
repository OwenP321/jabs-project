package jabs.network.node.nodes;

import jabs.consensus.algorithm.AbstractChainBasedConsensus;
import jabs.consensus.algorithm.PBFTTxVote;
import jabs.consensus.blockchain.LocalBlockTree;
import jabs.ledgerdata.*;
import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.ledgerdata.pbft.PBFTBlock;
import jabs.ledgerdata.pbft.PBFTTransactionVote;
import jabs.network.message.*;
import jabs.network.networks.Network;
import jabs.network.p2p.AbstractP2PConnections;
import jabs.network.p2p.PBFTP2P;
import jabs.simulator.Simulator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

@SuppressWarnings("hiding")
public abstract class PeerBlockchainNode<B extends SingleParentBlock<B>, T extends Tx<T>> extends PeerDLTNode<B, T> {
    protected final AbstractChainBasedConsensus<B, T> consensusAlgorithm;

    protected final HashMap<Hash, T> alreadySeenTxs = new HashMap<>();
    protected final HashMap<Hash, PBFTBlock> alreadySeenBlocks = new HashMap<>();
    protected final HashSet<Vote> alreadySeenVotes = new HashSet<>();
    protected final HashSet<Query> alreadySeenQueries = new HashSet<>();
    protected final LocalBlockTree<PBFTBlock> localBlockTree;

    public PeerBlockchainNode(Simulator simulator, Network network, int nodeID, long downloadBandwidth,
                              long uploadBandwidth, AbstractP2PConnections routingTable,
                              AbstractChainBasedConsensus<B, T> consensusAlgorithm) {
        super(simulator, network, nodeID, downloadBandwidth, uploadBandwidth, routingTable, consensusAlgorithm);
        this.consensusAlgorithm = consensusAlgorithm;
        this.localBlockTree = (LocalBlockTree<PBFTBlock>) consensusAlgorithm.getLocalBlockTree();
    }


    @Override
    public void processIncomingPacket(Packet packet) {
        Message message = packet.getMessage();
        if (message instanceof DataMessage) {
            Data data = ((DataMessage) message).getData();
            if (data instanceof PBFTBlock) {
                PBFTBlock block = (PBFTBlock) data;
                if (!localBlockTree.contains(block)){
                    localBlockTree.add(block);
                    alreadySeenBlocks.put(block.getHash(), block);
                    if (localBlockTree.getLocalBlock(block).isConnectedToGenesis) {
                        this.processNewBlock(block);
                        SortedSet<PBFTBlock> newBlocks = new TreeSet<>(localBlockTree.getAllSuccessors(block));
                        for (PBFTBlock newBlock:newBlocks){
                            this.processNewBlock(newBlock);
                        }
                    } else {
                        this.networkInterface.addToUpLinkQueue(
                                new Packet(this, packet.getFrom(),
                                        new RequestDataMessage(block.getParent().getHash())
                                )
                        );
                    }
                }
            } else if (data instanceof Tx) {
                T tx = (T) data;
                if (!alreadySeenTxs.containsValue(tx)){
                    alreadySeenTxs.put(tx.getHash(), tx);
                    this.processNewTx(tx, packet.getFrom());
                }
            } else if (data instanceof Recipt){
                Recipt recipt = (Recipt) data;
                //PROCESS NEW RECPIT 
            }
        } else if (message instanceof InvMessage) {
            Hash hash = ((InvMessage) message).getHash();
            if (hash.getData() instanceof Block){
                if (!alreadySeenTxs.containsKey(hash)) {
                    alreadySeenTxs.put(hash, null);
                    this.networkInterface.addToUpLinkQueue(
                            new Packet(this, packet.getFrom(),
                                    new RequestDataMessage(hash)
                            )
                    );
                }
            } else if (hash.getData() instanceof Tx) {
                if (!alreadySeenBlocks.containsKey(hash)) {
                    alreadySeenBlocks.put(hash, null);
                    this.networkInterface.addToUpLinkQueue(
                            new Packet(this, packet.getFrom(),
                                    new RequestDataMessage(hash)
                            )
                    );
                }
            }
        } else if (message instanceof RequestDataMessage) {
            Hash hash = ((RequestDataMessage) message).getHash();
            if (hash.getData() instanceof Block) {
                if (alreadySeenBlocks.containsKey(hash)) {
                    PBFTBlock block = alreadySeenBlocks.get(hash);
                    if (block != null) {
                        this.networkInterface.addToUpLinkQueue(
                                new Packet(this, packet.getFrom(),
                                        new DataMessage(block)
                                )
                        );
                    }
                }
            } else if (hash.getData() instanceof Tx) {
                if (alreadySeenTxs.containsKey(hash)) {
                    T tx = alreadySeenTxs.get(hash);
                    if (tx != null) {
                        this.networkInterface.addToUpLinkQueue(
                                new Packet(this, packet.getFrom(),
                                        new DataMessage(tx)
                                )
                        );
                        
                    }
                }
            }
        } else if (message instanceof VoteMessage) {
            Vote vote = ((VoteMessage) message).getVote();
            if (!alreadySeenVotes.contains(vote)) {
                alreadySeenVotes.add(vote);
                this.processNewVote(vote);
            }
        }else if (message instanceof QueryMessage) {
            Query query = ((QueryMessage) message).getQuery();
            if (!alreadySeenQueries.contains(query)) {
                alreadySeenQueries.add(query);
                this.processNewQuery(query);
            }
        }
    }

    protected abstract void processNewBlock(PBFTBlock block);
    protected abstract void processNewVote(Vote vote);
    protected abstract void processNewQuery(Query query);

    protected abstract void processNewTxVote(PBFTTransactionVote vote);

    public AbstractChainBasedConsensus<B, T> getConsensusAlgorithm() {
        return this.consensusAlgorithm;
    }

    public int numberOfAlreadySeenBlocks() {
        return alreadySeenBlocks.size();
    }
}
