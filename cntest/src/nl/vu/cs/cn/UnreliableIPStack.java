package nl.vu.cs.cn;

import android.util.Log;
import nl.vu.cs.cn.tcp.TransmissionControlBlock;
import nl.vu.cs.cn.tcp.segment.Segment;

import java.io.IOException;
import java.util.*;

/**
 * This class ...
 *
 * @author Jeffrey Klardie
 */
public class UnreliableIPStack extends IP {

    private static final String TAG = "UnreliableIP";

    public enum What {
        INCOMING, OUTGOING
    }

    public enum Type {
        SYN, SYNACK, ACK, FIN, DATA, ALL
    };

    public enum Action {
        DROP, CORRUPT;
    }


    private List<StackSetting> stackSettings = Collections.synchronizedList(new ArrayList<StackSetting>());
    private int delayMs;

    UnreliableIPStack(int address) throws IOException {
        super(address);
    }

    public static UnreliableIPStack address(int address) throws IOException {
        return new UnreliableIPStack(address);
    }

    public void reset(){
        stackSettings.clear();
    }

    public UnreliableIPStack dropIncoming(Type type, int... num){
        synchronized (stackSettings) {
            stackSettings.add(new StackSetting(type, Action.DROP, What.INCOMING, num));
        }
        return this;
    }

    public UnreliableIPStack corruptIncoming(Type type, int... num){
        stackSettings.add(new StackSetting(type, Action.CORRUPT, What.INCOMING, num));
        return this;
    }

    public UnreliableIPStack dropOutgoing(Type type, int... num){
        stackSettings.add(new StackSetting(type, Action.DROP, What.OUTGOING, num));
        return this;
    }

    public UnreliableIPStack corruptOutgoing(Type type, int... num){
        stackSettings.add(new StackSetting(type, Action.CORRUPT, What.OUTGOING, num));
        return this;
    }

    public UnreliableIPStack delay(int delayMs){
        this.delayMs = delayMs;
        return this;
    }

    @Override
    public int ip_send(Packet p) throws IOException {
        byte[] dataClone = getDataClone(p);
        Segment segment = new Segment(dataClone, p.source, p.destination);
        Type packetType = getType(segment);

        Random rand = new Random();

        synchronized (stackSettings) {
            Iterator iterator = stackSettings.iterator();
            while(iterator.hasNext()){
                StackSetting setting = (StackSetting) iterator.next();

                if (setting.num == 0) {
                    // remove setting when all have been applied
                    iterator.remove();
                } else if (setting.what == What.OUTGOING && (setting.type == Type.ALL || setting.type == packetType)) {
                    // stack setting applies to this packet (write), so use it
                    setting.num -= 1;

                    switch (setting.action) {
                        case DROP:
                            Log.w(TAG, "Dropping outgoing " + setting.type + " segment. SEQ: " + segment.getSeq());
                            return -1;
                        case CORRUPT:
                            Log.w(TAG, "Corrupting outgoing " + setting.type + " segment. SEQ: " + segment.getSeq());

                            // note, we don't want to corrupt the header, only the data (otherwise the packet might not arrive)
                            byte[] corruptData = new byte[10];
                            rand.nextBytes(corruptData);

                            int dataOffset = Segment.HEADER_SIZE / 2;
                            System.arraycopy(corruptData, 0, p.data, dataOffset, corruptData.length);
                            break;
                    }
                }
            }
        }

        if(delayMs > 0){
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return super.ip_send(p);
    }

    @Override
    public void ip_receive(Packet p) throws IOException {
        try {
            ip_receive_timeout(p, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void ip_receive_timeout(Packet p, int timeout) throws IOException, InterruptedException {

        super.ip_receive_timeout(p, timeout);

        byte[] dataClone = getDataClone(p);
        Segment segment = new Segment(dataClone, p.source, p.destination);
        Type packetType = getType(segment);

        Random rand = new Random();

        for(StackSetting setting : stackSettings){
            if(setting.num == 0){
                // remove setting when all have been applied
                stackSettings.remove(setting);
            } else if(setting.what == What.INCOMING && (setting.type == Type.ALL || setting.type == packetType)){
                // stack setting applies to this packet (write), so use it
                setting.num -= 1;

                switch(setting.action){
                    case DROP:
                        Log.w(TAG, "Dropping incoming " + setting.type + " segment. SEQ: " + segment.getSeq());
                        p.data = null;
                        break;
                    case CORRUPT:
                        Log.w(TAG, "Corrupting incoming " + setting.type + " segment. SEQ: " + segment.getSeq());

                        // note, we don't want to corrupt the header, only the data (otherwise the packet might not arrive)
                        byte[] corruptData = new byte[10];
                        rand.nextBytes(corruptData);

                        int dataOffset = Segment.HEADER_SIZE / 2;
                        System.arraycopy(corruptData, 0, p.data, dataOffset, corruptData.length);
                        break;
                }
            }
        }

    }

    private byte[] getDataClone(Packet p) {
        // important: data must be cloned, because the bytebuffer in TCPPacket
        // wraps the byte[]. Changing the bytebuffer changes the byte[].
        byte[] dataClone = new byte[p.data.length];
        System.arraycopy(p.data, 0, dataClone, 0, p.data.length);

        return dataClone;
    }

    private Type getType(Segment s) {
        if(s.isSyn() && s.isAck()) return Type.SYNACK;
        if(s.isSyn()) return Type.SYN;
        if(s.isFin()) return Type.FIN;
        if(s.getLen() > 0) return Type.DATA;

        return Type.ACK;
    }

    private class StackSetting {
        Type type;
        Action action;
        What what;
        int num;

        private StackSetting(Type type, Action action, What what, int... num) {
            this.type = type;
            this.action = action;
            this.what = what;

            if(num.length > 0) {
                this.num = num[0];
            } else {
                this.num = TransmissionControlBlock.MAX_RETRANSMITS + 1;    // mess up all packets, none will arrive correctly
            }
        }

    }

}
