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

public class MXWXWMtest {
    private static IInorderPipeline pipeline;

    private static Insn makeInsn(int dst, int src1, int src2, MemoryOp mop) {
        return new Insn(dst, src1, src2,
                1, 4,
                null, 0, null,
                mop, 1, 1,
                "synthetic");
    }

    @Before
    public void setup() {
        pipeline = new InorderPipeline(0, Bypass.FULL_BYPASS);
    }
    
    @Test
    public void testEmptyInput() {
        List<Insn> insns = new LinkedList<>();
        pipeline.run(insns);
        int expectedInsn = 0;
        int expectedCycle = 0;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
    
    @Test
    public void testNoHazard() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, null));
        insns.add(makeInsn(3, 2, 2, null));
        insns.add(makeInsn(-1, 2, 1, MemoryOp.Store));
        insns.add(makeInsn(1, 2, -1, MemoryOp.Load));
        pipeline.run(insns);
        int expectedInsn = 4;
        int expectedCycle = 9;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
    
    @Test
    public void testMX() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, 3, null));
        insns.add(makeInsn(2, 1, 4, null));
        insns.add(makeInsn(-1, 2, 3, MemoryOp.Store));
        pipeline.run(insns);
        int expectedInsn = 3;
        int expectedCycle = 8;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
    
    @Test
    public void testWM() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, -1, MemoryOp.Load));
        insns.add(makeInsn(-1, 1, 3, MemoryOp.Store));
        insns.add(makeInsn(2, 4, 3, null));
        insns.add(makeInsn(-1, 2, 3, MemoryOp.Store));
        pipeline.run(insns);
        int expectedInsn = 4;
        int expectedCycle = 9;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
    
    @Test
    public void testWX() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, 3, null));
        insns.add(makeInsn(5, 7, -1, MemoryOp.Load));
        insns.add(makeInsn(2, 1, 4, null));
        pipeline.run(insns);
        int expectedInsn = 3;
        int expectedCycle = 8;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
    
    @Test
    public void test1WX1MXOnDifferentReg() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, 3, null));
        insns.add(makeInsn(3, 4, 5, null));
        insns.add(makeInsn(2, 1, 3, null));
        pipeline.run(insns);
        int expectedInsn = 3;
        int expectedCycle = 8;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
    
    @Test
    public void test1WX1MXOnSameReg() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, 3, null));
        insns.add(makeInsn(1, 4, 5, null));
        insns.add(makeInsn(3, 1, 2, null));
        pipeline.run(insns);
        int expectedInsn = 3;
        int expectedCycle = 8;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
    
    @Test
    public void test1WX1WM() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, -1, MemoryOp.Load));
        insns.add(makeInsn(-1, 1, 3, MemoryOp.Store));
        insns.add(makeInsn(3, 2, 1, null));
        pipeline.run(insns);
        int expectedInsn = 3;
        int expectedCycle = 8;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
    
    @Test
    public void testLoadStall() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 2, -1, MemoryOp.Load));
        insns.add(makeInsn(4, 2, 3, null));
        insns.add(makeInsn(2, 1, 3, null));
        pipeline.run(insns);
        int expectedInsn = 3;
        int expectedCycle = 9;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
    
    @Test
    public void testMXStoreAfterLoad() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(2, 1, 3, null));
        insns.add(makeInsn(3, 2, -1, MemoryOp.Load));
        insns.add(makeInsn(-1, 1, 3, MemoryOp.Store));
        pipeline.run(insns);
        int expectedInsn = 3;
        int expectedCycle = 9;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
    
    @Test
    public void testNoNeedWX() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, -1, MemoryOp.Load));
        insns.add(makeInsn(2, 2, 2, null));
        insns.add(makeInsn(-1, 1, 3, MemoryOp.Store));
        pipeline.run(insns);
        int expectedInsn = 3;
        int expectedCycle = 8;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
    
    @Test
    public void testSpecialHazard() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, -1, MemoryOp.Load));
        insns.add(makeInsn(-1, 1, 1, MemoryOp.Store));
        pipeline.run(insns);
        int expectedInsn = 2;
        int expectedCycle = 8;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
    
    @Test
    public void test2MX1WX() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, 3, null));
        insns.add(makeInsn(3, 2, 1, null));
        insns.add(makeInsn(2, 1, 3, null));
        pipeline.run(insns);
        int expectedInsn = 3;
        int expectedCycle = 8;
        assertEquals(expectedInsn, pipeline.getInsns());
        assertEquals(expectedCycle, pipeline.getCycles());
    }
}
