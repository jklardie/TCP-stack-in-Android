package nl.vu.cs.cn;

import android.util.Log;

import java.io.IOException;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.tcp.Segment;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

/**
 * This class represents a TCP stack. It should be built on top of the IP stack
 * which is bound to a given IP address.
 */
public class TCP {

    private static final String TAG = "TCP";

    private static final short LOCAL_PORT = 3110;

    /** The underlying IP stack for this TCP stack. */
    private IP ip;
    private TransmissionControlBlock tcb;
    private boolean sendIssued;


    /**
     * Constructs a TCP stack for the given virtual address.
     * The virtual address for this TCP stack is then
     * 192.168.1.address.
     *
     * @param address The last octet of the virtual IP address 1-254.
     * @throws IOException if the IP stack fails to initialize.
     */
    public TCP(int address) throws IOException {
        ip = new IP(address);
    }

    /**
     * @return a new socket for this stack
     */
    public Socket socket() {
        return new Socket();
    }

    /**
     * @return a new server socket for this stack bound to the given port
     * @param port the port to bind the socket to.
     */
    public Socket socket(int port) {
        return new Socket(port);
    }

    /**
     * ??
     * @param buf
     * @param offset
     * @param len
     * @return
     */
    public int send(byte[] buf, int offset, int len) {
        switch(tcb.getState()){
            case CLOSED:
                Log.e(TAG, "Error in send(): connection does not exist");
                return -1;
            case LISTEN:
                Log.e(TAG, "Error in send(): switching from passive to active open is not supported");
                return -1;
            case SYN_SENT:
            case SYN_RECEIVED:
                // Queue data for transmission after entering ESTABLISHED state
                int bytesQueued = tcb.queueDataForTransmission(buf, offset, len);
                if(bytesQueued < len){
                    // this will (should) never happen. Adding data to queue always succeeds
                    Log.e(TAG, "Error in send(): insufficient resources");
                    return -1;
                }
                Log.v(TAG, "send(): data queued for transmission after entering ESTABLISHED");
                // TODO: should we already return, or wait for ACK?
                return bytesQueued;
            case ESTABLISHED:
            case CLOSE_WAIT:
                // segmentize buffer and send it with a piggybacked ACK
                // TODO: implement
                Log.v(TAG, "send(): buffer segmentized and piggybacked with ACK");
                sendIssued = true;
                // TODO: don't forget return here
            default:
                Log.e(TAG, "Error in send(): connection closing");
                return -1;
        }
    }

    /**
     * ??
     * @param buf
     * @param offset
     * @param maxlen
     * @return
     */
    public int receive(byte[] buf, int offset, int maxlen) {
        switch(tcb.getState()){
            case CLOSED:
                Log.e(TAG, "Error in receive(): connection does not exist");
                return -1;
            case LISTEN:
            case SYN_SENT:
            case SYN_RECEIVED:
                Log.v(TAG, "receive(): call queued until state is ESTABLISHED");
                // TODO: Queue receive for processing after entering ESTABLISHED state
                // e.g. block until state == established, and process receive
                // TODO: don't forget return here.
            case ESTABLISHED:
            case FIN_WAIT_1:
            case FIN_WAIT_2:
                if(!tcb.hasDataToProcess()){
                    Log.v(TAG, "receive(): call queued until segments arrive");
                    // TODO: Queue receive for processing after segments come in
                    // e.g. block until new data is received
                }
                Log.v(TAG, "receive(): returning data from processing queue");
                return tcb.getDataToProcess(buf, offset, maxlen);
            case CLOSE_WAIT:
                if(!tcb.hasDataToProcess()){
                    Log.e(TAG, "Error in receive(): connection closing");
                    return -1;
                }
                Log.v(TAG, "receive(): returning data from processing queue");
                return tcb.getDataToProcess(buf, offset, maxlen);
            default:
                Log.e(TAG, "Error in receive(): connection closing");
                return -1;
        }
    }

    public void onSegmentArrive(Segment segment, IpAddress source){
        switch(tcb.getState()){
            case CLOSED:
                // Normally connection would be RESET. However, that is not supported in this implementation.
                Log.v(TAG, "onSegmentArrive(): segment is dropped. Connection does not exist");
                return;
            case LISTEN:
                handleSegmentArriveInListenState(segment, source);
                return;
            case SYN_SENT:
                handleSegmentArriveInSynSentState(segment, source);
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
                    if(!handleACKArriveInDefaultState(segment, source)){
                        return;
                    }
                }

                // sixth, check the URG bit (not supported in this implementation)

                // seventh, process the segment text
                handleSegmentText(segment, source);

                // eigth, check the FIN bit
                if(segment.isFin()){
                    handleSegmentFIN(segment, source);
                }
        }
    }

    private void handleSegmentArriveInListenState(Segment segment, IpAddress source){
        if(segment.isRst()){
            // An incoming RST should be ignored.
            Log.v(TAG, "onSegmentArrive(RST): state is LISTEN, RST is ignored");
        } else if(segment.isAck()){
            // Any acknowledgment is bad if it arrives on a connection still in the LISTEN state.
            // Normally connection would be RESET. However, that is not supported in this implementation.
            Log.v(TAG, "onSegmentArrive(): state is LISTEN, unexpected ACK (ignored)");
        } else if(segment.isSyn()){
            // Normally security would be checked here. However, that is not supported in this implementation.
            // Also, precedence not checked here for the same reason.
            tcb.setReceiveNext(segment.getSeq()+1);
            tcb.setInitialReceiveSequenceNumber(segment.getSeq());

            // TODO: queue any other control or text for processing later.

            // TODO: send SYN segment <SEQ=ISS><ACK=RCV.NXT><CTL=SYN,ACK>

            tcb.setSendNext(tcb.getInitialSendSequenceNumber()+1);
            tcb.setSendUnacknowledged(tcb.getInitialSendSequenceNumber());

            tcb.enterState(TransmissionControlBlock.State.SYN_RECEIVED);

            tcb.setForeignSocketInfo(source, segment.getSourcePort());
        } else {
            // Any control- or text-bearing segment must have an ack, and would be
            // handled by the isAck() check. This situation is unexpected, but can be ignored.
            Log.w(TAG, "onSegmentArrive(): unexpected state. Not a problem though. Ignoring segment");
        }
    }

    private void handleSegmentArriveInSynSentState(Segment segment, IpAddress source){
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
        }

        // Secondly, after checking ACK, RST would be checked. However, that is not supported in this implementation.
        // Thirdly, security and precedence would be checked. Also not implemented.

        // at this point the segment either does not contain ACK, or the ACK is ok

        // Fourthly, check syn
        if(segment.isSyn()){
            tcb.setReceiveNext(segment.getSeq()+1);
            tcb.setInitialReceiveSequenceNumber(segment.getSeq());

            if(segment.isAck()){
                tcb.setSendUnacknowledged(segment.getAck());
                // TODO: any segments on the retransmission queue which are
                // hereby acknowledged should be removed.
            }

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
        }
    }

    /**
     * Handle an ACK message when in a state other then CLOSED, LISTEN, or SYN-SENT
     * @param segment
     * @param source
     * @return true if and only if the processing of the segment should continue
     */
    private boolean handleACKArriveInDefaultState(Segment segment, IpAddress source){
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

    private void handleSegmentText(Segment segment, IpAddress source){
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

    private void handleSegmentFIN(Segment segment, IpAddress source){
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

    private void onUserTimeout(){
        /*
         * TODO:
         * - flush all queus
         * - signal user: "error:  connection aborted due to user timeout" in general
         * - signal user: "error:  connection aborted due to user timeout" in any outstanding calls
         * - enter closed state
         */

        tcb.enterState(TransmissionControlBlock.State.CLOSED);
    }

    private void onRetransmissionTimeout(){
        /*
         * TODO:
         * - resend segment at front of retransmission queue again
         * - reinitialze retrainsmission timer
         */
    }

    private void onTimeWaitTimeout(){
        tcb.enterState(TransmissionControlBlock.State.CLOSED);
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




    /**
     * This class represents a TCP socket.
     *
     */
    public class Socket {

        /**
         * Construct a client socket and setup a transmission control block with
         * local socket information.
         */
        private Socket() {
            tcb = new TransmissionControlBlock(ip.getLocalAddress(), LOCAL_PORT);
        }

        /**
         * Construct a server socket bound to the given local port and setup
         * transmission control block with a local socket information.
         *
         * @param port the local port to use
         */
        private Socket(int port) {
            tcb = new TransmissionControlBlock(ip.getLocalAddress(), (short)port);
        }

        /**
         * Connect this socket to the specified destination and port.
         *
         * @param dst the destination to connect to
         * @param port the port to connect to
         * @return true if the connect succeeded.
         */
        public boolean connect(IpAddress dst, int port) {
            if(tcb.getState() == TransmissionControlBlock.State.LISTEN){
                Log.e(TAG, "Error in connect(): switching from passive to active open is not supported");
                return false;
            } else if(tcb.getState() != TransmissionControlBlock.State.CLOSED){
                Log.e(TAG, "Error in connect(): connection already exists");
                return false;
            } else if(dst == null || port == 0){
                Log.e(TAG, "Error in connect(): foreign socket unspecified");
                return false;
            } else if(tcb.hasForeignSocketInfo()){
                Log.e(TAG, "Error in connect(): insufficient resources");
                return false;
            }

            tcb.setForeignSocketInfo(dst, (short)port);

            // TODO: send SYN packet

            tcb.setSendUnacknowledged(tcb.getInitialSendSequenceNumber());
            tcb.setSendNext(tcb.getInitialSendSequenceNumber()+1);
            tcb.enterState(TransmissionControlBlock.State.SYN_SENT);

            return false;
        }

        /**
         * Accept a connection on this socket.
         * This call blocks until a connection is made.
         */
        public void accept() {
            tcb.enterState(TransmissionControlBlock.State.LISTEN);


        }

        /**
         * Reads bytes from the socket into the buffer.
         * This call is not required to return maxlen bytes
         * every time it returns.
         *
         * @param buf the buffer to read into
         * @param offset the offset to begin reading data into
         * @param maxlen the maximum number of bytes to read
         * @return the number of bytes read, or -1 if an error occurs.
         */
        public int read(byte[] buf, int offset, int maxlen) {
            return receive(buf, offset, maxlen);
        }

        /**
         * Writes to the socket from the buffer.
         *
         * @param buf the buffer to
         * @param offset the offset to begin writing data from
         * @param len the number of bytes to write
         * @return the number of bytes written or -1 if an error occurs.
         */
        public int write(byte[] buf, int offset, int len) {
            return send(buf, offset, len);
        }

        /**
         * Closes the connection for this socket.
         * Blocks until the connection is closed.
         *
         * @return true unless no connection was open.
         */
        public boolean close() {
            switch(tcb.getState()){
                case CLOSED:
                    Log.e(TAG, "Error in close(): connection does not exist");
                    return false;
                case LISTEN:
                    // TODO: Any outstanding RECEIVEs are returned with "error:  closing" responses
                    tcb.enterState(TransmissionControlBlock.State.CLOSED);
                    return true;
                case SYN_SENT:
                    // TODO: return "error:  closing" responses to any queued SENDs, or RECEIVEs
                    tcb.enterState(TransmissionControlBlock.State.CLOSED);
                    return true;
                case SYN_RECEIVED:
                    // TODO: this probably needs concurrency protection
                    if(!sendIssued && !tcb.hasDataToTransmit()){
                        // TODO: send FIN
                        tcb.enterState(TransmissionControlBlock.State.FIN_WAIT_1);
                    }

                    // TODO: Queue close for processing after entering ESTABLISHED state
                    // e.g. block until state == established, and process close
                    return true;
                case ESTABLISHED:
                    // TODO: Queue this close until all preceding SENDs have been segmentized,
                    // then form a FIN segment and send it (e.g wait until ... something)
                    tcb.enterState(TransmissionControlBlock.State.FIN_WAIT_1);
                    return true;
                case FIN_WAIT_1:
                case FIN_WAIT_2:
                    // TODO: according assignment this should also return true
                    Log.e(TAG, "Error in close(): connection closing");
                    return false;
                case CLOSE_WAIT:
                    // TODO: Queue this request until all preceding SENDs have been segmentized;
                    // then send a FIN segment, enter CLOSING state.
                    // TODO: don't forget the return here
                default:
                    // TODO: according to assignment this should return true
                    Log.e(TAG, "Error in close(): connection closing");
                    return false;
            }
        }
    }
}