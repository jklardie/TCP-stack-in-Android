package nl.vu.cs.cn.tcp;

/**
 * A TCP segment which contains a header and a data part.
 */
public class Segment {

    private short sourcePort;
    private short destinationPort;

    private int seq;    // segment sequence number
    private int ack;    // segment acknowledgement number
    private int len;    // segment length (both header + data)
    private int wnd;    // segment window

    // Note: we omit variables urgent pointer and precendence value because
    // those are unsupported in this implementation.

    private byte[] data;

    // control bits
    private boolean isUrg, isAck, isPsh, isRst, isSyn, isFin;

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
