package nl.vu.cs.cn.close;


import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

/**
 * Test the normal flow off closing a connection
 */
public class TestClose extends TestBase {

    private int clientSleep;
    private int serverSleep;

    public void testClientFirstClose() throws Exception {
        // server sleeps 2 seconds before closing, to make sure client closes first
        // client sleeps 0,5 seconds before closing, to make sure server reads ESTABLISHED state
        clientSleep = 500;
        serverSleep = 2000;

        doNormalCloseTest();
    }

    public void testServerFirstClose() throws Exception {
        // client sleeps 2 seconds before closing, to make sure server closes first
        // server sleeps 0,5 seconds before closing, to make sure client reads ESTABLISHED state
        clientSleep = 2000;
        serverSleep = 500;

        doNormalCloseTest();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        clientSleep = 0;
        serverSleep = 0;
    }

    public void doNormalCloseTest() throws Exception {
        startServer(new ServerRunnable());
        connect();

        try {
            Thread.sleep(clientSleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // start close procedure
        boolean closed = clientSocket.close();

        // test client part
        assertTrue("Expected clientSocket.close() to return true", closed);

        assertEquals("Client should be in CLOSED state",
                TransmissionControlBlock.State.CLOSED,
                getClientState());
    }

    protected class ServerRunnable implements Runnable {

        @Override
        public void run() {
            serverSocket.accept();

            try {
                Thread.sleep(serverSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // start close procedure
            boolean closed = serverSocket.close();

            assertTrue("Expected serverSocket.close() to return true", closed);

            assertEquals("Server should be in CLOSED state",
                    TransmissionControlBlock.State.CLOSED,
                    getServerState());
        }
    }

}
