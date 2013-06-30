package nl.vu.cs.cn;

import android.util.Log;

import java.io.IOException;
import java.util.Random;

import nl.vu.cs.cn.tcp.segment.Segment;

/**
 * Unreliable IP stack that is capable of dropping specific packets
 */
public class UnreliableIP extends IP {

    public enum DropType {
        NONE,
        FIRST,
        ALL,
        RANDOM
    }

    private boolean droppedSYN, droppedSYNACK, droppedACK;

    private DropType dropSYNType = DropType.NONE;
    private DropType dropSYNACKType = DropType.NONE;
    private DropType dropACKType = DropType.NONE;
    private double dropSYNRate, dropSYNACKRate, dropACKRate;
    private long sendLatencyMs;

    public UnreliableIP(int address) throws IOException {
        super(address);
    }

    protected void dropSYN(DropType type, double... dropRates){
        dropSYNType = type;
        if(dropRates.length > 0){
            dropSYNRate = dropRates[0];
        }
    }

    protected void dropSYNACK(DropType type, double... dropRates){
        dropSYNACKType = type;
        if(dropRates.length > 0){
            dropSYNACKRate = dropRates[0];
        }
    }

    protected void dropACK(DropType type, double... dropRates){
        dropACKType = type;
        if(dropRates.length > 0){
            dropACKRate = dropRates[0];
        }
    }

    protected void setIPSendLatency(long sendLatencyMs){
        Log.v("UnreliableIP", "Setting latency to " + sendLatencyMs);
        this.sendLatencyMs = sendLatencyMs;
    }

    @Override
    public int ip_send(Packet p) throws IOException {
        // important: data must be cloned, because the bytebuffer in TCPPacket
        // wraps the byte[]. Changing the bytebuffer changes the byte[].
        byte[] dataClone = new byte[p.data.length];
        System.arraycopy(p.data, 0, dataClone, 0, p.data.length);

        Random rand = new Random();

        Segment segment = new Segment(dataClone, p.source, p.destination);

        // check if we need to drop anything. We either drop, and return the datalength,
        // or we continue, and send the packet normally (at the end of this method).
        if((segment.isAck() && segment.isSyn()) && (dropSYNACKType == DropType.ALL ||
                (dropSYNACKType == DropType.FIRST && !droppedSYNACK) ||
                (dropSYNACKType == DropType.RANDOM && rand.nextDouble() < dropSYNACKRate))){

            Log.i(TestBase.TAG, "Dropping SYN, ACK segment: " + segment.toString());
            droppedSYNACK = true;
            return dataClone.length;

        } else if(segment.isSyn() && (dropSYNType == DropType.ALL ||
                (dropSYNType == DropType.FIRST && !droppedSYN) ||
                (dropSYNType == DropType.RANDOM && rand.nextDouble() < dropSYNRate))){

            Log.i(TestBase.TAG, "Dropping SYN segment: " + segment.toString());
            droppedSYN = true;
            return dataClone.length;

        } else if(segment.isAck() && (dropACKType == DropType.ALL ||
                (dropACKType == DropType.FIRST && !droppedACK) ||
                (dropACKType == DropType.RANDOM && rand.nextDouble() < dropACKRate))){

            Log.i(TestBase.TAG, "Dropping ACK segment: " + segment.toString());
            droppedACK = true;
            return dataClone.length;

        }

        if(sendLatencyMs > 0){
            try {
                Thread.sleep(sendLatencyMs);
            } catch (InterruptedException e) {
                // ignore interrupt
            }
        }

        // normally send packet
        return super.ip_send(p);
    }
}
