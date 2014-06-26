package nl.vu.cs.cn.transmission;

import nl.vu.cs.cn.TestBase;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * This class ...
 *
 * @author Jeffrey Klardie
 */
public class TestTransmitBase extends TestBase {

    protected static final String[] MESSAGES = {
            "\u000C\u0026\u0008",
            "This is the first message",
            "This is another message that I'm sending",
            "Ру́сский язы́к относится к восточной группе славянских языков, принадлежащих индоевропейской се" +
                    "мье языков. Он является официальным языком России, Беларуси, Казахстана и Кыргызстана, " +
                    "а также региональным в некоторых регионах Украины. Также является восьмым языком в мире по " +
                    "численности владеющих им как родным и пятым языком в мире по общей численности говорящих[18]. " +
                    "Русский язык — национальный язык русского народа, основной язык международного общения в " +
                    "центральной Евразии, в Восточной Европе, в странах бывшего Советского Союза, один из рабочих " +
                    "языков ООН. Он является наиболее распространённым славянским языком и самым распространённым " +
                    "языком в Европе — географически и по числу носителей языка как родного. Занимает четвёртое место " +
                    "среди самых переводимых языков, а также — седьмое место среди языков, на которые переводится " +
                    "большинство книг.[19] В 2013 году русский язык вышел на второе место среди самых популярных " +
                    "языков Интернета[20].",
            "འགྲོ་བ་མིའི་རིགས་རྒྱུད་ཡོངས་ལ་སྐྱེས་ཙམ་ཉིད་ནས་ཆེ་མཐོངས་དང༌། ཐོབ་ཐངགི་རང་དབང་འདྲ་མཉམ་དུ་ཡོད་ལ། ཁོང་ཚོར་རང་བྱུང་གི་བློ་རྩལ་དང་བསམ་ཚུལ་བཟང་པོ་འདོན་པའི་འོས་བབས་ཀྱང་ཡོད། " +
                    "དེ་བཞིན་ཕན་ཚུན་གཅིག་གིས་གཅིག་ལ་བུ་སྤུན་གྱི་འདུ་ཤེས་འཛིན་པའི་བྱ་སྤྱོད་ཀྱང་ལག་ལེན་བསྟར་དགོས་པ་ཡིན༎"};

    protected byte[][] data;

    public TestTransmitBase(){
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

    public void doNormalTransmissionTest() throws Exception {
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
                buf = new byte[bytesExpected];
                bytesRead = 0;
                do {
                    bytesRead += serverSocket.read(buf, bytesRead, bytesExpected-bytesRead);
                } while (bytesRead < bytesExpected);

                assertEquals(bytesExpected, bytesRead);
                assertTrue("Expected to receive exact same data", Arrays.equals(data[i], buf));

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
