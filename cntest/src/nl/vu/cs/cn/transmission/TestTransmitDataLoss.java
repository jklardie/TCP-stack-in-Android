package nl.vu.cs.cn.transmission;


import nl.vu.cs.cn.UnreliableIPStack;

/**
 * This class ...
 *
 * @author Jeffrey Klardie
 */
public class TestTransmitDataLoss extends TestTransmitBase {

    public void testDropFirstDataOutgoing() throws Exception {
        client.dropOutgoing(UnreliableIPStack.Type.DATA, 1);
        doNormalTransmissionTest();
    }

    public void testDropFirstDataIncoming() throws Exception {
        server.dropIncoming(UnreliableIPStack.Type.DATA, 1);
        doNormalTransmissionTest();
    }

    public void testDropHalfDataOutgoing() throws Exception {
        client.dropOutgoing(UnreliableIPStack.Type.DATA, 5);
        doNormalTransmissionTest();
    }

    public void testDropHalfDataIncoming() throws Exception {
        server.dropIncoming(UnreliableIPStack.Type.DATA, 5);
        doNormalTransmissionTest();
    }

}
