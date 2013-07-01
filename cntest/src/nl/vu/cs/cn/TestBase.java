package nl.vu.cs.cn;

import android.util.Log;

import junit.framework.TestCase;

import nl.vu.cs.cn.tcp.TransmissionControlBlock;
import nl.vu.cs.cn.tcp.segment.SegmentReceiver;


public class TestBase extends TestCase {

    public static final String TAG = "TCPTest";

    private static final int CLIENT_ADDR_LAST_OCTET = 15;
    private static final int SERVER_ADDR_LAST_OCTET = 16;

    protected static final int SERVER_PORT = 123;
    protected static final IP.IpAddress SERVER_IP_ADDR = IP.IpAddress.getAddress("192.168.0." + SERVER_ADDR_LAST_OCTET);

    protected UnreliableTCP client;
    protected UnreliableTCP server;
    protected TCP.Socket clientSocket;
    protected TCP.Socket serverSocket;

    @Override
    protected void setUp() throws Exception {
        client = new UnreliableTCP(CLIENT_ADDR_LAST_OCTET);
        server = new UnreliableTCP(SERVER_ADDR_LAST_OCTET);

        clientSocket = client.socket();
        serverSocket = server.socket(SERVER_PORT);
    }

    @Override
    protected void tearDown() throws Exception {
        clearRetransmissionQueues();

        try {
            client.segmentReceiver.stop();
            Log.i(TAG, "Stopped client segment receiver");
        } catch (NullPointerException e){}
        try {
            server.segmentReceiver.stop();
            Log.i(TAG, "Stopped server segment receiver");
        } catch (NullPointerException e){}

        super.tearDown();
    }

    protected void clearRetransmissionQueues() throws Exception {
        // make sure all retransmission tasks are cleared (so other tests can start fresh)
        try {
            client.tcb.removeFromRetransmissionQueue(Integer.MAX_VALUE);
            server.tcb.removeFromRetransmissionQueue(Integer.MAX_VALUE);
        } catch (Exception e) {
            // NullPointerException
        }
    }

    /**
     * Start a new tread and run the runnable
     * @param runnable
     */
    protected Thread startServer(Runnable runnable){
        Thread thread = new Thread(runnable);
        thread.start();

        try {
            // give server time to enter LISTEN state
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return thread;
    }

    protected TransmissionControlBlock.State getClientState(){
        return client.getState();
    }

    protected TransmissionControlBlock.State getServerState(){
        return server.getState();
    }
}
