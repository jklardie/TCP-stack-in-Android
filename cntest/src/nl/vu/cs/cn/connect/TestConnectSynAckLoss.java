package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.UnreliableIPStack;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectSynAckLoss extends TestConnect {

    public void testDropFirstSYNACKOutgoing() throws Exception {
        server.dropOutgoing(UnreliableIPStack.Type.SYNACK, 1);
        doNormalConnectionTest();
    }

    public void testDropFirstSYNACKIncoming() throws Exception {
        client.dropIncoming(UnreliableIPStack.Type.SYNACK, 1);
        doNormalConnectionTest();
    }

    public void testDropHalfSYNACKOutgoing() throws Exception {
        server.dropOutgoing(UnreliableIPStack.Type.SYNACK, 5);
        doNormalConnectionTest();
    }

    public void testDropHalfSYNACKIncoming() throws Exception {
        client.dropIncoming(UnreliableIPStack.Type.SYNACK, 5);
        doNormalConnectionTest();
    }

    public void testDropAllSYNACKOutgoing() throws Exception {
        server.dropOutgoing(UnreliableIPStack.Type.SYNACK);
        doDropAllSYNACKTest();
    }

    public void testDropAllSYNACKIncoming() throws Exception {
        client.dropIncoming(UnreliableIPStack.Type.SYNACK);
        doDropAllSYNACKTest();
    }

    private void doDropAllSYNACKTest() throws Exception {
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
