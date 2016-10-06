package cis501.submission;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import cis501.Bypass;
import cis501.IInorderPipeline;
import cis501.Insn;
import cis501.MemoryOp;

public class InorderPipelineWMBypass {
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
    	Set<Bypass> set = new HashSet<Bypass>();
    	set.add(Bypass.WM);
        sim = new InorderPipeline(0/*no add'l memory latency*/, set);
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
        assertEquals(COUNT + 5, sim.getCycles());
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
        assertEquals(9, sim.getCycles()); ///CHANGE HERE
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
        assertEquals(9, sim.getCycles());
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
        final long expected = 9;
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
        // fdxmw  |
        //  fdx.mw|
        final long expected = 7;
        assertEquals(expected, sim.getCycles());
    }
    
    @Test
    // 1 <- 3,2
    // 4 <- 1,2
    public void testAddAddValue() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 3, 2, null));
        insns.add(makeInsn(4, 1, 2, null)); // load to src reg 1 (store value), so no stall
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw  |
        //  fd.xmw|
        final long expected = 9;
        assertEquals(expected, sim.getCycles());
    }
    
    @Test
    // add 1 <- 2, 3
    // ld  1 <- 4
    public void testAddLoadValue1() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, 3, null));
        insns.add(makeInsn(1, 4, 2, MemoryOp.Load)); // load to src reg 1 (store value), so no stall
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw  |
        //  fdxmw|
        final long expected = 7;
        assertEquals(expected, sim.getCycles());
    }
    
    @Test
    // add 1 <- 2, 3
    // ld  4 <- 1
    public void testAddLoadValue2() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, 3, null));
        insns.add(makeInsn(4, 1, 2, MemoryOp.Load)); // load to src reg 1 (store value), so no stall
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw  |
        //  fdxmw|
        final long expected = 9;
        assertEquals(expected, sim.getCycles());
    }
    
    @Test
    // add 1 <- 3, 2
    // store 1 <- 4
    public void testAddStoreValue2() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 3, 2, null));
        insns.add(makeInsn(1, 4, 5, MemoryOp.Load)); // load to src reg 1 (store value), so no stall
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw |
        //  fdxmw|
        final long expected = 7;
        assertEquals(expected, sim.getCycles());
    }
    
}