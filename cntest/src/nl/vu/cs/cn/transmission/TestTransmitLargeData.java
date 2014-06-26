package nl.vu.cs.cn.transmission;

import nl.vu.cs.cn.tcp.TransmissionControlBlock;

import java.util.Random;

/**
 * This class ...
 *
 * @author Jeffrey Klardie
 */
public class TestTransmitLargeData extends TestTransmitBase {

    public TestTransmitLargeData(){
        super();

        // create new byte[] that is too large for a single packet
        Random rand = new Random();
        data = new byte[1][];

        data[0] = new byte[TransmissionControlBlock.MAX_SEGMENT_SIZE * 4];
        rand.nextBytes(data[0]);
    }

    public void testTransmit() throws Exception {
        doNormalTransmissionTest();
    }
}

