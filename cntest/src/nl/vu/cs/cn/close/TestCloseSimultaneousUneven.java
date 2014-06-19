package nl.vu.cs.cn.close;


import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

import java.util.concurrent.CountDownLatch;

public class TestCloseSimultaneousUneven extends TestBase {

    private CountDownLatch synLatch;
    private CountDownLatch closeLatch;

    public void testClose() throws Exception {
        synLatch = new CountDownLatch(2);
        closeLatch = new CountDownLatch(2);

        startServer(new ServerRunnable());

        connect();

        // synchronize client and server so they both finish connect at the same time
        synLatch.countDown();

        // set ip packet latency to 1s so we can perform the simultaneous close
        client.setIPSendLatency(1000);
        server.setIPSendLatency(1000);

        // synchronize client and server so they both close at the same time
        Thread.sleep(1000);
        closeLatch.countDown();

        // start close procedure
        boolean closed = clientSocket.close();

        // give both threads to finish TIME WAIT timer
        Thread.sleep(TransmissionControlBlock.TIME_WAIT_TIMEOUT_SEC*1000 + 1000);

        // test client part
        assertTrue("Expected clientSocket.close() to return true", closed);
        assertEquals("Client should be in CLOSED state",
                TransmissionControlBlock.State.CLOSED,
                getClientState());

        // test server part
        assertEquals("Server should be in CLOSED state",
                TransmissionControlBlock.State.CLOSED,
                getServerState());
    }

    protected class ServerRunnable implements Runnable {

        @Override
        public void run() {
            serverSocket.accept();

            // synchronize client and server so they both finish connect at the same time
            synLatch.countDown();

            // set ip packet latency to 1s so we can perform the simultaneous close
            client.setIPSendLatency(1000);
            server.setIPSendLatency(1000);

            // synchronize client and server so they both close at the same time
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            closeLatch.countDown();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            serverSocket.close();
        }
    }
}
