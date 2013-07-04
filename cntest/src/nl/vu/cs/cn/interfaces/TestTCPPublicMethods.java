package nl.vu.cs.cn.interfaces;

import junit.framework.TestCase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import nl.vu.cs.cn.TCP;

/**
 * Check if the TCP implementation conforms to the provided
 * interfaces and does not expose additional public methods.
 *
 * Allowed methods:
 *  - TCP(int addr)
 *  - public Socket socket()
 *  - public Socket socket(int port)
 */
public class TestTCPPublicMethods extends TestCase {

    private static final int NUM_PUBLIC_METHODS = 2;
    private static final int NUM_CONSTRUCTORS = 1;

    private Method[] declaredMethods;
    private Constructor[] declaredConstructors;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        declaredMethods = TCP.class.getDeclaredMethods();
        declaredConstructors = TCP.class.getDeclaredConstructors();
    }

    /**
     * Test that number of public methods equals 2
     */
    public void testNumPublicMethods(){
        int numPublicMethods = 0;
        for(Method method : declaredMethods){
            if(Modifier.isPublic(method.getModifiers())){
                numPublicMethods++;
            }
        }

        assertEquals("TCP class should expose exactly 2 public methods",
                NUM_PUBLIC_METHODS, numPublicMethods);
    }


    /**
     * Test that number of constructors equals 1
     */
    public void testNumConstructors(){
        assertEquals("TCP class should expose exactly 1 constructor",
                NUM_CONSTRUCTORS, TCP.class.getDeclaredConstructors().length);
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

            if("socket".equals(method.getName())){
                if(paramTypes.length == 1){
                    // socket() with one parameter should have type int
                    assertEquals("int", paramTypes[0].getName());
                } else {
                    // either one or zero parameters for socket()
                    assertEquals(0, paramTypes.length);
                }

                // return type of both socket() methods should be Socket
                assertEquals("Socket", method.getReturnType().getSimpleName());

            } else {
                fail("Unexpected exposed method: " + method);

            }

        }
    }

    /**
     * Test constructor on name and parameter
     */
    public void testConstructor(){
        Constructor constructor = declaredConstructors[0];
        Class<?>[] paramTypes = constructor.getParameterTypes();

        if("nl.vu.cs.cn.TCP".equals(constructor.getName())){
            // method TCP (constructor) has one parameter of type int
            assertEquals(1, paramTypes.length);
            assertEquals("int", paramTypes[0].getName());

        } else {
            fail("Unexpected exposed constructor: " + constructor);
        }
    }

}