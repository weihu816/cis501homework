package cis501.submission;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import cis501.Bypass;
import cis501.IInorderPipeline;
import cis501.Insn;
import cis501.MemoryOp;

public class InorderPipelineOneMemoryNoBypass {
	private static IInorderPipeline sim;

    private static Insn makeInsn(int dst, int src1, int src2, MemoryOp mop) {
        return new Insn(dst, src1, src2,
                1, 4,
                null, 0, null,
                mop, 1, 1,
                "synthetic");
    }

    @Before
    public void setup() {
        sim = new InorderPipeline(1, Bypass.NO_BYPASS);
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
        assertEquals(7, sim.getCycles());
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
        assertEquals(25, sim.getCycles());
    }

    @Test
    public void testLoadUse1() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 3, 4, null)); // load to src reg 1
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw   |
        //  fd..xmw|
        assertEquals(10, sim.getCycles());
    }

    @Test
    public void testLoadUse2() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 4, 3, null)); // load to src reg 2
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw   |
        //  fd..xmw|
        assertEquals(10, sim.getCycles());
    }

    @Test
    public void testLoadUseStoreAddress() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 4, 3, MemoryOp.Store)); // load to src reg 2 (store address), so we stall
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abc
        // fdxmw   |
        //  fd..xmw|
        final long expected = 11;
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
        // fdxm.w  |
        //  fd..xmw|
        final long expected = 10;
        assertEquals(expected, sim.getCycles());
    }
}
