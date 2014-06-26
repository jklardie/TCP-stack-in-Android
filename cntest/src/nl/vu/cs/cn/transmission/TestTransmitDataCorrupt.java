package nl.vu.cs.cn.transmission;


import nl.vu.cs.cn.UnreliableIPStack;

/**
 * This class ...
 *
 * @author Jeffrey Klardie
 */
public class TestTransmitDataCorrupt extends TestTransmitBase {

    public void testCorruptFirstDataOutgoing() throws Exception {
        client.corruptOutgoing(UnreliableIPStack.Type.DATA, 1);
        doNormalTransmissionTest();
    }

    public void testCorruptFirstDataIncoming() throws Exception {
        server.corruptIncoming(UnreliableIPStack.Type.DATA, 1);
        doNormalTransmissionTest();
    }

    public void testCorruptHalfDataOutgoing() throws Exception {
        client.corruptOutgoing(UnreliableIPStack.Type.DATA, 5);
        doNormalTransmissionTest();
    }

    public void testCorruptHalfDataIncoming() throws Exception {
        server.corruptIncoming(UnreliableIPStack.Type.DATA, 5);
        doNormalTransmissionTest();
    }

    public void testCorruptAllDataOutgoing() throws Exception {
        client.corruptOutgoing(UnreliableIPStack.Type.DATA);
        startServer(new ServerRunnable());

        connect();

        for(byte[] buf : data){
            int bytesSent = clientSocket.write(buf, 0, buf.length);
            assertEquals("Expected no data to be sent", 0, bytesSent);
        }

        clientSocket.close();
    }

    public void testCorruptAllDataIncoming() throws Exception {
        server.corruptIncoming(UnreliableIPStack.Type.DATA);
        startServer(new ServerRunnable());

        connect();

        for(byte[] buf : data){
            int bytesSent = clientSocket.write(buf, 0, buf.length);
            assertEquals("Expected no data to be sent", 0, bytesSent);
        }

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
