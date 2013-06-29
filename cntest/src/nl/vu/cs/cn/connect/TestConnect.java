package nl.vu.cs.cn.connect;


import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnect extends TestBase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testConnect() throws Exception {
        // start server, to listen for incomming connects
        new Thread(new ServerRunnable()).start();

        boolean connected = clientSocket.connect(SERVER_IP_ADDR, SERVER_PORT);
        assertTrue("Expected clientSocket.connect() to return true", connected);
        assertEquals("After connect() client should be in establised state",
                TransmissionControlBlock.State.ESTABLISHED,
                getClientState());
    }

    private class ServerRunnable implements Runnable {

        @Override
        public void run() {
            serverSocket.accept();
            assertEquals("After accept() server should be in establised state",
                    TransmissionControlBlock.State.ESTABLISHED,
                    getServerState());
        }
    }
}
