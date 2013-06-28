package nl.vu.cs.cn.tcp;

public interface OnTimeoutListener {

    public void onUserTimeout();
    public void onRetransmissionTimeout();
    public void onTimeWaitTimeout();
}
