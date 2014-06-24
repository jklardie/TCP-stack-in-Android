package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.UnreliableIPStack;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectSynLoss extends TestConnect {

    public void testDropFirstSYNOutgoing() throws Exception {
        client.dropOutgoing(UnreliableIPStack.Type.SYN, 1);
        doNormalConnectionTest();
    }

    public void testDropFirstSYNIncoming() throws Exception {
        server.dropIncoming(UnreliableIPStack.Type.SYN, 1);
        doNormalConnectionTest();
    }

    public void testDropHalfSYNOutgoing() throws Exception {
        client.dropOutgoing(UnreliableIPStack.Type.SYN, 5);
        doNormalConnectionTest();
    }

    public void testDropHalfSYNIncoming() throws Exception {
        server.dropIncoming(UnreliableIPStack.Type.SYN, 5);
        doNormalConnectionTest();
    }

    public void testDropAllSYNOutgoing() throws Exception {
        client.dropOutgoing(UnreliableIPStack.Type.SYN);
        doDropAllSYNTest();
    }

    public void testDropAllSYNIncoming() throws Exception {
        server.dropIncoming(UnreliableIPStack.Type.SYN);
        doDropAllSYNTest();
    }

    private void doDropAllSYNTest() throws Exception {
        startServer(new ServerRunnable());

        boolean connected = clientSocket.connect(SERVER_IP_ADDR, SERVER_PORT);

        assertFalse("Expected clientSocket.connect() to return false", connected);
        assertEquals(TransmissionControlBlock.State.CLOSED,
                getClientState());

        // wait for server
        Thread.sleep(1000);

        assertEquals("Server never received SYN, so state should still be LISTEN",
                TransmissionControlBlock.State.LISTEN,
                getServerState());
    }

}
