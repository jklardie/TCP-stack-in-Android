package nl.vu.cs.cn.connect;

public class TestConnectSegmentLoss extends TestConnect {

    public void testConnect() throws Exception {
        performTestSuccess();
    }

    public void performTestSuccess() throws Exception {
        startServer(new ServerRunnable());
        connect();
    }



}
