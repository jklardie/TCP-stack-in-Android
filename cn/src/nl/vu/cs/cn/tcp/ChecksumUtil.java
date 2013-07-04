package nl.vu.cs.cn.tcp;

import java.nio.ByteBuffer;

import nl.vu.cs.cn.IP;

public class ChecksumUtil {

    // Size of pseudo header in bytes
    private static final int PSEUDO_HEADER_SIZE = 96 / 8;

    public static short calculateChecksum(ByteBuffer tcpPacketBuffer, IP.IpAddress srcAddr, IP.IpAddress destAddr, int tcpLength) {
        tcpPacketBuffer.flip();
        ByteBuffer pseudoHeader = getPseudoHeader(srcAddr, destAddr, tcpLength);
        ByteBuffer packet = ByteBuffer.allocate(tcpLength + PSEUDO_HEADER_SIZE);

        packet.put(pseudoHeader);
        packet.put(tcpPacketBuffer);

        return getOnesCompliment(packet);
    }

    /**
     * Ones compliment calculation used for tcp checksum.
     * Source: https://code.google.com/p/java-router/
     *
     * @param packet - the tcp packet, including the data and pseudo header
     * @return
     */
    private static short getOnesCompliment(ByteBuffer packet) {
        packet.flip();
        int sum = 0;
        boolean isOdd = false;
        if (packet != null) {
            int pos = packet.position();

            while (packet.remaining() > 1) {
                if (isOdd) {
                    sum += (packet.get() & 0xFF);
                    isOdd = false;
                } else {
                    sum += (packet.getShort() & 0xFFFF);
                }
            }
            if (packet.remaining() == 1) {
                if (isOdd) {
                    sum += (packet.get() & 0xFF);
                    isOdd = false;
                } else {
                    sum += ((packet.get() & 0xFF) << 8);
                    isOdd = true;
                }
            }
            packet.position(pos);
        }

        while ((sum >> 16) > 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }

        return (short)(~sum);
    }

    /**
     * Create a pseudo header that looks as follows:
     *
     * +--------+--------+--------+--------+
     * |           Source Address          |
     * +--------+--------+--------+--------+
     * |         Destination Address       |
     * +--------+--------+--------+--------+
     * |  zero  |  PTCL  |    TCP Length   |
     * +--------+--------+--------+--------+
     *
     * It is used to calculate the checksum in the tcp packet, and ensures that
     * misrouted segments can be detected. When calculating the checksum,
     * this pseudo header is prefixed to the tcp header.
     *
     * @param srcAddr - 32 bit source ip address
     * @param destAddr - 32 bit destination ip address
     * @param tcpLength - length of tcp header + data, in bytes
     *
     * @return
     */
    private static ByteBuffer getPseudoHeader(IP.IpAddress srcAddr, IP.IpAddress destAddr, int tcpLength) {
        ByteBuffer bb = ByteBuffer.allocate(PSEUDO_HEADER_SIZE);
        bb.putInt(srcAddr.getAddress());
        bb.putInt(destAddr.getAddress());

        // add 1 byte of zero's
        bb.put((byte) 0);

        // protocol (TCP)
        bb.put((byte) IP.TCP_PROTOCOL);

        // set combined TCP header and data length in bytes
        bb.putShort((short) tcpLength);

        return bb;
    }

}