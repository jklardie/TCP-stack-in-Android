package nl.vu.cs.cn.corrupt;

import nl.vu.cs.cn.UnreliableIP;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectSynCorrupt extends TestConnectSegmentCorrupt {

    public void testCorruptFirstSYN() throws Exception {
        client.corruptFirstSyn(true);

        performTestCorrupt();
    }

    public void testCorruptAllSYN() throws Exception {
        client.corruptAllSyn(true);

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
