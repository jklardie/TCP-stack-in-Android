package nl.vu.cs.cn;

import junit.framework.TestCase;

import nl.vu.cs.cn.tcp.segment.SegmentUtil;

public class TestComparison extends TestCase {

    public void testComparisonLow() throws Exception {
        assertTrue(SegmentUtil.inWindow(0, 0, 1));
        assertTrue(SegmentUtil.inWindow(0, 1, 2));
        assertTrue(SegmentUtil.inWindow(0, 5, 10));
        assertFalse(SegmentUtil.inWindow(0, 0, 0));
        assertFalse(SegmentUtil.inWindow(0, 1, 0));
        assertFalse(SegmentUtil.inWindow(0, 1, 1));
        assertTrue(SegmentUtil.inWindow(10, 10, 11111));
        assertTrue(SegmentUtil.inWindow(10, 111151, 21222121));
    }

    public void testComparisonHigh() throws Exception {
        int max = Integer.MAX_VALUE;
        assertTrue(SegmentUtil.inWindow(max-100, max-100, max));
        assertTrue(SegmentUtil.inWindow(max-100, max-99, max));
        assertFalse(SegmentUtil.inWindow(max-100, max, max));
        assertFalse(SegmentUtil.inWindow(max-100, max-200, max));
    }

    public void testComparisonOverlap() throws Exception {
        int max = Integer.MAX_VALUE;

        // seq num has wrapped from max to 10.
        assertTrue(SegmentUtil.inWindow(max, 6, 10));
        assertTrue(SegmentUtil.inWindow(max, max, 10));
        assertTrue(SegmentUtil.inWindow(max, 9, 10));
        assertFalse(SegmentUtil.inWindow(max, 10, 10));
        assertFalse(SegmentUtil.inWindow(max, 11, 10));
        assertFalse(SegmentUtil.inWindow(max, max-1, 10));
    }
}
