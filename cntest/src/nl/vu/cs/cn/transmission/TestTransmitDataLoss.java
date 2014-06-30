package nl.vu.cs.cn.transmission;


import nl.vu.cs.cn.UnreliableIPStack;

import java.util.Arrays;

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

    public void testDropAllDataOutgoing() throws Exception {
        client.dropOutgoing(UnreliableIPStack.Type.DATA);
        startServer(new ServerRunnable());

        connect();

        byte[] buf = data[0];
        int bytesSent = clientSocket.write(buf, 0, buf.length);
        assertTrue("Expected no data to be sent", bytesSent == 0 || bytesSent == -1);

        clientSocket.close();
    }

    public void testDropAllDataIncoming() throws Exception {
        server.dropIncoming(UnreliableIPStack.Type.DATA);
        startServer(new ServerRunnable());

        connect();

        byte[] buf = data[0];
        int bytesSent = clientSocket.write(buf, 0, buf.length);
        assertTrue("Expected no data to be sent", bytesSent == 0 || bytesSent == -1);

        clientSocket.close();
    }


    protected class ServerRunnable implements Runnable {

        @Override
        public void run() {
            serverSocket.accept();

            byte[] buf;
            int bytesRead, buffSize;
            int bytesExpected = -1;
            for(int i=0; i<data.length; i++){
                // data could be larger than max packet size, so receive until we received all
                buffSize = data[i].length;
                buf = new byte[buffSize];
                bytesRead = 0;
                do {
                    bytesRead += serverSocket.read(buf, bytesRead, buffSize-bytesRead);
                } while (bytesRead < buffSize);

                assertEquals(bytesExpected, bytesRead);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            serverSocket.close();
        }
    }
}
