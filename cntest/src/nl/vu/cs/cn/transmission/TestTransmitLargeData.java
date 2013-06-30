package nl.vu.cs.cn.transmission;


import java.io.UnsupportedEncodingException;
import java.util.Random;

import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestTransmitLargeData extends TestTransmit {

    public TestTransmitLargeData(){
        super();

        // create new byte[] that is too large for a single packet
        Random rand = new Random();
        data = new byte[1][];

        data[0] = new byte[TransmissionControlBlock.MAX_SEGMENT_SIZE * 4];
        rand.nextBytes(data[0]);
    }

    // test of TestTransmit will be ran automatically
}
