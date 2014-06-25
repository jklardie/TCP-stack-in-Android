package nl.vu.cs.cn.close;

import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

import java.util.concurrent.CountDownLatch;

/**
 * Created by jeffrey on 6/25/14.
 */
public class TestSimultaneousClose extends TestBase {

    private CountDownLatch synLatch;
    private CountDownLatch closeLatch;

    public void testClose() throws Exception {
        synLatch = new CountDownLatch(2);
        closeLatch = new CountDownLatch(2);

        startServer(new ServerRunnable());

        connect();

        // allow connect to finish for both client/server
        synLatch.countDown();
        Thread.sleep(1000);
        closeLatch.countDown();


        // start close procedure
        boolean closed = clientSocket.close();

        assertTrue("Expected clientSocket.close() to return true", closed);
        assertEquals("Client should be in CLOSED state",
                TransmissionControlBlock.State.CLOSED,
                getClientState());
    }

    protected class ServerRunnable implements Runnable {

        @Override
        public void run() {
            serverSocket.accept();

            synLatch.countDown();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            closeLatch.countDown();

            // start close procedure
            boolean closed = serverSocket.close();

            assertTrue("Expected serverSocket.close() to return true", closed);
            assertEquals("Server should be in CLOSED state",
                    TransmissionControlBlock.State.CLOSED,
                    getServerState());
        }
    }

}
