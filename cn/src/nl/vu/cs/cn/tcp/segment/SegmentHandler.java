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
        Log.v(TAG, "New segment arrived");

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
                    Log.w(TAG, "onSegmentArrive(): unacceptable segment received. Dropping segment");
                    // TODO: send ACK <SEQ=SND.NXT><ACK=RCV.NXT><CTL=ACK>
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
                handleSegmentText(segment);

                // eigth, check the FIN bit
                if(segment.isFin()){
                    handleSegmentFIN(segment);
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

            tcb.setReceiveNext(segment.getSeq()+1);
            tcb.setInitialReceiveSequenceNumber(segment.getSeq());

            // TODO: queue any other control or text for processing later (actually, can SYN contain data?).

            // Send SYN segment <SEQ=ISS><ACK=RCV.NXT><CTL=SYN,ACK>
            int iss = tcb.getInitialSendSequenceNumber();
            Segment outSegment = SegmentUtil.getSYNACKPacket(tcb, iss, tcb.getReceiveNext());
            IP.Packet packet = IPUtil.getPacket(outSegment);
            try {
                Log.v(TAG, "Sending SYN ACK " + outSegment.getSeq());
                ip.ip_send(packet);
                tcb.addToRetransmissionQueue(new RetransmissionSegment(outSegment));
            } catch (IOException e) {
                Log.e(TAG, "Error while sending SYN ACK", e);

                // TODO: howto handle this?
                return;
            }

            tcb.setSendNext(iss+1);
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
            if(segment.getAck() <= tcb.getInitialSendSequenceNumber() ||
                    segment.getAck() > tcb.getSendNext()){
                // Normally a RESET would be sent, but that is not supported in this situation.
                Log.w(TAG, "onSegmentArrive(): unexpected ACK num (segment discarded)");
                return;
            } else if(tcb.getSendUnacknowledged() <= segment.getAck() &&
                    segment.getAck() <= tcb.getSendNext()){
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
            tcb.setReceiveNext(segment.getSeq()+1);
            tcb.setInitialReceiveSequenceNumber(segment.getSeq());

            tcb.setSendUnacknowledged(segment.getAck());
            tcb.removeFromRetransmissionQueue(segment.getAck());

            if(tcb.getSendUnacknowledged() > tcb.getInitialSendSequenceNumber()){
                // our SYN has been ACKed
                tcb.enterState(TransmissionControlBlock.State.ESTABLISHED);
                // TODO: send ACK segment <SEQ=SND.NXT><ACK=RCV.NXT><CTL=ACK>
                // Data or controls which were queued for transmission may be included
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
        if(tcb.getState() == TransmissionControlBlock.State.SYN_RECEIVED){
            if(tcb.getSendUnacknowledged() <= segment.getAck() &&
                    segment.getAck() <= tcb.getSendNext()){
                tcb.enterState(TransmissionControlBlock.State.ESTABLISHED);
            } else {
                // RESET should be send, not supported though.
            }

        } else if(tcb.getState() == TransmissionControlBlock.State.ESTABLISHED){
            if(tcb.getSendUnacknowledged() < segment.getAck() &&
                    segment.getAck() <= tcb.getSendNext()){
                tcb.setSendUnacknowledged(segment.getAck());
                // TODO: Any segments on the retransmission queue which are thereby
                // entirely acknowledged are removed.

                // TODO: Users should receive positive acknowledgments for buffers
                // which have been SENT and fully acknowledged
            } else if(segment.getAck() < tcb.getSendUnacknowledged()){
                Log.v(TAG, "onSegmentArrive(): duplicate ACK received. Ignoring");
            } else if(segment.getAck() > tcb.getSendNext()){
                Log.v(TAG, "onSegmentArrive(): ACK acks non-sent seq num. Dropping segment");
                // TODO: send ACK
                return false;
            }

            if(tcb.getSendUnacknowledged() < segment.getAck() &&
                    segment.getAck() <= tcb.getSendNext()){
                // TODO: update send window according to page 72
            }

        } else if(tcb.getState() == TransmissionControlBlock.State.FIN_WAIT_1){
            // TODO: same as in ESTABLISHED state
            // TODO: check if FIN is acknowledged. If so, enter FIN_WAIT_2 state
            tcb.enterState(TransmissionControlBlock.State.FIN_WAIT_2);
        } else if(tcb.getState() == TransmissionControlBlock.State.FIN_WAIT_2){
            // TODO: same as in ESTABLISHED state
            // TODO: check if retransmission queue is empty. If so:
            // return OK to users close call
        } else if(tcb.getState() == TransmissionControlBlock.State.CLOSE_WAIT){
            // TODO: same as in ESTABLISHED state
        } else if(tcb.getState() == TransmissionControlBlock.State.CLOSING){
            // TODO: same as in ESTABLISHED state
            // TODO: check if this ACK acks our FIN. If so:
            tcb.enterState(TransmissionControlBlock.State.TIME_WAIT);
            // otherwise: ignore segment:
            return false;
        } else if(tcb.getState() == TransmissionControlBlock.State.LAST_ACK){
            // The only thing that can arrive in this state is an acknowledgment of our FIN.
            tcb.enterState(TransmissionControlBlock.State.CLOSED);
            return false;
        } else if(tcb.getState() == TransmissionControlBlock.State.TIME_WAIT){
            // The only thing that can arrive in this state is a retransmission of the remote FIN.
            // TODO: send ACK and restart 2*MSL timeout
        }

        return true;
    }

    private void handleSegmentText(Segment segment){
        switch(tcb.getState()){
            case ESTABLISHED:
            case FIN_WAIT_1:
            case FIN_WAIT_2:
                Log.v(TAG, "onSegmentArrive(): adding data to processing queue");
                tcb.queueDataForProcessing(segment.getData(), 0, segment.getDataLength());
                // TODO: notify threads waiting for data

                // TODO: send ACK <SEQ=SND.NXT><ACK=RCV.NXT><CTL=ACK>
                // TODO: update receive next sequence number to.. something
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
        // TODO: advance RCV.NXT over the FIN

        switch(tcb.getState()){
            case SYN_RECEIVED:
            case ESTABLISHED:
                tcb.enterState(TransmissionControlBlock.State.CLOSE_WAIT);
                return;
            case FIN_WAIT_1:
                // TODO: check if our FIN has been ACKED. if so:
                tcb.enterState(TransmissionControlBlock.State.TIME_WAIT);
                // TODO: start time-wait timer, turn of other timers
                // else:
                tcb.enterState(TransmissionControlBlock.State.CLOSING);
                return;
            case FIN_WAIT_2:
                tcb.enterState(TransmissionControlBlock.State.TIME_WAIT);
                // TODO: start time-wait timer, turn of other timers
                return;
            case CLOSE_WAIT:
            case CLOSING:
            case LAST_ACK:
                // do nothing, stay in same state
                return;
            case TIME_WAIT:
                // remain in TIME_WAIT state
                // TODO: start 2 * MSL timer
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
        if(segment.getLen() == 0){
            return tcb.getReceiveNext() <= segment.getSeq() &&
                    segment.getSeq() < (tcb.getReceiveNext()+tcb.getReceiveWindow());
        } else if(segment.getLen() > 0){
            return (tcb.getReceiveNext() <= segment.getSeq() &&
                    segment.getSeq() < (tcb.getReceiveNext()+tcb.getReceiveWindow()) ) ||

                    (tcb.getReceiveNext() <= segment.getSeq()+segment.getLen()-1 &&
                            segment.getSeq()+segment.getLen()-1 < (tcb.getReceiveNext()+tcb.getReceiveWindow()) );
        }

        // Segment length < 0 not accepted
        return false;
    }

}
