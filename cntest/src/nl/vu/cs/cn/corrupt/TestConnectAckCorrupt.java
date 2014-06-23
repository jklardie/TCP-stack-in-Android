package nl.vu.cs.cn.corrupt;

import nl.vu.cs.cn.UnreliableIP;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectAckCorrupt extends TestConnectSegmentCorrupt {

    public void testCorruptFirstACK() throws Exception {
        client.corruptFirstAck(true);

        performTestCorrupt();
    }

    public void testCorruptAllACK() throws Exception {
        client.corruptAllAck(true);

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
