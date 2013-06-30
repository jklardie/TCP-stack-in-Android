package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectSegmentLoss extends TestConnect {

    @Override
    public void testConnect() throws Exception {
        // don't test normal connect here
    }

    public void performTestDropFirst() throws Exception {
        startServer(new ServerRunnable());

        boolean connected = clientSocket.connect(SERVER_IP_ADDR, SERVER_PORT);

        assertTrue("Expected clientSocket.connect() to return true", connected);
        assertEquals(TransmissionControlBlock.State.ESTABLISHED,
                getClientState());

        // wait for server
        Thread.sleep(2000);

        assertEquals("Three-way handshake succeeded. Server should be in ESTABLISHED state",
                TransmissionControlBlock.State.ESTABLISHED,
                getServerState());
    }

}
