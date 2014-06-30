package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.UnreliableIPStack;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectAckCorrupt extends TestConnect {

    public void testCorruptFirstACKOutgoing() throws Exception {
        client.corruptOutgoing(UnreliableIPStack.Type.ACK, 1);
        doNormalConnectionTest();
    }

    public void testCorruptFirstACKIncoming() throws Exception {
        server.corruptIncoming(UnreliableIPStack.Type.ACK, 1);
        doNormalConnectionTest();
    }

    public void testCorruptHalfACKOutgoing() throws Exception {
        client.corruptOutgoing(UnreliableIPStack.Type.ACK, 5);
        doNormalConnectionTest();
    }

    public void testCorruptHalfACKIncoming() throws Exception {
        server.corruptIncoming(UnreliableIPStack.Type.ACK, 5);
        doNormalConnectionTest();
    }

    public void testCorruptAllACKOutgoing() throws Exception {
        client.corruptOutgoing(UnreliableIPStack.Type.ACK);
        doCorruptAllACKTest();
    }

    public void testCorruptAllACKIncoming() throws Exception {
        server.corruptIncoming(UnreliableIPStack.Type.ACK);
        doCorruptAllACKTest();
    }

    private void doCorruptAllACKTest() throws Exception {
        // even when all ACKs are corruptped, the client will think the connection is established.
        startServer(new ServerRunnable());

        boolean connected = clientSocket.connect(SERVER_IP_ADDR, SERVER_PORT);

        assertTrue("Expected clientSocket.connect() to return true", connected);
        assertEquals(TransmissionControlBlock.State.ESTABLISHED,
                getClientState());

        // wait for server (client will retry 10 times)
        Thread.sleep(6000);

        assertEquals("Server never received ACK, so should still be SYN_RECEIVED",
                TransmissionControlBlock.State.SYN_RECEIVED,
                getServerState());

        // wait for server (client will retry 10 times)
        Thread.sleep(6000);

        assertEquals("Server never received ACK, should be reset to CLOSED",
                TransmissionControlBlock.State.CLOSED,
                getServerState());
    }

}
