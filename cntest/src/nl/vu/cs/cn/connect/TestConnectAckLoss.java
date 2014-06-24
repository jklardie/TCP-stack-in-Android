package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.UnreliableIPStack;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectAckLoss extends TestConnectSegmentLoss {

    public void testDropFirstACKOutgoing() throws Exception {
        client.dropOutgoing(UnreliableIPStack.Type.ACK, 1);
        performTestSuccess();
    }

    public void testDropFirstACKIncoming() throws Exception {
        server.dropIncoming(UnreliableIPStack.Type.ACK, 1);
        performTestSuccess();
    }

    public void testDropHalfACKOutgoing() throws Exception {
        client.dropOutgoing(UnreliableIPStack.Type.ACK, 5);
        performTestSuccess();
    }

    public void testDropHalfACKIncoming() throws Exception {
        server.dropIncoming(UnreliableIPStack.Type.ACK, 5);
        performTestSuccess();
    }

    public void testDropAllACKOutgoing() throws Exception {
        client.dropOutgoing(UnreliableIPStack.Type.ACK);

        doDropAllACKTest();
    }

    public void testDropAllACKIncoming() throws Exception {
        server.dropIncoming(UnreliableIPStack.Type.ACK);

        doDropAllACKTest();
    }

    private void doDropAllACKTest() throws Exception {
        // even when all ACKs are dropped, the client will think the connection is established.
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

        assertEquals("Server never received ACK, should be reset to LISTEN",
                TransmissionControlBlock.State.LISTEN,
                getServerState());
    }

}
