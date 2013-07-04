package nl.vu.cs.cn.close;


import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.UnreliableIP;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestCloseFinSendLoss extends TestBase {

    public void testFinLoseFirst() throws Exception {
        client.dropFIN(UnreliableIP.DropType.FIRST);

        startServer(new ServerRunnable());

        connect();

        // start close procedure
        boolean closed = clientSocket.close();

        // test client part
        assertTrue("Expected clientSocket.close() to return true", closed);
        assertEquals("Client should be in CLOSED state",
                TransmissionControlBlock.State.CLOSED,
                getClientState());

        // Give client to retry send
        Thread.sleep(10000);

        // test server part
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
