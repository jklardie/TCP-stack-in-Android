package nl.vu.cs.cn.tcp.segment;


import android.util.Log;

import nl.vu.cs.cn.IP;

public class SegmentReceiver {

    private static final String TAG = "SegmentReceiver";
    private static final int RECEIVE_TIMEOUT = 60;

    private final IP ip;
    private final OnSegmentArriveListener listener;
    private final IP.Packet packet;

    public SegmentReceiver(OnSegmentArriveListener listener, IP ip){
        this.ip = ip;
        this.listener = listener;
        packet = new IP.Packet();
    }

    public void run(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(packet.data == null || packet.data.length == 0){
                    try {
                        ip.ip_receive_timeout(packet, RECEIVE_TIMEOUT);
                    } catch (Exception e) {
                        Log.v(TAG, "Receive timed out without receiving packet. Retrying");
                        packet.data = null;
                    }
                }

                Log.v(TAG, "Received IP packet");
                Segment segment = new Segment(packet.data, packet.source, packet.destination);
                listener.onSegmentArrive(segment);
            }
        }).start();
    }

}
