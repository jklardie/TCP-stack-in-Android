package nl.vu.cs.cn.connect;

import nl.vu.cs.cn.UnreliableIP;

public class TestConnectSynLoss extends TestConnectSegmentLoss {

    public void testDropFirstSYN() throws Exception {
        init();
        client.dropSYN(UnreliableIP.DropType.FIRST);

        performTestDropFirst();
    }

    public void testDropAllSYN() throws Exception {
        init();
        client.dropSYN(UnreliableIP.DropType.ALL);

        performTestDropAll();
    }
}
