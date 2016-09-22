package cis501.submission;

import cis501.Bypass;
import cis501.IInorderPipeline;
import cis501.Insn;
import cis501.MemoryOp;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class InorderPipelineSampleTest {

    private static IInorderPipeline sim, sim_d, sim_wm, sim_wx, sim_mx, sim_wm_wx;

    private static Insn makeInsn(int dst, int src1, int src2, MemoryOp mop) {
        return new Insn(dst, src1, src2,
                1, 4,
                null, 0, null,
                mop, 1, 1,
                "synthetic");
    }

    @Before
    public void setup() {
        sim = new InorderPipeline(0/*no add'l memory latency*/, Bypass.FULL_BYPASS);
        sim_d = new InorderPipeline(2/*no add'l memory latency*/, Bypass.FULL_BYPASS);
        sim_wm = new InorderPipeline(0/*no add'l memory latency*/, EnumSet.of(Bypass.WM));
        sim_wx = new InorderPipeline(0/*no add'l memory latency*/, EnumSet.of(Bypass.WX));
        sim_mx = new InorderPipeline(0/*no add'l memory latency*/, EnumSet.of(Bypass.MX));
        sim_wm_wx = new InorderPipeline(0/*no add'l memory latency*/, EnumSet.of(Bypass.MX, Bypass.WX));
    }

    @Test
    public void test1Uop() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, null));
        sim.run(insns);
        assertEquals(1, sim.getInsns());
        // 12345678
        // fdxmw|
        assertEquals(6, sim.getCycles());
    }

    @Test
    public void testManyUops() {
        List<Insn> insns = new LinkedList<>();
        final int COUNT = 10;
        for (int i = 0; i < COUNT; i++) {
            insns.add(makeInsn(3, 1, 2, null));
        }
        sim.run(insns);
        assertEquals(COUNT, sim.getInsns());
        assertEquals(5 + COUNT, sim.getCycles());
    }

    @Test
    public void test1MemUop() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        sim.run(insns);
        assertEquals(1, sim.getInsns());
        // 123456789abcdef
        // fdxmw|
        assertEquals(6, sim.getCycles());
    }

    @Test
    public void testManyMemUops() {
        List<Insn> insns = new LinkedList<>();
        final int COUNT = 10;
        for (int i = 0; i < COUNT; i++) {
            insns.add(makeInsn(3, 1, 2, MemoryOp.Store)); // no load-use dependencies
        }
        sim.run(insns);
        assertEquals(COUNT, sim.getInsns());
        assertEquals(5 + COUNT, sim.getCycles());
    }

    @Test
    public void testLoadUse1() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 3, 4, null)); // load to src reg 1
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw  |
        //  fd.xmw|
        assertEquals(6 + 2, sim.getCycles());
    }

    @Test
    public void testLoadUse2() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 4, 3, null)); // load to src reg 2
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw  |
        //  fd.xmw|
        assertEquals(6 + 2, sim.getCycles());
    }

    @Test
    public void testLoadUseStoreAddress() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 4, 3, MemoryOp.Store)); // load to src reg 2 (store address), so we stall
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abc
        // fdxmw  |
        //  fd.xmw|
        final long expected = 6 + 2;
        assertEquals(expected, sim.getCycles());
    }

    @Test
    public void testLoadUseStoreValue() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 3, 4, MemoryOp.Store)); // load to src reg 1 (store value), so no stall
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw |
        //  fdxmw|
        final long expected = 6 + 1;
        assertEquals(expected, sim.getCycles());
    }

    // ------------------------------------------------------------
    // Test with additional memory delay
    // ------------------------------------------------------------
    @Test
    public void test1UopDelay() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        sim_d.run(insns);
        assertEquals(1, sim_d.getInsns());
        // 12345678
        // fdxm  w|
        assertEquals(8, sim_d.getCycles());
    }

    @Test
    public void testManyUopsDelay() {
        List<Insn> insns = new LinkedList<>();
        final int COUNT = 10;
        for (int i = 0; i < COUNT; i++) {
            insns.add(makeInsn(3, 1, 2, MemoryOp.Store));
        }
        sim_d.run(insns);
        assertEquals(COUNT, sim_d.getInsns());
        assertEquals(3 + 3 * COUNT + 2, sim_d.getCycles());
    }

    @Test
    public void test1UopNoDelay() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, null));
        sim_d.run(insns);
        assertEquals(1, sim_d.getInsns());
        // 12345678
        // fdxmw|
        assertEquals(6, sim_d.getCycles());
    }

    @Test
    public void testManyUopsNoDelay() {
        List<Insn> insns = new LinkedList<>();
        final int COUNT = 10;
        for (int i = 0; i < COUNT; i++) {
            insns.add(makeInsn(3, 1, 2, null));
        }
        sim_d.run(insns);
        assertEquals(COUNT, sim_d.getInsns());
        assertEquals(5 + COUNT, sim_d.getCycles());
    }

    // ------------------------------------------------------------
    // WM
    // ------------------------------------------------------------
    @Test
    public void test1UopWM1() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 2, -1, MemoryOp.Load));
        insns.add(makeInsn(4, 3, -1, MemoryOp.Store));
        sim_wm.run(insns);
        assertEquals(2, sim_wm.getInsns());
        // 1234567
        // fdxmw |
        //  fdxmw|
        assertEquals(7, sim_wm.getCycles());
    }

    @Test
    public void test1UopWM2() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 2, -1, MemoryOp.Load));
        insns.add(makeInsn(3, 4, -1, MemoryOp.Store));
        sim_wm.run(insns);
        assertEquals(2, sim_wm.getInsns());
        // 12345678
        // fdxmw  |
        //  fd.xmw|
        assertEquals(8, sim_wm.getCycles());
    }

    // ------------------------------------------------------------
    // MX
    // ------------------------------------------------------------
    @Test
    public void test1UopWX2() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 2, -1, MemoryOp.Load));
        insns.add(makeInsn(3, 4, -1, MemoryOp.Store));
        sim_wm.run(insns);
        assertEquals(2, sim_wm.getInsns());
        // 1234567
        // fdxmw |
        //  fdxmw|
        assertEquals(7, sim_wm.getCycles());
    }

    // ------------------------------------------------------------
    // WM and WX
    // ------------------------------------------------------------
    public void test1UopWMandWXWM() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 2, -1, MemoryOp.Load));
        insns.add(makeInsn(4, 3, -1, MemoryOp.Store));
        sim_wm_wx.run(insns);
        assertEquals(2, sim_wm_wx.getInsns());
        // 12345678
        // fdxmw  |
        //  fd.xmw|
        assertEquals(8, sim_wm_wx.getCycles());
    }
}
