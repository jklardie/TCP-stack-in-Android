package nl.vu.cs.cn.tcp.segment;

import android.util.Log;

import java.io.IOException;

import nl.vu.cs.cn.IP;
import nl.vu.cs.cn.IPUtil;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class SegmentHandler implements OnSegmentArriveListener {

    private String TAG = "SegmentHandler";

    private final TransmissionControlBlock tcb;
    private final IP ip;

    public SegmentHandler(TransmissionControlBlock tcb, IP ip){
        this.tcb = tcb;
        this.ip = ip;
        TAG += (tcb.isServer()) ? " [server]" : " [client]";
    }

    public void onSegmentArrive(Segment segment){
        if(!segment.hasValidChecksum()){
            Log.v(TAG, "Received segment with invalid checksum. Dropping segment");
            return;
        }

        Log.v(TAG, "Received: " + segment.toString());

        // we need to handle this segment in a synchronized fashion so other methods have
        // time to finish (e.g. entering a state).
        synchronized (tcb){
            switch(tcb.getState()){
                case CLOSED:
                    // Normally connection would be RESET. However, that is not supported in this implementation.
                    Log.v(TAG, "onSegmentArrive(): segment is dropped. Connection does not exist");
                    return;
                case LISTEN:
                    handleSegmentArriveInListenState(segment);
                    return;
                case SYN_SENT:
                    handleSegmentArriveInSynSentState(segment);
                    return;
                default:
                    // first check sequence number
                    if(!acceptableSegment(segment)){
                        Log.w(TAG, "onSegmentArrive(): unacceptable segment (" + segment.toString() + ") received. Dropping segment");

                        // send ACK <SEQ=SND.NXT><ACK=RCV.NXT><CTL=ACK>
                        Segment outSegment = SegmentUtil.getPacket(tcb, tcb.getSendNext(), tcb.getReceiveNext());
                        IP.Packet packet = IPUtil.getPacket(outSegment);
                        try {
                            Log.v(TAG, "Sending: " + outSegment.toString());
                            ip.ip_send(packet);
                            tcb.addToRetransmissionQueue(new RetransmissionSegment(outSegment));
                        } catch (IOException e) {
                            Log.e(TAG, "Error while sending ACK", e);
                            // TODO: howto handle this? For now just ignore
                        }

                        return;
                    } else {
                        // TODO: ??
                    }

                    // second check the RST bit (not supported by this implementation)
                    // third check security and precedence (not supported by this implementation)

                    // fourth, check the SYN bit
                    if(segment.isSyn()){
                        // This is an error, and should responed with RESET (however that's not supported)
                        Log.e(TAG, "onSegmentArrive(): unexpected SYN segment. Ignoring");
                        return;
                    }

                    // fifth, check the ACK field
                    if(!segment.isAck()){
                        Log.w(TAG, "onSegmentArrive(): unexpected segment without ACK. Dropping segment");
                        return;
                    } else {
                        if(!handleACKArriveInDefaultState(segment)){
                            return;
                        }
                    }

                    // sixth, check the URG bit (not supported in this implementation)

                    // seventh, process the segment text
                    if(segment.getDataLength() > 0){
                        handleSegmentText(segment);
                    }

                    // eigth, check the FIN bit
                    if(segment.isFin()){
                        handleSegmentFIN(segment);
                    }
            }
        }
    }

    private void handleSegmentArriveInListenState(Segment segment){
        if(segment.isRst()){
            // An incoming RST should be ignored.
            Log.v(TAG, "onSegmentArrive(RST): state is LISTEN, RST is ignored");
        } else if(segment.isAck()){
            // Any acknowledgment is bad if it arrives on a connection still in the LISTEN state.
            // Normally connection would be RESET. However, that is not supported in this implementation.
            Log.v(TAG, "onSegmentArrive(): state is LISTEN, unexpected ACK (ignored)");
        } else if(segment.isSyn()){
            Log.v(TAG, "onSegmentArrive(): Received SYN " + segment.getSeq());
            tcb.setForeignSocketInfo(segment.getSourceAddr(), segment.getSourcePort());

            // Normally security would be checked here. However, that is not supported in this implementation.
            // Also, precedence not checked here for the same reason.

            // advance receive next sequence number by the length of this segment,
            // which should be one because it only has the SYN control bit
            tcb.setReceiveNext((segment.getSeq() + segment.getLen()) % Integer.MAX_VALUE);
            tcb.setInitialReceiveSequenceNumber(segment.getSeq());

            // TODO: queue any other control or text for processing later (actually, can SYN contain data?).

            // Send SYN,ACK segment <SEQ=ISS><ACK=RCV.NXT><CTL=SYN,ACK>
            long iss = tcb.getInitialSendSequenceNumber();
            Segment outSegment = SegmentUtil.getSYNACKPacket(tcb, iss, tcb.getReceiveNext());
            IP.Packet packet = IPUtil.getPacket(outSegment);
            try {
                Log.v(TAG, "Sending: " + outSegment.toString());
                ip.ip_send(packet);
                tcb.addToRetransmissionQueue(new RetransmissionSegment(outSegment));
            } catch (IOException e) {
                Log.e(TAG, "Error while sending SYN ACK", e);

                // TODO: howto handle this?
                return;
            }

            tcb.setSendNext(iss + segment.getLen());

            // initial packet so set snd_una
            tcb.setSendUnacknowledged(iss);

            tcb.enterState(TransmissionControlBlock.State.SYN_RECEIVED);
        } else {
            // Any control- or text-bearing segment must have an ack, and would be
            // handled by the isAck() check. This situation is unexpected, but can be ignored.
            Log.w(TAG, "onSegmentArrive(): unexpected state. Not a problem though. Ignoring segment");
        }
    }

    private void handleSegmentArriveInSynSentState(Segment segment){
        // first, check if this is an ACK packet
        if(segment.isAck()){
            if(SegmentUtil.isLess(segment.getAck(), tcb.getInitialSendSequenceNumber()+1) ||
                    SegmentUtil.isGreater(segment.getAck(), tcb.getSendNext())){
                // ACK outisde window. Normally a RESET would be sent, but that is not supported in this situation.
                Log.w(TAG, "onSegmentArrive(): unexpected ACK num (segment discarded)");
                return;
            } else if(SegmentUtil.isLess(segment.getAck(), tcb.getSendNext()+1) ||
                    SegmentUtil.isGreater(segment.getAck(), tcb.getSendUnacknowledged()-1)) {
                // Acceptable ack
                Log.v(TAG, "onSegmentArrive(): acceptable ACK received");
            } else {
                Log.w(TAG, "onSegmentArrive(): unacceptable ACK received. Ignoring");
                return;
            }
        } else {
            // Simultaneous open is not supported (we use a client-server model
            Log.w(TAG, "onSegmentArrive(): excepted SYN-ACK in SYN SENT state. Missing ACK. Ignoring");
            return;
        }

        // Secondly, after checking ACK, RST would be checked. However, that is not supported in this implementation.
        // Thirdly, security and precedence would be checked. Also not implemented.

        // at this point the segment contains an ACK

        // Fourthly, check syn
        if(segment.isSyn()){
            // set receive next sequence number by the length of this segment,
            // which should be one because it only has the SYN control bit
            tcb.setReceiveNext(segment.getSeq() + segment.getLen());
            tcb.setInitialReceiveSequenceNumber(segment.getSeq());

            // first packet, so we need to set snd_una
            tcb.setSendUnacknowledged(segment.getAck());
            tcb.removeFromRetransmissionQueue(segment.getAck());

            if(SegmentUtil.isGreater(tcb.getSendUnacknowledged(), tcb.getInitialSendSequenceNumber())){
                // our SYN has been ACKed
                tcb.enterState(TransmissionControlBlock.State.ESTABLISHED);

                // TODO: add data that has been queued for transmission here

                // Send ACK segment <SEQ=SND.NXT><ACK=RCV.NXT><CTL=ACK>
                Segment outSegment = SegmentUtil.getPacket(tcb, tcb.getSendNext(), tcb.getReceiveNext());
                IP.Packet packet = IPUtil.getPacket(outSegment);
                try {
                    Log.v(TAG, "Sending: " + outSegment.toString());
                    ip.ip_send(packet);
                    tcb.addToRetransmissionQueue(new RetransmissionSegment(outSegment));
                    tcb.advanceSendNext(outSegment.getLen());
                } catch (IOException e) {
                    Log.e(TAG, "Error while sending SYN ACK", e);

                    // TODO: howto handle this?
                    return;
                }

            } else {
                tcb.enterState(TransmissionControlBlock.State.SYN_RECEIVED);
                // TODO: send SYN ACK segment <SEQ=ISS><ACK=RCV.NXT><CTL=SYN,ACK>
                // TODO If there are other controls or text in the segment, queue them for
                // processing after the ESTABLISHED state has been reached
            }
        } else {
            Log.w(TAG, "onSegmentArrive(): excepted SYN-ACK in SYN SENT state. Missing SYN. Ignoring");
            return;
        }
    }

    /**
     * Handle an ACK message when in a state other then CLOSED, LISTEN, or SYN-SENT
     * @param segment
     * @return true if and only if the processing of the segment should continue
     */
    private boolean handleACKArriveInDefaultState(Segment segment){
        switch (tcb.getState()) {
            case SYN_RECEIVED:
                if(SegmentUtil.isLess(segment.getAck(), tcb.getSendNext()+1) || SegmentUtil.isGreater(segment.getAck(), tcb.getSendUnacknowledged())){
                    tcb.enterState(TransmissionControlBlock.State.ESTABLISHED);
                } else {
                    // RESET should be send, not supported though.
                    // TODO: should we accept the data and fin if the ACK is incorrect?
                    // This should never happen as we already check ACK validity earlier
                    return true;
                }
            case ESTABLISHED:
            case FIN_WAIT_1:
            case FIN_WAIT_2:
            case CLOSE_WAIT:
            case CLOSING:
                if(SegmentUtil.isLess(segment.getAck(), tcb.getSendNext()+1) || SegmentUtil.isGreater(segment.getAck(), tcb.getSendUnacknowledged())){
                    tcb.setSendUnacknowledged(segment.getAck());
                    tcb.removeFromRetransmissionQueue(segment.getAck());
                } else if(SegmentUtil.isLess(segment.getAck(), tcb.getSendUnacknowledged())){
                    Log.v(TAG, "onSegmentArrive(): duplicate ACK received. Ignoring");
                } else if(SegmentUtil.isGreater(segment.getAck(), tcb.getSendNext())){
                    Log.v(TAG, "onSegmentArrive(): ACK acks non-sent seq num. Dropping segment");
                    // TODO: send ACK
                    return false;
                }

                if(SegmentUtil.isLess(segment.getAck(), tcb.getSendNext()+1) || SegmentUtil.isGreater(segment.getAck(), tcb.getSendUnacknowledged())){
                    // TODO: normally window size would be updated here. However, that is not supported in this implementation
                }

                if (tcb.getState() == TransmissionControlBlock.State.FIN_WAIT_1) {
                    // Check if our FIN has been ACKed
                    if(SegmentUtil.isGreater(segment.getAck(), tcb.getUnacknowledgedFin())){
                        tcb.enterState(TransmissionControlBlock.State.FIN_WAIT_2);
                    }
                } else if (tcb.getState() == TransmissionControlBlock.State.FIN_WAIT_2) {
                    // TODO: check if retransmission queue is empty. If so:
                    // return OK to users close call
                } else if (tcb.getState() == TransmissionControlBlock.State.CLOSING) {
                    if(SegmentUtil.isGreater(segment.getAck(), tcb.getUnacknowledgedFin())){

                        // This ACK ACKs our FIN, so move to TIME_WAIT and start timer
                        tcb.enterState(TransmissionControlBlock.State.TIME_WAIT);
                        tcb.startTimeWaitTimer();
                    }
                    // otherwise: ignore segment:
                    return false;
                }
                break;
            case LAST_ACK:
                // The only thing that can arrive in this state is an acknowledgment of our FIN.
                tcb.enterState(TransmissionControlBlock.State.CLOSED);
                return false;
            case TIME_WAIT:
                // The only thing that can arrive in this state is a retransmission of the remote FIN.
                // ACK will be sent handleSegmentFin() method

                // (re)start timer
                tcb.startTimeWaitTimer();
                break;
        }

        return true;
    }

    private void handleSegmentText(Segment segment){
        if(segment.getDataLength() == 0){
            return;
        }

        switch(tcb.getState()){
            case ESTABLISHED:
            case FIN_WAIT_1:
            case FIN_WAIT_2:
                Log.v(TAG, "onSegmentArrive(): adding data to processing queue");
                tcb.queueDataForProcessing(segment.getData(), 0, segment.getDataLength());

                // update receive next sequence number to RCV.NXT + segment.getLen()
                tcb.advanceReceiveNext(segment.getLen());

                // send ACK <SEQ=SND.NXT><ACK=RCV.NXT><CTL=ACK>
                Segment outSegment = SegmentUtil.getPacket(tcb, tcb.getSendNext(), tcb.getReceiveNext());
                IP.Packet packet = IPUtil.getPacket(outSegment);
                try {
                    Log.v(TAG, "Sending: " + outSegment.toString());
                    ip.ip_send(packet);
                    tcb.addToRetransmissionQueue(new RetransmissionSegment(outSegment));
                } catch (IOException e) {
                    Log.e(TAG, "Error while sending ACK", e);
                    // TODO: howto handle this? For now just ignore
                }

                return;
            case CLOSE_WAIT:
            case CLOSING:
            case LAST_ACK:
            case TIME_WAIT:
                Log.w(TAG, "onSegmentArrive(): unexpected data segment after receiving FIN. Ignoring");
                return;
        }
    }

    private void handleSegmentFIN(Segment segment){
        if(!segment.isFin()){
            return;
        }

        switch(tcb.getState()){
            // dont handle FIN in specific states
            case CLOSED:
            case LISTEN:
            case SYN_SENT:
                // the SEG.SEQ cannot be validated, so drop
                Log.w(TAG, "onSegmentArrive(): dropping FIN. Was in Closed/Listen/Syn_sent state");
                return;
        }

        // TODO: signal user "connection closing"
        // TODO: return all pending receives with "connection closing"

        // advance receive next sequence number by the length of this segment
        tcb.advanceReceiveNext(segment.getLen());

        // TODO: if segment contains both FIN and text, two ACKs are sent

        // send ack for FIN
        Segment outSegment = SegmentUtil.getPacket(tcb, tcb.getSendNext(), tcb.getReceiveNext());
        IP.Packet packet = IPUtil.getPacket(outSegment);
        try {
            Log.v(TAG, "Sending: " + outSegment.toString());
            ip.ip_send(packet);
            tcb.addToRetransmissionQueue(new RetransmissionSegment(outSegment));
            tcb.advanceSendNext(outSegment.getLen());
        } catch (IOException e) {
            Log.e(TAG, "Error while sending ACK for FIN", e);
            // TODO: howto handle this? For now let other party send FIN again (they did not get the ACK)
        }

        switch(tcb.getState()){
            case SYN_RECEIVED:
            case ESTABLISHED:
                tcb.enterState(TransmissionControlBlock.State.CLOSE_WAIT);
                return;
            case FIN_WAIT_1:
                // Check if our FIN has been ACKed
                if(SegmentUtil.isGreater(segment.getAck(), tcb.getUnacknowledgedFin())){
                    tcb.enterState(TransmissionControlBlock.State.TIME_WAIT);
                    // TODO: start time-wait timer, turn of other timers
                    tcb.startTimeWaitTimer();
                } else {
                    // simultaneous close
                    tcb.enterState(TransmissionControlBlock.State.CLOSING);
                }

                return;
            case FIN_WAIT_2:
                tcb.enterState(TransmissionControlBlock.State.TIME_WAIT);
                tcb.startTimeWaitTimer();
                return;
            case CLOSE_WAIT:
            case CLOSING:
            case LAST_ACK:
                // do nothing, stay in same state
                return;
            case TIME_WAIT:
                // remain in TIME_WAIT state, restart timer
                tcb.startTimeWaitTimer();
                return;
        }
    }

    /**
     * Check whether a segment is acceptable, based on its length, sequence number, and
     * the current window size. A sequence number is valid if it falls inside the limits of
     * RCV.NXT and RCV.NXT + RCV.WND.
     *
     * Note that in this implementation the window size is always equal to the max segment size.
     *
     * @param segment
     * @return true if and only if the segment is acceptable
     */
    private boolean acceptableSegment(Segment segment){
        long lower = tcb.getReceiveNext();
        long upper = tcb.getReceiveNext() + tcb.getReceiveWindow();
        long firstSeq = segment.getSeq();
        long lastSeq = segment.getSeq() + segment.getLen() - 1;

        if(segment.getLen() == 0){
            return SegmentUtil.isLess(firstSeq, upper) && SegmentUtil.isGreater(firstSeq, lower-1);
        } else if(segment.getLen() > 0){
            return SegmentUtil.isLess(firstSeq, upper) && SegmentUtil.isGreater(firstSeq, lower-1) ||
                    SegmentUtil.isLess(lastSeq, upper) && SegmentUtil.isGreater(lastSeq, lower-1);
        }

        // Segment length < 0 not accepted
        return false;
    }


}
