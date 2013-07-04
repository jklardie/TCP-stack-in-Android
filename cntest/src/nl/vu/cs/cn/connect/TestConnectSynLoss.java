package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.UnreliableIP;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectSynLoss extends TestConnectSegmentLoss {

    public void testDropFirstSYN() throws Exception {
        client.dropSYN(UnreliableIP.DropType.FIRST);

        performTestDropFirst();
    }

    public void testDropAllSYN() throws Exception {
        client.dropSYN(UnreliableIP.DropType.ALL);

        startServer(new ServerRunnable());

        boolean connected = clientSocket.connect(SERVER_IP_ADDR, SERVER_PORT);

        assertFalse("Expected clientSocket.connect() to return false", connected);
        assertEquals(TransmissionControlBlock.State.CLOSED,
                getClientState());

        // wait for server
        Thread.sleep(1000);

        assertEquals("Server never received SYN, so should still be SYN_RECEIVED",
                TransmissionControlBlock.State.LISTEN,
                getServerState());
    }
}
