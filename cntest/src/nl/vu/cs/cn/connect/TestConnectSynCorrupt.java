package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.UnreliableIPStack;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectSynCorrupt extends TestConnect {

    public void testCorruptFirstSYNOutgoing() throws Exception {
        client.corruptOutgoing(UnreliableIPStack.Type.SYN, 1);
        doNormalConnectionTest();
    }

    public void testCorruptFirstSYNIncoming() throws Exception {
        server.corruptIncoming(UnreliableIPStack.Type.SYN, 1);
        doNormalConnectionTest();
    }

    public void testCorruptHalfSYNOutgoing() throws Exception {
        client.corruptOutgoing(UnreliableIPStack.Type.SYN, 5);
        doNormalConnectionTest();
    }

    public void testCorruptHalfSYNIncoming() throws Exception {
        server.corruptIncoming(UnreliableIPStack.Type.SYN, 5);
        doNormalConnectionTest();
    }

    public void testCorruptAllSYNOutgoing() throws Exception {
        client.corruptOutgoing(UnreliableIPStack.Type.SYN);
        doCorruptAllSYNTest();
    }

    public void testCorruptAllSYNIncoming() throws Exception {
        server.corruptIncoming(UnreliableIPStack.Type.SYN);
        doCorruptAllSYNTest();
    }

    private void doCorruptAllSYNTest() throws Exception {
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
