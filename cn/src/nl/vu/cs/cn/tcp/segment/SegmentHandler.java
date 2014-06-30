package nl.vu.cs.cn.tcp.segment;

import java.io.IOException;

import nl.vu.cs.cn.IP;
import nl.vu.cs.cn.IPUtil;
import nl.vu.cs.cn.Log;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class SegmentHandler implements OnSegmentArriveListener {

    private String TAG = "SegmentHandler";

    private volatile TransmissionControlBlock tcb;
    private volatile IP ip;

    public SegmentHandler(TransmissionControlBlock tcb, IP ip){
        this.tcb = tcb;
        this.ip = ip;
        TAG += (tcb.isServer()) ? " [server]" : " [client]";
    }

    public void onSegmentArrive(Segment segment){
        Log.v(TAG, "Received: " + segment.toString());

        if(!segment.hasValidChecksum()){
            Log.v(TAG, "Received segment with invalid checksum. Dropping segment");
            return;
        }

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
                        } catch (IOException e) {
                            Log.e(TAG, "Error while sending ACK. Will be retransmitted...", e);
                        }

                        return;
                    }

                    // second check the RST bit (not supported by this implementation)
                    // third check security and precedence (not supported by this implementation)

                    // fourth, check the SYN bit
                    if(segment.isSyn()){
                        // This is an error, and should responed with RESET (however that's not supported)
                        Log.e(TAG, "onSegmentArrive(): unexpected SYN segment. Ignoring");
                        tcb.enterState(TransmissionControlBlock.State.CLOSED);
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
            } catch (IOException e) {
                Log.e(TAG, "Error while sending SYN ACK. Will be retried later...", e);
            } finally {
                tcb.addToRetransmissionQueue(new RetransmissionSegment(outSegment));
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

            // this should ACK our SYN (iss)
            if(!SegmentUtil.inWindow(segment.getAck(), segment.getAck(), tcb.getInitialSendSequenceNumber()+2)){
                // ACK outside window. Normally a RESET would be sent, but that is not supported in this situation.
                Log.w(TAG, "onSegmentArrive(): unexpected ACK num (segment discarded)");
                return;
            } else if(SegmentUtil.inWindow(tcb.getSendUnacknowledged(), segment.getAck(), tcb.getSendNext()+1)){
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

            if(SegmentUtil.isAcked(tcb.getInitialSendSequenceNumber(), segment.getAck(), 1)){
                // our SYN has been ACKed
                tcb.enterState(TransmissionControlBlock.State.ESTABLISHED);

                // TODO: add data that has been queued for transmission here

                // Send ACK segment <SEQ=SND.NXT><ACK=RCV.NXT><CTL=ACK>
                Segment outSegment = SegmentUtil.getPacket(tcb, tcb.getSendNext(), tcb.getReceiveNext());
                IP.Packet packet = IPUtil.getPacket(outSegment);
                try {
                    Log.v(TAG, "Sending: " + outSegment.toString());
                    ip.ip_send(packet);
                } catch (IOException e) {
                    Log.e(TAG, "Error while sending ACK.. will be retried", e);
                }

                tcb.advanceSendNext(outSegment.getLen());

            } else {
                tcb.enterState(TransmissionControlBlock.State.SYN_RECEIVED);

                // Send SYN ACK segment <SEQ=ISS><ACK=RCV.NXT><CTL=SYN,ACK>
                Segment outSegment = SegmentUtil.getSYNACKPacket(tcb, tcb.getInitialSendSequenceNumber(), tcb.getReceiveNext());
                IP.Packet packet = IPUtil.getPacket(outSegment);
                try {
                    Log.v(TAG, "Sending: " + outSegment.toString());
                    ip.ip_send(packet);
                } catch (IOException e) {
                    Log.e(TAG, "Error while sending SYN ACK.. will be retried", e);
                } finally {
                    tcb.addToRetransmissionQueue(new RetransmissionSegment(outSegment));
                }
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
                if(SegmentUtil.inWindow(tcb.getSendUnacknowledged(), segment.getAck(), tcb.getSendNext()+1)){
                    Log.d(TAG, "Change state from SYN_RECEIVED to ESTABLISHED. Should continue processing");
                    tcb.enterState(TransmissionControlBlock.State.ESTABLISHED);
                } else {
                    // RESET should be send, not supported though.
                    // This should never happen as we already check ACK validity earlier
                    Log.v(TAG, "onSegmentArrive(): state SYN_RECEIVED, received unacceptable ACK. Dropping segment.");
                    return false;
                }
            case ESTABLISHED:
            case FIN_WAIT_1:
            case FIN_WAIT_2:
            case CLOSE_WAIT:
            case CLOSING:
                Log.d(TAG, "Continuing processing!!!");
                if(SegmentUtil.inWindow(tcb.getSendUnacknowledged()+1, segment.getAck(), tcb.getSendNext()+1)){
                    tcb.setSendUnacknowledged(segment.getAck());
                    tcb.removeFromRetransmissionQueue(segment.getAck());

                    // normally window size would be updated here. However, that is not supported in this implementation
                } else if(SegmentUtil.inWindow(tcb.getSendUnacknowledged()-tcb.getSendWindow(), segment.getAck(), tcb.getSendUnacknowledged())){
                    Log.v(TAG, "onSegmentArrive(): duplicate ACK received. Ignoring");
                } else if(SegmentUtil.inWindow(tcb.getSendNext()+1, segment.getAck(), tcb.getSendNext()+tcb.getSendWindow())){
                    Log.v(TAG, "onSegmentArrive(): ACK acks non-sent seq num. Dropping segment");

                    // send ACK <SEQ=SND.NXT><ACK=RCV.NXT><CTL=ACK>
                    Segment outSegment = SegmentUtil.getPacket(tcb, tcb.getSendNext(), tcb.getReceiveNext());
                    IP.Packet packet = IPUtil.getPacket(outSegment);
                    try {
                        Log.v(TAG, "Sending: " + outSegment.toString());
                        ip.ip_send(packet);
                    } catch (IOException e) {
                        Log.e(TAG, "Error while sending ACK. Will be retransmitted...", e);
                    }

                    return false;
                }

                if (tcb.getState() == TransmissionControlBlock.State.FIN_WAIT_1) {
                    // Check if our FIN has been ACKed
                    if(tcb.getUnacknowledgedFin() != null && SegmentUtil.isAcked(tcb.getUnacknowledgedFin(), segment.getAck())){
                        tcb.enterState(TransmissionControlBlock.State.FIN_WAIT_2);
                    }
                } else if (tcb.getState() == TransmissionControlBlock.State.FIN_WAIT_2) {
                    // This ACKs our FIN packet

                    // TODO: check if retransmission queue is empty. If so:

                    // return OK to users close call
                } else if (tcb.getState() == TransmissionControlBlock.State.CLOSING) {
                    if(SegmentUtil.isAcked(tcb.getUnacknowledgedFin(), segment.getAck())){
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
                if(SegmentUtil.inWindow(tcb.getSendUnacknowledged()+1, segment.getAck(), tcb.getSendNext()+1)){
                    tcb.setSendUnacknowledged(segment.getAck());
                    tcb.removeFromRetransmissionQueue(segment.getAck());
                }
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
                } catch (IOException e) {
                    Log.e(TAG, "Error while sending ACK", e);
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
        } catch (IOException e) {
            Log.e(TAG, "Error while sending ACK for FIN", e);
        }

        tcb.advanceSendNext(outSegment.getLen());

        switch(tcb.getState()){
            case SYN_RECEIVED:
            case ESTABLISHED:
                tcb.enterState(TransmissionControlBlock.State.CLOSE_WAIT);
                return;
            case FIN_WAIT_1:
                // Check if our FIN has been ACKed
                if(SegmentUtil.isAcked(tcb.getUnacknowledgedFin(), segment.getAck())){
                    tcb.enterState(TransmissionControlBlock.State.TIME_WAIT);

                    // start time-wait timer, turn of other timers
                    tcb.startTimeWaitTimer();
                    tcb.clearRetransmissionQueue();
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
     * @param segment
     * @return true if and only if the segment is acceptable
     */
    private boolean acceptableSegment(Segment segment){
        if(tcb.getReceiveWindow() == 0){
            // If the RCV.WND is zero, no segments will be acceptable, except valid ACKs, URGs and RSTs.
            return segment.getLen() == 0 && segment.getSeq() == tcb.getReceiveNext();
        } else if (segment.getLen() == 0) {
            return SegmentUtil.inWindow(tcb.getReceiveNext(), segment.getSeq(), tcb.getReceiveNext() + tcb.getReceiveWindow());
        } else {
            return SegmentUtil.inWindow(tcb.getReceiveNext(), segment.getSeq(), tcb.getReceiveNext() + tcb.getReceiveWindow())
                    || SegmentUtil.inWindow(tcb.getReceiveNext(), segment.getSeq() + segment.getLen()-1, tcb.getReceiveNext());
        }
    }


}
