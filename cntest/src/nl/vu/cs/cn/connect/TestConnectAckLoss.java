package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.UnreliableIP;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectAckLoss extends TestConnectSegmentLoss {

    public void testDropFirstACK() throws Exception {
        client.dropACK(UnreliableIP.DropType.FIRST);

        performTestDropFirst();
    }

    public void testDropAllACK() throws Exception {
        client.dropACK(UnreliableIP.DropType.ALL);

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
