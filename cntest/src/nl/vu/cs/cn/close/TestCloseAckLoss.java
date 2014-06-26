package nl.vu.cs.cn.close;


import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.UnreliableIPStack;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

/**
 * Test the normal flow off closing a connection
 */
public class TestCloseAckLoss extends TestBase {

    private int clientSleep;
    private int serverSleep;
    private int clientDropNumIncoming;
    private int clientDropNumOutgoing;
    private int serverDropNumIncoming;
    private int serverDropNumOutgoing;

    public void testDropFirstACKOutgoingClientFirst() throws Exception {
        clientDropNumOutgoing = 1;
        testClientFirstClose();
    }

    public void testDropFirstACKIncomingClientFirst() throws Exception {
        serverDropNumIncoming = 1;
        testClientFirstClose();
    }

    public void testDropHalfFINOutgoingClientFirst() throws Exception {
        clientDropNumOutgoing = 5;
        testClientFirstClose();
    }

    public void testDropHalfFINIncomingClientFirst() throws Exception {
        serverDropNumIncoming = 5;
        testClientFirstClose();
    }

    public void testDropFirstACKOutgoingServerFirst() throws Exception {
        clientDropNumOutgoing = 1;
        testServerFirstClose();
    }

    public void testDropFirstACKIncomingServerFirst() throws Exception {
        serverDropNumIncoming = 1;
        testServerFirstClose();
    }

    public void testDropHalfFINOutgoingServerFirst() throws Exception {
        clientDropNumOutgoing = 5;
        testServerFirstClose();
    }

    public void testDropHalfFINIncomingServerFirst() throws Exception {
        serverDropNumIncoming = 5;
        testServerFirstClose();
    }

    public void testClientFirstClose() throws Exception {
        // server sleeps 2 seconds before closing, to make sure client closes first
        // client sleeps 0,5 seconds before closing, to make sure server reads ESTABLISHED state
        clientSleep = 500;
        serverSleep = 2000;

        doCloseTestLoseFinAck();
    }

    public void testServerFirstClose() throws Exception {
        // client sleeps 2 seconds before closing, to make sure server closes first
        // server sleeps 0,5 seconds before closing, to make sure client reads ESTABLISHED state
        clientSleep = 2000;
        serverSleep = 500;

        doCloseTestLoseFinAck();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        clientSleep = 0;
        serverSleep = 0;
        clientDropNumIncoming = 0;
        clientDropNumOutgoing = 0;
        serverDropNumIncoming = 0;
        serverDropNumOutgoing = 0;
    }

    public void doCloseTestLoseFinAck() throws Exception {
        startServer(new ServerRunnable());
        connect();

        try {
            Thread.sleep(clientSleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        client.dropIncoming(UnreliableIPStack.Type.ACK, clientDropNumIncoming);
        client.dropOutgoing(UnreliableIPStack.Type.ACK, clientDropNumOutgoing);
        
        // start close procedure
        boolean closed = clientSocket.close();

        // test client part
        assertTrue("Expected clientSocket.close() to return true", closed);

        assertEquals("Client should be in CLOSED state",
                TransmissionControlBlock.State.CLOSED,
                getClientState());
    }

    protected class ServerRunnable implements Runnable {

        @Override
        public void run() {
            serverSocket.accept();

            try {
                Thread.sleep(serverSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            server.dropIncoming(UnreliableIPStack.Type.ACK, serverDropNumIncoming);
            server.dropOutgoing(UnreliableIPStack.Type.ACK, serverDropNumOutgoing);

            // start close procedure
            boolean closed = serverSocket.close();

            assertTrue("Expected serverSocket.close() to return true", closed);

            assertEquals("Server should be in CLOSED state",
                    TransmissionControlBlock.State.CLOSED,
                    getServerState());
        }
    }

}
