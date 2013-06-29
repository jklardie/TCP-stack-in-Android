package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.UnreliableIP;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectSynLoss extends TestConnect {

    @Override
    protected void setUp() throws Exception {
        client = server = null;
        clientSocket = serverSocket = null;
    }

    @Override
    public void testConnect() throws Exception {
        // don't test normal connect here
    }

    public void testDropFirstSYN() throws Exception {
        init();
        client.dropSYN(UnreliableIP.DropType.FIRST);

        Thread server = startServer(new ServerRunnable());

        boolean connected = clientSocket.connect(SERVER_IP_ADDR, SERVER_PORT);

        assertTrue("Expected clientSocket.connect() to return true", connected);
        assertEquals(TransmissionControlBlock.State.ESTABLISHED,
                getClientState());

        // wait 15 seconds for server to finish, or continue
        server.join(15);

        assertEquals("Three-way handshake succeeded. Server should be in ESTABLISHED state",
                TransmissionControlBlock.State.ESTABLISHED,
                getServerState());

        clearRetransmissionQueues();
    }

    public void testDropAllSYN() throws Exception {
        init();
        client.dropSYN(UnreliableIP.DropType.ALL);

        Thread server = startServer(new ServerRunnable());

        boolean connected = clientSocket.connect(SERVER_IP_ADDR, SERVER_PORT);

        assertFalse("Expected clientSocket.connect() to return false", connected);
        assertEquals(TransmissionControlBlock.State.CLOSED,
                getClientState());

        // wait 15 seconds for server to finish, or continue
        server.join(15);

        assertEquals("Three-way handshake failed. Server should still be in LISTEN state",
                TransmissionControlBlock.State.LISTEN,
                getServerState());

        clearRetransmissionQueues();
    }
}
