package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestConnectSegmentLoss extends TestConnect {

    @Override
    public void testConnect() throws Exception {
        // don't test normal connect here
    }

    public void performTestDropFirst() throws Exception {
        startServer(new ServerRunnable());

        connect();
    }

}
