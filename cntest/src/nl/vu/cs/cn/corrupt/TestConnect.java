package nl.vu.cs.cn.corrupt;


import nl.vu.cs.cn.TestBase;

public class TestConnect extends TestBase {

    public void testConnect() throws Exception {
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
