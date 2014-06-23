package nl.vu.cs.cn;

import java.io.IOException;

/**
 * TCP stack that uses an unreliable ip connected capable of dropping specific packets.
 */
public class UnreliableTCP extends TCP {

    private final UnreliableIP unreliableIP;

    public UnreliableTCP(int address) throws IOException {
        super(address);
        ip = new UnreliableIP(address);
        unreliableIP = (UnreliableIP) ip;
    }

    public void dropSYN(UnreliableIP.DropType type, double... dropRates){
        unreliableIP.dropSYN(type, dropRates);
    }

    public void dropSYNACK(UnreliableIP.DropType type, double... dropRates){
        unreliableIP.dropSYNACK(type, dropRates);
    }

    public void dropACK(UnreliableIP.DropType type, double... dropRates){
        unreliableIP.dropACK(type, dropRates);
    }

    public void dropFIN(UnreliableIP.DropType type, double... dropRates){
        unreliableIP.dropFIN(type, dropRates);
    }

    public void setIPSendLatency(long sendLatencyMs){
        unreliableIP.setIPSendLatency(sendLatencyMs);
    }

    public void corruptFirstDataPacket(boolean corrupt){
        unreliableIP.corruptFirstDataPacket(corrupt);
    }

    public void corruptFirstAck(boolean corrupt){
        unreliableIP.corruptFirstAck(corrupt);
    }

    public void corruptFirstSyn(boolean corrupt){
        unreliableIP.corruptFirstSyn(corrupt);
    }

    public void corruptFirstSynAck(boolean corrupt){
        unreliableIP.corruptFirstSynAck(corrupt);
    }
    
    public void corruptAllAck(boolean corrupt){
        unreliableIP.corruptAllAck(corrupt);
    }

    public void corruptAllSyn(boolean corrupt){
        unreliableIP.corruptAllSyn(corrupt);
    }

    public void corruptAllSynAck(boolean corrupt){
        unreliableIP.corruptAllSynAck(corrupt);
    }
}
