package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.UnreliableIPStack;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectSynAckCorrupt extends TestConnect {

    public void testCorruptFirstSYNACKOutgoing() throws Exception {
        server.corruptOutgoing(UnreliableIPStack.Type.SYNACK, 1);
        doNormalConnectionTest();
    }

    public void testCorruptFirstSYNACKIncoming() throws Exception {
        client.corruptIncoming(UnreliableIPStack.Type.SYNACK, 1);
        doNormalConnectionTest();
    }

    public void testCorruptHalfSYNACKOutgoing() throws Exception {
        server.corruptOutgoing(UnreliableIPStack.Type.SYNACK, 5);
        doNormalConnectionTest();
    }

    public void testCorruptHalfSYNACKIncoming() throws Exception {
        client.corruptIncoming(UnreliableIPStack.Type.SYNACK, 5);
        doNormalConnectionTest();
    }

    public void testCorruptAllSYNACKOutgoing() throws Exception {
        server.corruptOutgoing(UnreliableIPStack.Type.SYNACK);
        doCorruptAllSYNACKTest();
    }

    public void testCorruptAllSYNACKIncoming() throws Exception {
        client.corruptIncoming(UnreliableIPStack.Type.SYNACK);
        doCorruptAllSYNACKTest();
    }

    private void doCorruptAllSYNACKTest() throws Exception {
        startServer(new ServerRunnable());

        boolean connected = clientSocket.connect(SERVER_IP_ADDR, SERVER_PORT);

        assertFalse("Expected clientSocket.connect() to return false", connected);
        assertEquals(TransmissionControlBlock.State.CLOSED,
                getClientState());

        // Both client and server are waiting for each other to ACK packets
        // Wait 1 second, so both have time to set states correctly
        Thread.sleep(1000);

        assertEquals("Server never received ACK, should be reset to CLOSED",
                TransmissionControlBlock.State.CLOSED,
                getServerState());
    }

}
