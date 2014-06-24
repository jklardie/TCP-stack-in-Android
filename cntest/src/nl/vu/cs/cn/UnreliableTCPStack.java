package nl.vu.cs.cn;

import java.io.IOException;

/**
 * TCP stack that uses an unreliable ip stack capable of dropping/corrupted specific packets.
 */
public class UnreliableTCPStack extends TCP {

    private final UnreliableIPStack unreliableIPStack;

    public UnreliableTCPStack(int address) throws IOException {
        super(address);
        ip = new UnreliableIPStack(address);
        unreliableIPStack = (UnreliableIPStack) ip;
    }

    public UnreliableTCPStack dropIncoming(UnreliableIPStack.Type type, int... num){
        unreliableIPStack.dropIncoming(type, num);
        return this;
    }

    public UnreliableTCPStack corruptIncoming(UnreliableIPStack.Type type, int... num){
        unreliableIPStack.corruptIncoming(type, num);
        return this;
    }

    public UnreliableTCPStack dropOutgoing(UnreliableIPStack.Type type, int... num){
        unreliableIPStack.dropOutgoing(type, num);
        return this;
    }

    public UnreliableTCPStack corruptOutgoing(UnreliableIPStack.Type type, int... num){
        unreliableIPStack.corruptOutgoing(type, num);
        return this;
    }

    public UnreliableTCPStack delay(int delayMs){
        unreliableIPStack.delay(delayMs);
        return this;
    }

    public void reset(){
        unreliableIPStack.reset();
    }
}
