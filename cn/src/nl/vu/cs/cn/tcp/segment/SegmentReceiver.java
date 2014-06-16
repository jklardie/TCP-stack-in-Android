package nl.vu.cs.cn.tcp.segment;


import android.util.Log;
import nl.vu.cs.cn.IP;

public class SegmentReceiver {

    private static final int RECEIVE_TIMEOUT = 10;

    private final IP ip;
    private final OnSegmentArriveListener listener;
    private final IP.Packet packet;

    private volatile boolean shouldStop;

    public SegmentReceiver(OnSegmentArriveListener listener, IP ip){
        this.ip = ip;
        this.listener = listener;
        packet = new IP.Packet();
    }

    public void stop(){
        shouldStop = true;
    }

    public void run(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!shouldStop){
                    // reset packet.data before receiving again
                    packet.data = null;

                    // loop until we receive data
                    while(!shouldStop && (packet.data == null || packet.data.length == 0)){
                        try {
                            ip.ip_receive_timeout(packet, RECEIVE_TIMEOUT);
                            if(packet.data != null){
                                Segment segment = new Segment(packet.data, packet.source, packet.destination);
                                listener.onSegmentArrive(segment);
                            }
                        } catch (Exception e) {
                            packet.data = null;
                            Log.w("SegmentRecvr", "Exception in ip_receive_timeout()", e);
                        }
                    }
                }
            }
        }).start();
    }

}
