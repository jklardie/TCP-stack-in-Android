package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.UnreliableIP;

public class TestConnectSynLoss extends TestConnect {

    @Override
    public void testConnect() throws Exception {
        // don't test normal connect here
    }

    public void testDropFirstSYN() throws Exception {
        client.dropACK(UnreliableIP.DropType.NONE);
        client.dropSYNACK(UnreliableIP.DropType.NONE);
        client.dropSYN(UnreliableIP.DropType.FIRST);
        super.testConnect();
    }
}
