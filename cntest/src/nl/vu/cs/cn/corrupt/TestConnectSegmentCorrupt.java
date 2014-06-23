package nl.vu.cs.cn.corrupt;

public class TestConnectSegmentCorrupt extends TestConnect {

    @Override
    public void testConnect() throws Exception {
        // don't test normal connect here
    }

    public void performTestCorrupt() throws Exception {
        startServer(new ServerRunnable());

        connect();
    }

}
