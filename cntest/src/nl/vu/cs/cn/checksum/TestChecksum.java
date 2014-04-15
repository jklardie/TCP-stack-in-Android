package nl.vu.cs.cn.checksum;

import android.util.Log;
import junit.framework.TestCase;
import nl.vu.cs.cn.IP;
import nl.vu.cs.cn.IPUtil;
import nl.vu.cs.cn.tcp.ChecksumUtil;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;
import nl.vu.cs.cn.tcp.segment.Segment;
import nl.vu.cs.cn.tcp.segment.SegmentUtil;

import java.nio.ByteBuffer;

/**
 * This class ...
 *
 * @author Jeffrey Klardie
 */
public class TestChecksum extends TestCase {

    public void testChecksum() throws Exception {
//        // TODO implement
        IP ip = new IP(2);
        TransmissionControlBlock tcb = new TransmissionControlBlock(ip, false);
        IP.IpAddress srcAddr = IP.IpAddress.getAddress("192.168.0.2");
        IP.IpAddress destAddr = IP.IpAddress.getAddress("192.168.0.1");

        tcb.setForeignSocketInfo(destAddr, (short)2048);
        tcb.setLocalSocketInfo(srcAddr, (short)3110);

        Segment synSegment = SegmentUtil.getSYNPacket(tcb, 1048261844);

        byte[] packet = synSegment.encode();
        String packetStr = "";
        for(byte b : packet){
            packetStr += String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
        }

        Log.v("Packet", packetStr);

        short expectedChecksum = (short) 37246;

        assertEquals("Calculated checksum is different from expected checksum.", expectedChecksum, synSegment.getChecksum());

    }
}
