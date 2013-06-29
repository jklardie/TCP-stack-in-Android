package nl.vu.cs.cn.tcp.segment;

import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public abstract class SegmentUtil {

    /**
     * Construct the initial SYN packet needed to start the three-way handshake
     * @param tcb
     * @return
     */
    public static Segment getSYNPacket(TransmissionControlBlock tcb, int seq){
        Segment segment = new Segment(
                tcb.getLocalAddr(), tcb.getForeignAddr(),
                tcb.getLocalport(), tcb.getForeignPort(),
                seq, tcb.getSendWindow());

        segment.setIsSyn(true);
        return segment;
    }

    /**
     * Construct a SYN ACK packet used during the three-way handshake
     * @param tcb
     * @return
     */
    public static Segment getSYNACKPacket(TransmissionControlBlock tcb, int seq, int ack){
        Segment segment = getPacket(tcb, seq, ack);
        segment.setIsSyn(true);
        // isAck is automatically set inside getPacket()

        return segment;
    }

    /**
     * Construct an ACK packet possibly containing data
     * @param tcb
     * @return
     */
    public static Segment getPacket(TransmissionControlBlock tcb, int seq, int ack){
        Segment segment = new Segment(
                tcb.getLocalAddr(), tcb.getForeignAddr(),
                tcb.getLocalport(), tcb.getForeignPort(),
                seq, tcb.getSendWindow(), ack);

        // isAck is automatically set because we passed an ack num to Segment

        return segment;
    }
}
