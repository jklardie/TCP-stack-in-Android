package nl.vu.cs.cn.checksum;


import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;
import nl.vu.cs.cn.transmission.TestTransmit;

public class TestChecksum extends TestTransmit {

    protected static final String[] MESSAGES = {
            "This is the first message",
            "Αυτό είναι το καλύτερο, προκλητική και πιο ευχάριστη Φυσικά ποτέ ακολούθησαν."};

    protected byte[][] data;

    public TestChecksum(){
        data = new byte[MESSAGES.length][];
        int i = 0;
        for(String msg : MESSAGES){
            try {
                data[i] = msg.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                data[i] = msg.getBytes();
            }
            i++;
        }
    }

    public void testReadWrite() throws Exception {
        client.corruptFirstDataPacket(true);

        startServer(new ServerRunnable());

        connect();

        // first message should not be acked, and should return -1
        int bytesSent = clientSocket.write(data[0], 0, data[0].length);
        assertEquals("Expected all data to be sent", -1, bytesSent);

        // second message should be sent normally
        bytesSent = clientSocket.write(data[1], 0, data[1].length);
        assertEquals("Expected all data to be sent", data[1].length, bytesSent);

        // start close procedure
        boolean closed = clientSocket.close();

        // test client part
        assertTrue("Expected clientSocket.close() to return true", closed);
        assertEquals("Client should be in CLOSED state",
                TransmissionControlBlock.State.CLOSED,
                getClientState());

        // test server part
        assertEquals("Server should be in CLOSED state",
                TransmissionControlBlock.State.CLOSED,
                getServerState());
    }

    protected class ServerRunnable implements Runnable {

        @Override
        public void run() {
            serverSocket.accept();

            // the client will send two messages. The first one will be corrupted, and eventually dropped
            // the server should only receive the last (second) message.
            byte[] buf;
            int bytesExpected = data[1].length;
            buf = new byte[bytesExpected];
            int bytesRead = serverSocket.read(buf, 0, data[1].length);

            assertEquals(bytesExpected, bytesRead);
            assertTrue("Expected to receive exact same data", Arrays.equals(data[1], buf));

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            serverSocket.close();
        }
    }
}
