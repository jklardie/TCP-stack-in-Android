package nl.vu.cs.cn.tcp.segment;

import java.nio.ByteBuffer;

import nl.vu.cs.cn.IP;
import nl.vu.cs.cn.tcp.Util;

/**
 * A TCP segment which contains a header and a data part.
 */
public class Segment {

    /**
     * Masks for control bits.
     * 1  = 000001
     * 2  = 000010
     * 4  = 000100
     * 8  = 001000
     * 16 = 010000
     * 32 = 100000
     *
     * Using bitwise operators it is easy to set the control bits using the logical OR '|' operation.
     */
    public static final short URG_MASK = 32;
    public static final short ACK_MASK = 16;
    public static final short PSH_MASK = 8;
    public static final short RST_MASK = 4;
    public static final short SYN_MASK = 2;
    public static final short FIN_MASK = 1;


    private IP.IpAddress sourceAddr;
    private IP.IpAddress destinationAddr;
    private short sourcePort;
    private short destinationPort;

    private int seq;    // segment sequence number
    private int ack;    // segment acknowledgement number
    private int len;    // segment length (both header + data)
    private int wnd;    // segment window

    // Note: we omit variables urgent pointer and precendence value because
    // those are unsupported in this implementation.

    private byte[] data;
    private boolean isUrg, isAck, isPsh, isRst, isSyn, isFin;

    private short checksum;
    private final boolean validChecksum;


    public Segment(byte[] packet, int sourceAddr, int destinationAddr){
        this.sourceAddr = IP.IpAddress.getAddress(sourceAddr);
        this.destinationAddr = IP.IpAddress.getAddress(destinationAddr);

        ByteBuffer bb = ByteBuffer.wrap(packet);

        sourcePort = bb.getShort();
        destinationPort = bb.getShort();
        seq = bb.getInt();
        ack = bb.getInt();

        /*
         * bits contains data offset, reserved area, and control bits.
         * It looks like 0101 0000 0011 1111 (if all flags would be true,
         * and the data offset would be 5). We ignore the data offset for now,
         * and read out the control bits using the logical and operation.
         */
        short bits = bb.getShort();
        setControlBits(bits);

        wnd = bb.getShort();
        int checksumPosition = bb.position();
        checksum = bb.getShort();

        // ignore urgent pointer, never used in this implementation
        bb.getShort();

        // options never used in this implementation, so data comes next (if there is data)
        if(bb.remaining() > 0){
            data = new byte[bb.remaining()];
            bb.get(data);
        }

        // check if checksum is correct
        bb.putShort(checksumPosition, (short)0);

        short expectedChecksum = Util.calculateChecksum(bb, this.sourceAddr, this.destinationAddr, bb.capacity());
        validChecksum = (expectedChecksum == checksum);
    }

    public void setControlBits(short bits){
        isUrg = (URG_MASK & bits) != 0;
        isAck = (ACK_MASK & bits) != 0;
        isPsh = (PSH_MASK & bits) != 0;
        isRst = (RST_MASK & bits) != 0;
        isSyn = (SYN_MASK & bits) != 0;
        isFin = (FIN_MASK & bits) != 0;
    }

    public IP.IpAddress getSourceAddr(){
        return sourceAddr;
    }

    public short getSourcePort() {
        return sourcePort;
    }

    public short getDestinationPort() {
        return destinationPort;
    }

    public int getSeq() {
        return seq;
    }

    public int getAck() {
        return ack;
    }

    public int getLen() {
        return len;
    }

    public byte[] getData(){
        return data;
    }

    public int getDataLength(){
        return (data == null) ? 0 : data.length;
    }

    public boolean isUrg() {
        return isUrg;
    }

    public boolean isAck() {
        return isAck;
    }

    public boolean isPsh() {
        return isPsh;
    }

    public boolean isRst() {
        return isRst;
    }

    public boolean isSyn() {
        return isSyn;
    }

    public boolean isFin() {
        return isFin;
    }
}
