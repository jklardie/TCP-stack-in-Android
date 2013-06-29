package nl.vu.cs.cn;

import junit.framework.TestCase;

import nl.vu.cs.cn.tcp.TransmissionControlBlock;


public class TestBase extends TestCase {

    public static final String TAG = "TCPTest";

    protected static final int MAX_RUNTIME_MS = 30000;

    private static final int CLIENT_ADDR_LAST_OCTET = 15;
    private static final int SERVER_ADDR_LAST_OCTET = 16;

    protected static final int SERVER_PORT = 123;
    protected static final IP.IpAddress SERVER_IP_ADDR = IP.IpAddress.getAddress("192.168.0." + SERVER_ADDR_LAST_OCTET);

    protected UnreliableTCP client;
    protected UnreliableTCP server;
    protected TCP.Socket clientSocket;
    protected TCP.Socket serverSocket;

    @Override
    public void setUp() throws Exception {
        client = new UnreliableTCP(CLIENT_ADDR_LAST_OCTET);
        server = new UnreliableTCP(SERVER_ADDR_LAST_OCTET);

        clientSocket = client.socket();
        serverSocket = server.socket(SERVER_PORT);
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
