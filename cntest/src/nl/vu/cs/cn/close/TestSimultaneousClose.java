package nl.vu.cs.cn.close;

import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

import java.util.concurrent.CountDownLatch;

/**
 * Created by jeffrey on 6/25/14.
 */
public class TestSimultaneousClose extends TestBase {

    private CountDownLatch closeLatch;

    public void testClose() throws Exception {
        closeLatch = new CountDownLatch(2);

        startServer(new ServerRunnable());

        connect();

        closeLatch.countDown();

        // start close procedure
        boolean closed = clientSocket.close();

        // test client part
        assertTrue("Expected clientSocket.close() to return true", closed);

        // test server part
        assertEquals("Server should be in CLOSED state",
                TransmissionControlBlock.State.CLOSED,
                getServerState());
    }

    protected class ServerRunnable implements Runnable {

        @Override
        public void run() {
            serverSocket.accept();

            closeLatch.countDown();

            // start close procedure
            boolean closed = serverSocket.close();

            // test client part
            assertTrue("Expected serverSocket.close() to return true", closed);

            // test server part
            assertEquals("Server should be in CLOSED state",
                    TransmissionControlBlock.State.CLOSED,
                    getServerState());
        }
    }

}
