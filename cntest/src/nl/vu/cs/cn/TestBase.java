package nl.vu.cs.cn;

import android.util.Log;

import junit.framework.TestCase;

import nl.vu.cs.cn.tcp.TransmissionControlBlock;


public class TestBase extends TestCase {

    public static final String TAG = "TCPTest";

    private static final int CLIENT_ADDR_LAST_OCTET = 15;
    private static final int SERVER_ADDR_LAST_OCTET = 16;

    protected static final int SERVER_PORT = 123;
    protected static final IP.IpAddress SERVER_IP_ADDR = IP.IpAddress.getAddress("192.168.0." + SERVER_ADDR_LAST_OCTET);

    protected UnreliableTCPStack client;
    protected UnreliableTCPStack server;
    protected TCP.Socket clientSocket;
    protected TCP.Socket serverSocket;

    @Override
    protected void setUp() throws Exception {
        Log.i(TAG, "-----------------------------");
        Log.i(TAG, "-----------------------------");
        Log.i(TAG, "-----------------------------");
        Log.i(TAG, "-----------------------------");
        Log.i(TAG, "-----------------------------");

        client = new UnreliableTCPStack(CLIENT_ADDR_LAST_OCTET);
        server = new UnreliableTCPStack(SERVER_ADDR_LAST_OCTET);

        client.reset();
        server.reset();

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

        // Allow segment receiver to stop
        Thread.sleep(1500);

        Log.i(TAG, "-----------------------------");
        Log.i(TAG, "-----------------------------");
        Log.i(TAG, "-----------------------------");
        Log.i(TAG, "-----------------------------");
        Log.i(TAG, "-----------------------------");
    }

    protected void clearRetransmissionQueues() throws Exception {
        // make sure all retransmission tasks are cleared (so other tests can start fresh)
        try {
            client.tcb.clearRetransmissionQueue();
            server.tcb.clearRetransmissionQueue();
        } catch (Exception e) {
            // NullPointerException
        }
    }

    protected void connect() throws Exception {
        // make sure we are connected (both client and server)
        boolean connected = clientSocket.connect(SERVER_IP_ADDR, SERVER_PORT);

        assertTrue("Expected clientSocket.connect() to return true", connected);
        assertEquals("After connect() client should be in the ESTABLISHED state",
                TransmissionControlBlock.State.ESTABLISHED,
                getClientState());

        // Wait until server state changes from LISTEN to anything else
        server.tcb.waitForStates(TransmissionControlBlock.State.ESTABLISHED,
                TransmissionControlBlock.State.CLOSING,
                TransmissionControlBlock.State.CLOSED,
                TransmissionControlBlock.State.SYN_SENT,
                TransmissionControlBlock.State.SYN_RECEIVED,
                TransmissionControlBlock.State.FIN_WAIT_1,
                TransmissionControlBlock.State.FIN_WAIT_2,
                TransmissionControlBlock.State.LAST_ACK,
                TransmissionControlBlock.State.CLOSE_WAIT,
                TransmissionControlBlock.State.TIME_WAIT
                );

        if(getServerState() == TransmissionControlBlock.State.SYN_RECEIVED){
            server.tcb.waitForStates(TransmissionControlBlock.State.ESTABLISHED,
                    TransmissionControlBlock.State.CLOSING,
                    TransmissionControlBlock.State.CLOSED,
                    TransmissionControlBlock.State.SYN_SENT,
                    TransmissionControlBlock.State.FIN_WAIT_1,
                    TransmissionControlBlock.State.FIN_WAIT_2,
                    TransmissionControlBlock.State.LAST_ACK,
                    TransmissionControlBlock.State.CLOSE_WAIT,
                    TransmissionControlBlock.State.TIME_WAIT
            );
        }

        assertEquals("Server should never return before reaching the ESTABLISHED state",
                TransmissionControlBlock.State.ESTABLISHED,
                getServerState());
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
