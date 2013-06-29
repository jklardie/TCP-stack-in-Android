package nl.vu.cs.cn.interfaces;

import junit.framework.TestCase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import nl.vu.cs.cn.TCP;

/**
 * Check if the Socket implementation conforms to the provided
 * interfaces and does not expose additional public methods.
 *
 * Allowed methods:
 *  - public boolean connect(IpAddress dst, int port)
 *  - public void accept()
 *  - public int read(byte[] buf, int offset, int maxlen)
 *  - public int write(byte[] buf, int offset, int len)
 *  - public boolean close()
 */
public class TestTCPSocketPublicMethods extends TestCase {

    private static final int NUM_PUBLIC_METHODS = 5;
    private static final int NUM_CONSTRUCTORS = 0;

    private Method[] declaredMethods;
    private Constructor[] declaredConstructors;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        declaredMethods = TCP.Socket.class.getDeclaredMethods();
        declaredConstructors = TCP.Socket.class.getDeclaredConstructors();
    }

    /**
     * Test that number of public methods equals 5
     */
    public void testNumPublicMethods(){
        int numPublicMethods = 0;
        for(Method method : declaredMethods){
            if(Modifier.isPublic(method.getModifiers())){
                numPublicMethods++;
            }
        }

        assertEquals("Socket class should expose exactly 5 public methods",
                NUM_PUBLIC_METHODS, numPublicMethods);
    }


    /**
     * Test that number of constructors equals 0
     */
    public void testNumConstructors(){
        int numPublicConstructors = 0;
        for(Constructor constructor : declaredConstructors){
            if(Modifier.isPublic(constructor.getModifiers())){
                numPublicConstructors++;
            }
        }

        assertEquals("Socket class should expose no public constructor",
                NUM_CONSTRUCTORS, numPublicConstructors);
    }

    /**
     * Test the public methods on name, parameters and return type
     */
    public void testPublicMethods(){
        for(Method method : declaredMethods){
            if(!Modifier.isPublic(method.getModifiers())){
                continue;
            }

            Class<?>[] paramTypes = method.getParameterTypes();

            if("connect".equals(method.getName())){
                assertEquals("Connect() should have 2 params IpAddress, int", 2, paramTypes.length);
                assertEquals("IpAddress", paramTypes[0].getSimpleName());
                assertEquals("int", paramTypes[1].getName());
                assertEquals("boolean", method.getReturnType().getName());
            } else if("accept".equals(method.getName())){
                assertEquals("Accept() should have 0 params", 0, paramTypes.length);
                assertEquals("void", method.getReturnType().getName());
            } else if("read".equals(method.getName()) || "write".equals(method.getName())){
                // read() and write() share the same return type and parameters
                assertEquals(method.getName() + " should have 3 params byte[], int, int", 3, paramTypes.length);
                assertEquals("byte[]", paramTypes[0].getSimpleName());
                assertEquals("int", paramTypes[1].getName());
                assertEquals("int", paramTypes[1].getName());
                assertEquals("int", method.getReturnType().getName());
            } else if("close".equals(method.getName())){
                assertEquals("Close() should have 0 params", 0, paramTypes.length);
                assertEquals("boolean", method.getReturnType().getName());
            } else {
                fail("Unexpected exposed method: " + method);

            }

        }
    }

}