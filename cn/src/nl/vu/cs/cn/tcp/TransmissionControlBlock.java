package nl.vu.cs.cn.tcp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import nl.vu.cs.cn.IP;
import nl.vu.cs.cn.tcp.segment.RetransmissionSegment;
import nl.vu.cs.cn.tcp.timeout.TimeoutHandler;

/**
 * The Transmission Control Block in TCP keeps track of the state of a connection,
 * and contains all other important information needed during a connection.
 */
public class TransmissionControlBlock {

    private static final int RETRANSMIT_TIMEOUT_SEC = 1;    // number of time before retransmit
    public static final int MAX_RETRANSMITS = 10;          // maximum number of retransmits

    public enum State {
        CLOSED,
        LISTEN,
        SYN_RECEIVED,
        SYN_SENT,
        ESTABLISHED,
        FIN_WAIT_1,
        FIN_WAIT_2,
        TIME_WAIT,
        CLOSING,
        CLOSE_WAIT,
        LAST_ACK
    };

    private boolean isServer;   // used for logging purposes

    private State state;
    private IP.IpAddress localAddr;
    private short localPort;
    private IP.IpAddress foreignAddr;
    private short foreignPort;

    // sequence variables
    private final int iss;      // initial send sequence number
    private int irs;            // initial receive sequence number

    // send sequence variables (note that window and urgent pointer info is not used)
    private int snd_una;        // send - unacknowledged sequence number
    private int snd_nxt;        // send - next sequence number
    private int snd_wnd;        // send - window (offset of snd_una)


    // receive sequence variables
    private int rcv_nxt;        // receive - next sequence number
    private int rcv_wnd;        // receive - window

    private ConcurrentHashMap<RetransmissionSegment, ScheduledFuture> retransmissionMap;
    private ConcurrentLinkedQueue<Byte> transmissionQueue;
    private ConcurrentLinkedQueue<Byte> processingQueue;

    private TimeoutHandler timeoutHandler;

    /**
     * Create a new transmission control block (TCB) to hold connection state information.
     * When this method finishes the state is set to CLOSED.
     */
    public TransmissionControlBlock(IP ip) {
        iss = getInitialSendSequenceNumber();
        state = State.CLOSED;
        
        retransmissionMap = new ConcurrentHashMap<RetransmissionSegment, ScheduledFuture>();
        transmissionQueue = new ConcurrentLinkedQueue<Byte>();
        processingQueue = new ConcurrentLinkedQueue<Byte>();

        timeoutHandler = new TimeoutHandler(ip, this);
    }

    /**
     * Enter a specific TCP state
     * @param state
     */
    public void enterState(State state){
        this.state = state;
    }

    /**
     * Get the current state the TCP is in.
     * @return
     */
    public State getState(){
        return state;
    }

    public boolean isServer() {
        return isServer;
    }

    public void setIsServer(boolean isServer) {
        this.isServer = isServer;
    }

    /**
     * Set local socket address and port
     * @param localAddr
     * @param localPort
     */
    public void setLocalSocketInfo(IP.IpAddress localAddr, short localPort) {
        this.localAddr = localAddr;
        this.localPort = localPort;
    }

    /**
     * Get the local ip address
     * @return
     */
    public IP.IpAddress getLocalAddr(){
        return localAddr;
    }

    /**
     * Get the local tcp port
     * @return
     */
    public short getLocalport(){
        return localPort;
    }

    /**
     * Set foreign socket address and port
     * @param foreignAddr
     * @param foreignPort
     */
    public void setForeignSocketInfo(IP.IpAddress foreignAddr, short foreignPort) {
        this.foreignAddr = foreignAddr;
        this.foreignPort = foreignPort;
    }

    /**
     * Return true if and only if the foreign address and port have been set.
     * @return
     */
    public boolean hasForeignSocketInfo() {
        return foreignAddr != null && foreignPort != 0;
    }

    /**
     * Get foreign ip address
     * @return
     */
    public IP.IpAddress getForeignAddr(){
        return foreignAddr;
    }

    /**
     * Get foreign tcp port
     * @return
     */
    public short getForeignPort(){
        return foreignPort;
    }




    ////////////////////////
    // Sequence number methods
    ////////////////////////

    /**
     * Returns the initial sequence number, and creates it if this is the first call.
     * @return
     */
    public int getInitialSendSequenceNumber() {
        if(iss == 0){
            // TODO: implement create
        }

        return iss;
    }

    /**
     * Set the initial receive sequencen number.
     * @param irs
     */
    public void setInitialReceiveSequenceNumber(int irs){
        this.irs = irs;
    }

    /**
     * Set send unacknowledged sequence number.
     * @param snd_una
     */
    public void setSendUnacknowledged(int snd_una){
        this.snd_una = snd_una;
    }

    /**
     * Get send unacknowledged sequence number.
     * @return
     */
    public int getSendUnacknowledged(){
        return snd_una;
    }

    /**
     * Set send next sequence number.
     * @param snd_nxt
     */
    public void setSendNext(int snd_nxt){
        this.snd_nxt = snd_nxt;
    }

    /**
     * Get send next sequence number.
     * @return
     */
    public int getSendNext(){
        return snd_nxt;
    }

    /**
     * Set send window
     * @param snd_wnd
     * @return
     */
    public void setSendWindow(int snd_wnd){
        this.snd_wnd = snd_wnd;
    }

    /**
     * Set receive next sequence number.
     * @param rcv_nxt
     */
    public void setReceiveNext(int rcv_nxt){
        this.rcv_nxt = rcv_nxt;
    }

    /**
     * Get receive next sequence number.
     * @return
     */
    public int getReceiveNext(){
        return rcv_nxt;
    }

    /**
     * Get receive window.
     * @return
     */
    public int getReceiveWindow(){
        return rcv_wnd;
    }




    ////////////////////////
    // Data methods
    ////////////////////////

    /**
     * Add data to the transmission queue.
     * @param buf
     * @param offset
     * @param len
     * @return number of bytes added. This is always equal to len.
     */
    public int queueDataForTransmission(byte[] buf, int offset, int len){
        int i;
        for(i=0; i<len; i++){
            transmissionQueue.add(buf[offset+i]);
        }
        return i;
    }

    /**
     * Check whether or not there is data to transmit.
     * @return true if and only if there is data queued to transmit. False otherwise.
     */
    public boolean hasDataToTransmit(){
        return transmissionQueue.size() > 0;
    }

    /**
     * Add data to the processing queue.
     * @param buf
     * @param offset
     * @param len
     * @return number of bytes added to the queue. This is always equal to len
     */
    public int queueDataForProcessing(byte[] buf, int offset, int len){
        int i;
        for(i=0; i<len; i++){
            processingQueue.add(buf[offset+i]);
        }
        return i;
    }

    /**
     * Check whether or not there is data to process.
     * @return true if and only if there is data queued to transmit. False otherwise
     */
    public boolean hasDataToProcess(){
        return processingQueue.size() > 0;
    }

    /**
     * Write maxlen bytes of data into buf.
     * @param buf
     * @param offset
     * @param maxlen
     * @return the number of bytes written to buf
     */
    public int getDataToProcess(byte[] buf, int offset, int maxlen){
        int len = Math.min(maxlen, processingQueue.size());
        for(int i=0; i<len; i++){
            buf[offset+i] = processingQueue.remove();
        }
        return len;
    }

    
    ////////////////////////
    // Retransmission methods
    ////////////////////////

    /**
     * Add a segment to the retransmission queue and start a timer to check if the ACK
     * timed out.
     *
     * @param retransmissionSegment
     */
    public void addToRetransmissionQueue(final RetransmissionSegment retransmissionSegment){
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        ScheduledFuture task = exec.schedule(new Runnable() {
            @Override
            public void run() {
                timeoutHandler.onRetransmissionTimeout(retransmissionSegment);
            }
        }, RETRANSMIT_TIMEOUT_SEC, TimeUnit.SECONDS);

        retransmissionMap.put(retransmissionSegment, task);
    }

    /**
     * Remove all segments from the retransmission queue which have a sequence number
     * smaller than the ack number
     * @param ack
     */
    public void removeFromRetransmissionQueue(int ack){
        for(RetransmissionSegment segment : retransmissionMap.keySet()){
            if(segment.getSegment().getSeq() < ack){
                retransmissionMap.remove(segment);
            }
        }
    }

    /**
     * Remove a specific segment from the retransmission queue
     * @param retransmissionSegment
     * @return true if and only if the segment existed (and was removed)
     */
    public boolean removeFromRetransmissionQueue(RetransmissionSegment retransmissionSegment) {
        return (retransmissionMap.remove(retransmissionSegment) != null);
    }

}
