package nl.vu.cs.cn.corrupt;

import nl.vu.cs.cn.UnreliableIP;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectSynAckCorrupt extends TestConnectSegmentCorrupt {

    public void testCorruptFirstSYNACK() throws Exception {
        server.corruptFirstSynAck(true);

        performTestCorrupt();
    }

    public void testCorruptAllSYNACK() throws Exception {
        server.corruptAllSynAck(true);

        startServer(new ServerRunnable());

        boolean connected = clientSocket.connect(SERVER_IP_ADDR, SERVER_PORT);

        assertFalse("Expected clientSocket.connect() to return false", connected);
        assertEquals(TransmissionControlBlock.State.CLOSED,
                getClientState());

        // Both client and server are waiting for each other to ACK packets
        // Wait 1 second, so both have time to set states correctly
        Thread.sleep(1000);

        assertEquals("Server never received ACK, should be reset to LISTEN",
                TransmissionControlBlock.State.LISTEN,
                getServerState());
    }
}
