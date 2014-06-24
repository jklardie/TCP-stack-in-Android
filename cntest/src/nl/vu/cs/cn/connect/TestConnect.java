package nl.vu.cs.cn.connect;


import nl.vu.cs.cn.TestBase;

/**
 * Test the normal flow off connecting (3-way handshake)
 */
public class TestConnect extends TestBase {

    public void testConnect() throws Exception {
        doNormalConnectionTest();
    }

    public void doNormalConnectionTest() throws Exception {
        startServer(new ServerRunnable());
        connect();
    }

    protected class ServerRunnable implements Runnable {

        @Override
        public void run() {
            serverSocket.accept();
        }
    }
}
