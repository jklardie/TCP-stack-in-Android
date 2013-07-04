package nl.vu.cs.cn.transmission;


import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

public class TestTransmitMismatchBuffer extends TestTransmit {

    public void testReadWrite() throws Exception {
        startServer(new ServerRunnable());

        connect();

        for(byte[] buf : data){
            int bytesSent = clientSocket.write(buf, 0, buf.length);
            assertEquals("Expected all data to be sent", buf.length, bytesSent);
        }

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

            byte[] buf;
            int bytesRead, bytesExpected;
            for(int i=0; i<data.length; i++){
                // data could be larger than max packet size, so receive until we received all
                bytesExpected = data[i].length;

                // NOTE: 100 is different than the buffer size used in the send part. This should still succeed.
                buf = new byte[100];
                bytesRead = 0;
                do {
                    bytesRead += serverSocket.read(buf, 0, Math.min(bytesExpected-bytesRead, 100));
                } while (bytesRead < bytesExpected);

                assertEquals(bytesExpected, bytesRead);
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            serverSocket.close();
        }
    }
}
