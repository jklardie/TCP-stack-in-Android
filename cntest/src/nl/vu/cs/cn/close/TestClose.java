package nl.vu.cs.cn.close;


import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestClose extends TestBase {

    public void testClose() throws Exception {
        startServer(new ServerRunnable());

        // make sure we are connected (both client and server)
        boolean connected = clientSocket.connect(SERVER_IP_ADDR, SERVER_PORT);

        assertTrue("Expected clientSocket.connect() to return true", connected);
        assertEquals("After connect() client should be in the ESTABLISHED state",
                TransmissionControlBlock.State.ESTABLISHED,
                getClientState());

        Thread.sleep(1000);

        assertEquals("Server should never return before reaching the ESTABLISHED state",
                TransmissionControlBlock.State.ESTABLISHED,
                getServerState());

        // start close procedure
        boolean closed = clientSocket.close();

        // TODO: client will probably never change to state closed yet.

        // wait so server also has time to call close
        Thread.sleep(5000);

        assertEquals("Server should be in CLOSED state",
                TransmissionControlBlock.State.CLOSED,
                getServerState());
    }

    protected class ServerRunnable implements Runnable {

        @Override
        public void run() {
            serverSocket.accept();

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            serverSocket.close();
        }
    }
}
