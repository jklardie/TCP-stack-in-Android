package nl.vu.cs.cn;

import junit.framework.TestCase;

public class TestComparison extends TestCase {

    /**
     * Return true if and only if x is less (wraparound-safe) than y
     */
    private boolean isLess(long x, long y){
        return (x < y && y - x < Integer.MAX_VALUE) ||
                (x > y && x - y > Integer.MAX_VALUE);
    }

    /**
     * Return true if and only if x is greater (wraparound-safe) than y
     */
    private boolean isGreater(long x, long y){
        return (x < y && y - x > Integer.MAX_VALUE) ||
                (x > y && x - y < Integer.MAX_VALUE);
    }


    public void testComparison() throws Exception {
        assertEquals(true, isLess(1, 2));
        assertEquals(true, isLess(1, Integer.MAX_VALUE-1));
        assertEquals(true, isLess(125, Integer.MAX_VALUE-5));
        assertEquals(false, isLess(1,1));

        assertEquals(true, isGreater(2, 1));
        assertEquals(true, isGreater(Integer.MAX_VALUE - 1, 1));
        assertEquals(true, isGreater(Integer.MAX_VALUE - 5, 123544));
        assertEquals(false, isGreater(1,1));
    }
}
