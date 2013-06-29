package nl.vu.cs.cn.tcp;


public class TimeoutHandler implements OnTimeoutListener {

    private final TransmissionControlBlock tcb;

    public TimeoutHandler(TransmissionControlBlock tcb){
        this.tcb = tcb;
    }

    public void onUserTimeout(){
        /*
         * TODO:
         * - flush all queus
         * - signal user: "error:  connection aborted due to user timeout" in general
         * - signal user: "error:  connection aborted due to user timeout" in any outstanding calls
         * - enter closed state
         */

        tcb.enterState(TransmissionControlBlock.State.CLOSED);
    }

    public void onRetransmissionTimeout(){
        /*
         * TODO:
         * - resend segment at front of retransmission queue again
         * - reinitialze retrainsmission timer
         */
    }

    public void onTimeWaitTimeout(){
        tcb.enterState(TransmissionControlBlock.State.CLOSED);
    }
}
