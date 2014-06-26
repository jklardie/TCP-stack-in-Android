package nl.vu.cs.cn.transmission;

import nl.vu.cs.cn.tcp.TransmissionControlBlock;

import java.util.concurrent.CountDownLatch;

/**
 * This class ...
 *
 * @author Jeffrey Klardie
 */
public class TestTransmit extends TestTransmitBase {


    private CountDownLatch closeLatch;

    public void testReadWrite() throws Exception {
        closeLatch = new CountDownLatch(2);

        startServer(new ServerRunnable());

        connect();

        for(byte[] buf : data){
            int bytesSent = clientSocket.write(buf, 0, buf.length);
            assertEquals("Expected all data to be sent", buf.length, bytesSent);
        }

        clientSocket.close();
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

            serverSocket.close();
        }
    }
}
