package nl.vu.cs.cn.connect;


import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnect extends TestBase {

    public void testConnect() throws Exception {
        startServer(new ServerRunnable());

        boolean connected = clientSocket.connect(SERVER_IP_ADDR, SERVER_PORT);

        assertTrue("Expected clientSocket.connect() to return true", connected);
        assertEquals("After connect() client should be in establised state",
                TransmissionControlBlock.State.ESTABLISHED,
                getClientState());

        Thread.sleep(2000);

        assertEquals("Server should never return before reaching the ESTABLISHED state",
                TransmissionControlBlock.State.ESTABLISHED,
                getServerState());
    }

    protected class ServerRunnable implements Runnable {

        @Override
        public void run() {
            serverSocket.accept();
        }
    }
}
