package nl.vu.cs.cn;

import android.util.Log;

import java.io.IOException;
import java.util.Random;

import nl.vu.cs.cn.tcp.TransmissionControlBlock;
import nl.vu.cs.cn.tcp.segment.Segment;

/**
 * Unreliable IP stack that is capable of dropping specific packets
 */
public class UnreliableIP extends IP {

    public enum DropType {
        NONE,
        FIRST,
        ALL,
        FIRST_HALF,
        RANDOM
    }

    private boolean droppedSYN, droppedSYNACK, droppedACK, droppedFIN;
    private int numDroppedSYN, numDroppedSYNACK, numDroppedAck, numDroppedFIN;

    private DropType dropSYNType = DropType.NONE;
    private DropType dropSYNACKType = DropType.NONE;
    private DropType dropACKType = DropType.NONE;
    private DropType dropFINType = DropType.NONE;

    private double dropSYNRate, dropSYNACKRate, dropACKRate, dropFINRate;
    private long sendLatencyMs;
    private boolean corruptFirstDataPacket, corruptFirstAck, corruptFirstSyn, corruptFirstSynAck;
    private boolean corruptAllAck, corruptAllSyn, corruptAllSynAck;
    private boolean corruptedAck, corruptedSyn, corruptedSynAck;
    private long corruptedDataSeq = -1;

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

    protected void dropFIN(DropType type, double... dropRates){
        this.dropFINType = type;
        if(dropRates.length > 0){
            dropFINRate = dropRates[0];
        }
    }

    protected void setIPSendLatency(long sendLatencyMs){
        Log.v("UnreliableIP", "Setting latency to " + sendLatencyMs);
        this.sendLatencyMs = sendLatencyMs;
    }

    protected void corruptFirstDataPacket(boolean corrupt){
        corruptFirstDataPacket = corrupt;
    }
    protected void corruptFirstAck(boolean corrupt){
        corruptFirstAck = corrupt;
    }
    protected void corruptFirstSyn(boolean corrupt){
        corruptFirstSyn = corrupt;
    }
    protected void corruptFirstSynAck(boolean corrupt){
        corruptFirstSynAck = corrupt;
    }
    protected void corruptAllAck(boolean corrupt){
        corruptAllAck = corrupt;
    }
    protected void corruptAllSyn(boolean corrupt){
        corruptAllSyn = corrupt;
    }
    protected void corruptAllSynAck(boolean corrupt){
        corruptAllSynAck = corrupt;
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
                (dropSYNACKType == DropType.FIRST_HALF && numDroppedSYNACK < 5) ||
                (dropSYNACKType == DropType.RANDOM && rand.nextDouble() < dropSYNACKRate))){

            Log.i(TestBase.TAG, "Dropping SYN, ACK segment: " + segment.toString());
            droppedSYNACK = true;
            numDroppedSYNACK += 1;
            return dataClone.length;

        } else if(segment.isSyn() && (dropSYNType == DropType.ALL ||
                (dropSYNType == DropType.FIRST && !droppedSYN) ||
                (dropSYNType == DropType.FIRST_HALF && numDroppedSYN < 5) ||
                (dropSYNType == DropType.RANDOM && rand.nextDouble() < dropSYNRate))){

            Log.i(TestBase.TAG, "Dropping SYN segment: " + segment.toString());
            droppedSYN = true;
            numDroppedSYN += 1;
            return dataClone.length;

        } else if(segment.isFin() && (dropFINType == DropType.ALL ||
                (dropFINType == DropType.FIRST && !droppedFIN) ||
                (dropFINType == DropType.FIRST_HALF && numDroppedFIN < 5) ||
                (dropFINType == DropType.RANDOM && rand.nextDouble() < dropFINRate))) {

            Log.i(TestBase.TAG, "Dropping FIN segment: " + segment.toString());
            droppedFIN = true;
            numDroppedFIN += 1;
            return dataClone.length;
        } else if(segment.isSyn() && ((corruptFirstSyn && !corruptedSyn) || corruptAllSyn)){
            // note, we don't want to corrupt the header, only the data (otherwise the packet might not arrive)
            byte[] corruptData = new byte[10];
            rand.nextBytes(corruptData);

            int dataOffset = Segment.HEADER_SIZE / 2;
            System.arraycopy(corruptData, 0, p.data, dataOffset, corruptData.length);

            corruptedSyn = true;

            Log.i(TestBase.TAG, "Corrupting ack: " + segment.toString());

        } else if(segment.isSyn() && segment.isAck() && ((corruptFirstSynAck && !corruptedSynAck) || corruptAllSynAck)){
            // note, we don't want to corrupt the header, only the data (otherwise the packet might not arrive)
            byte[] corruptData = new byte[10];
            rand.nextBytes(corruptData);

            int dataOffset = Segment.HEADER_SIZE / 2;
            System.arraycopy(corruptData, 0, p.data, dataOffset, corruptData.length);

            corruptedSynAck = true;

            Log.i(TestBase.TAG, "Corrupting ack: " + segment.toString());

        } else if(segment.getDataLength() > 0 && !segment.isFin() && !segment.isSyn()){
            if(corruptFirstDataPacket && (corruptedDataSeq == -1 || corruptedDataSeq == segment.getSeq())){
                // note, we don't want to corrupt the header, only the data (otherwise the packet might not arrive)

                byte[] corruptData = new byte[segment.getDataLength()];
                rand.nextBytes(corruptData);

                int dataOffset = Segment.HEADER_SIZE;
                System.arraycopy(corruptData, 0, p.data, dataOffset, corruptData.length);

                corruptedDataSeq = segment.getSeq();

                Log.i(TestBase.TAG, "Corrupting data segment: " + segment.toString());
            }
        } else if(segment.isAck() && (dropACKType == DropType.ALL ||
                (dropACKType == DropType.FIRST && !droppedACK) ||
                (dropACKType == DropType.FIRST_HALF && numDroppedAck < 5) ||
                (dropACKType == DropType.RANDOM && rand.nextDouble() < dropACKRate))){

            Log.i(TestBase.TAG, "Dropping ACK segment: " + segment.toString());
            droppedACK = true;
            numDroppedAck += 1;
            return dataClone.length;

        } else if(segment.isAck() && ((corruptFirstAck && !corruptedAck) || corruptAllAck)){
            // note, we don't want to corrupt the header, only the data (otherwise the packet might not arrive)
            byte[] corruptData = new byte[10];
            rand.nextBytes(corruptData);

            int dataOffset = Segment.HEADER_SIZE / 2;
            System.arraycopy(corruptData, 0, p.data, dataOffset, corruptData.length);

            corruptedAck = true;

            Log.i(TestBase.TAG, "Corrupting ack: " + segment.toString());

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
