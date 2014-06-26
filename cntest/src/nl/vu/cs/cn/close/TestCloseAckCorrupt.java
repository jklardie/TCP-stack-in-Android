package nl.vu.cs.cn.close;


import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.UnreliableIPStack;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

/**
 * Test the normal flow off closing a connection
 */
public class TestCloseAckCorrupt extends TestBase {

    private int clientSleep;
    private int serverSleep;
    private int clientCorruptNumIncoming;
    private int clientCorruptNumOutgoing;
    private int serverCorruptNumIncoming;
    private int serverCorruptNumOutgoing;

    public void testCorruptFirstACKOutgoingClientFirst() throws Exception {
        clientCorruptNumOutgoing = 1;
        testClientFirstClose();
    }

    public void testCorruptFirstACKIncomingClientFirst() throws Exception {
        serverCorruptNumIncoming = 1;
        testClientFirstClose();
    }

    public void testCorruptHalfFINOutgoingClientFirst() throws Exception {
        clientCorruptNumOutgoing = 5;
        testClientFirstClose();
    }

    public void testCorruptHalfFINIncomingClientFirst() throws Exception {
        serverCorruptNumIncoming = 5;
        testClientFirstClose();
    }

    public void testCorruptFirstACKOutgoingServerFirst() throws Exception {
        clientCorruptNumOutgoing = 1;
        testServerFirstClose();
    }

    public void testCorruptFirstACKIncomingServerFirst() throws Exception {
        serverCorruptNumIncoming = 1;
        testServerFirstClose();
    }

    public void testCorruptHalfFINOutgoingServerFirst() throws Exception {
        clientCorruptNumOutgoing = 5;
        testServerFirstClose();
    }

    public void testCorruptHalfFINIncomingServerFirst() throws Exception {
        serverCorruptNumIncoming = 5;
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
        clientCorruptNumIncoming = 0;
        clientCorruptNumOutgoing = 0;
        serverCorruptNumIncoming = 0;
        serverCorruptNumOutgoing = 0;
    }

    public void doCloseTestLoseFinAck() throws Exception {
        startServer(new ServerRunnable());
        connect();

        try {
            Thread.sleep(clientSleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        client.corruptIncoming(UnreliableIPStack.Type.ACK, clientCorruptNumIncoming);
        client.corruptOutgoing(UnreliableIPStack.Type.ACK, clientCorruptNumOutgoing);
        
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

            server.corruptIncoming(UnreliableIPStack.Type.ACK, serverCorruptNumIncoming);
            server.corruptOutgoing(UnreliableIPStack.Type.ACK, serverCorruptNumOutgoing);

            // start close procedure
            boolean closed = serverSocket.close();

            assertTrue("Expected serverSocket.close() to return true", closed);

            assertEquals("Server should be in CLOSED state",
                    TransmissionControlBlock.State.CLOSED,
                    getServerState());
        }
    }

}
