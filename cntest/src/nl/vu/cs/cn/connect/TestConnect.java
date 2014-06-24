package nl.vu.cs.cn.connect;


import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnect extends TestBase {

    protected class ServerRunnable implements Runnable {

        @Override
        public void run() {
            serverSocket.accept();
        }
    }
}
