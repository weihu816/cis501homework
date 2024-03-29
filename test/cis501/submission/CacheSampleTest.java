package cis501.submission;

import cis501.*;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CacheSampleTest {

    // TODO: replace the path of trace file here
    private static final String TRACE_FILE = System.getProperty("trace");

    private static final int HIT_LAT = 0;
    private static final int CLEAN_MISS_LAT = 2;
    private static final int DIRTY_MISS_LAT = 3;

    private static final int INDEX_BITS = 3;
    private static final int WAYS = 1;
    private static final int BLOCK_BITS = 2;
    private static final int BLOCK_SIZE = 1 << BLOCK_BITS; // 4B, 1 ARM insn per block

    private ICache cache;
    private IInorderPipeline pipe;

    private static Insn makeInt(int sr1, int sr2, int dr, long pc, int isize) {
        return new Insn(dr, sr1, sr2,
                pc, isize,
                null, 0, null,
                null, 0, 0,
                "fake-alu");
    }

    private static Insn makeBr(long pc, Direction dir, int isize, long targetPC) {
        return new Insn(-1, 0, 0,
                pc, isize,
                dir, targetPC, null,
                null, 0, 0,
                "fake-branch");
    }

    // cache tests

    private static Insn makeMem(int src1, int src2, int dst, long pc, int isize, MemoryOp mop, long dataAddr) {
        return new Insn(dst, src1, src2,
                pc, isize,
                null, 0, null,
                mop, dataAddr, 1,
                "fake-mem");
    }

    /** Runs before each test...() method */
    @Before
    public void setup() {
        cache = new Cache(INDEX_BITS, WAYS, BLOCK_BITS, HIT_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT);

        IBranchTargetBuffer btb = new BranchTargetBuffer(3/*index bits*/);
        IDirectionPredictor never = new DirPredNeverTaken();

        // pipeline uses never predictor for simplicity
        pipe = new InorderPipeline(new BranchPredictor(never, btb),
                new Cache(INDEX_BITS, WAYS, BLOCK_BITS, HIT_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT),
                new Cache(INDEX_BITS, WAYS, BLOCK_BITS, HIT_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT));
    }

    @Test
    public void testInitialState() {
        final long addr = 0xFF << (INDEX_BITS + BLOCK_BITS);
        int lat = cache.access(true, addr);
        assertEquals(CLEAN_MISS_LAT, lat);
    }

    @Test
    public void testRemainderIndexing() {
        final long addr = -1;
        int lat = cache.access(true, addr);
        assertEquals(CLEAN_MISS_LAT, lat);
    }

    @Test
    public void testBlockOffset() {
        final long firstByteInBlock = 0xFF << (INDEX_BITS + BLOCK_BITS);
        int lat = cache.access(true, firstByteInBlock);
        assertEquals(CLEAN_MISS_LAT, lat);
        final long lastByteInBlock = firstByteInBlock + (1 << BLOCK_BITS) - 1;
        lat = cache.access(true, lastByteInBlock);
        assertEquals(HIT_LAT, lat);
    }

    @Test
    public void testLRU() {
        final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
        final long waySize = (1 << INDEX_BITS) * (1 << BLOCK_BITS);

        int lat = cache.access(true, a);
        assertEquals(CLEAN_MISS_LAT, lat);

        for (int w = 1; w < WAYS * 2; w++) {
            // a hits
            lat = cache.access(true, a);
            assertEquals(HIT_LAT, lat);

            // conflicting access
            lat = cache.access(true, a + (w * waySize));
            assertEquals(CLEAN_MISS_LAT, lat);
        }
    }

    // pipeline integration tests

    @Test
    public void testFullSetLoads() {
        final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
        final long waySize = (1 << INDEX_BITS) * (1 << BLOCK_BITS);
        for (int w = 0; w < WAYS; w++) {
            int lat = cache.access(true, a + (w * waySize));
            assertEquals(CLEAN_MISS_LAT, lat);
        }

        // a should still be in the cache
        int lat = cache.access(true, a);
        assertEquals(HIT_LAT, lat);
    }

    @Test
    public void testConflictMissLoads() {
        final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
        final long waySize = (1 << INDEX_BITS) * (1 << BLOCK_BITS);
        for (int w = 0; w < WAYS + 1; w++) {
            int lat = cache.access(true, a + (w * waySize));
            assertEquals(CLEAN_MISS_LAT, lat);
        }

        // a should have gotten evicted
        int lat = cache.access(true, a);
        assertEquals(CLEAN_MISS_LAT, lat);
    }

    @Test
    public void testImiss() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInt(1, 2, 3, 0xAA, 4));
        pipe.run(insns);

        assertEquals(1, pipe.getInsns());
        // 123456789a
        // f..dxmw|
        assertEquals(6 + CLEAN_MISS_LAT, pipe.getCycles());
    }

    @Test
    public void test2Imiss() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInt(1, 2, 3, 0x0, 4));
        insns.add(makeInt(1, 2, 3, BLOCK_SIZE, 4));
        pipe.run(insns);

        assertEquals(2, pipe.getInsns());
        // 123456789abcd
        // f..dxmw   |
        //    f..dxmw|
        assertEquals(7 + (2 * CLEAN_MISS_LAT), pipe.getCycles());
    }

    @Test
    public void testImissIhit() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInt(1, 2, 3, 0x0, 2));
        insns.add(makeInt(1, 2, 3, 0x2, 2));
        pipe.run(insns);

        assertEquals(2, pipe.getInsns());
        // 123456789abcd
        // f..dxmw |
        //    fdxmw|
        assertEquals(7 + CLEAN_MISS_LAT, pipe.getCycles());
    }

    @Test
    public void testManyImiss() {
        List<Insn> insns = new LinkedList<>();
        final int numInsns = 10;
        for (int i = 0; i < numInsns; i++) {
            insns.add(makeInt(1, 2, 3, i * BLOCK_SIZE, 4));
        }
        pipe.run(insns);

        assertEquals(numInsns, pipe.getInsns());
        // 123456789abcdef
        // f..dxmw      |
        //    f..dxmw   |
        //       f..dxmw|
        assertEquals(5 + (numInsns * (CLEAN_MISS_LAT + 1)), pipe.getCycles());
    }

    @Test
    public void testImissDmiss() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeMem(1, 2, 3, 0x0, 4, MemoryOp.Load, 0xB));
        pipe.run(insns);

        assertEquals(1, pipe.getInsns());
        // 123456789a
        // f..dxm..w|
        assertEquals(6 + (2 * CLEAN_MISS_LAT), pipe.getCycles());
    }

    @Test
    public void testImissDmissIhitDhit() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Load, 0x42));
        insns.add(makeMem(1, 2, 3, 0x2, 1, MemoryOp.Load, 0x42));
        pipe.run(insns);

        assertEquals(2, pipe.getInsns());
        // 123456789abcd
        // f..dxm..w |
        //    fdx  mw|
        assertEquals(7 + (2 * CLEAN_MISS_LAT), pipe.getCycles());
    }

    @Test
    public void testBranchMispredImiss() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeBr(0x2, Direction.Taken, 2, 0x42));
        insns.add(makeInt(1, 2, 3, 0x42, 2));
        pipe.run(insns);

        assertEquals(2, pipe.getInsns());
        // 123456789abcd
        // f..dxmw     |
        //      f..dxmw|
        assertEquals(7 + (2 * CLEAN_MISS_LAT) + 2/*br mispred*/, pipe.getCycles());
    }

    // More tests
    // private static Insn makeMem(int src1, int src2, int dst, long pc, int isize, MemoryOp mop, long dataAddr) {
    @Test
    public void testFail1() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeMem(1, 2, 3, 0x0, 0, MemoryOp.Load, 0x0));
        insns.add(makeMem(1, 2, 3, 0x0, 0, MemoryOp.Store, 0x0));
        insns.add(makeMem(1, 2, 3, 0x0, 0, MemoryOp.Store, 0x10));
        insns.add(makeMem(1, 2, 3, 0x0, 0, MemoryOp.Store, 0x20));
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Store, 0x0));
        pipe.run(insns);
        assertEquals(5, pipe.getInsns());
        assertEquals(22, pipe.getCycles());
    }

    @Test
    public void testFail2() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Store, 0x0));
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Store, 0x10));
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Store, 0x20));
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Store, 0x0));
        pipe.run(insns);
        assertEquals(4, pipe.getInsns());
        assertEquals(21, pipe.getCycles());
    }

    @Test
    public void testFail3() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeMem(1, 2, 3, 0x0, 0, MemoryOp.Load, 0x0));
        insns.add(makeMem(1, 2, 3, 0x0, 0, MemoryOp.Store, 0x0));
        insns.add(makeMem(1, 2, 3, 0x0, 0, MemoryOp.Store, 0x10));
        insns.add(makeMem(1, 2, 3, 0x0, 0, MemoryOp.Store, 0x20));
        insns.add(makeMem(1, 2, 3, 0x0, 0, MemoryOp.Store, 0x30));
        insns.add(makeMem(1, 2, 3, 0x0, 0, MemoryOp.Store, 0x40));
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Store, 0x0));
        pipe.run(insns);
        assertEquals(7, pipe.getInsns());
        assertEquals(28, pipe.getCycles());
    }

    @Test
    public void testFail4() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Store, 0x0));
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Store, 0x10));
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Store, 0x20));
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Store, 0x30));
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Store, 0x40));
        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Store, 0x0));
        pipe.run(insns);
        assertEquals(6, pipe.getInsns());
        assertEquals(27, pipe.getCycles());
    }

    @Test
    public void test5k() {
        if (TRACE_FILE == null) return;
        final IDirectionPredictor never = new DirPredBimodal(10);
        final IBranchTargetBuffer bigBtb = new BranchTargetBuffer(10);
        final ICache ic = new Cache(10, 2, 2, 0, 2, 3), dc = new Cache(10, 2, 2, 0, 2 ,3);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        IInorderPipeline pl = new InorderPipeline(new BranchPredictor(never, bigBtb), ic, dc);
        pl.run(uiter);
        System.out.println("5000 Gshare \n insn: " + pl.getInsns() + " cycles: " + pl.getCycles());
    }

    @Test
    public void testGraph() {
        if (TRACE_FILE == null) return;
        int[] ways = {1,2,4,8,16};
        int[] sizes = {9,10,11,12,13,14,15,16,17,18};
        for (int size : sizes) {
            for (int i = 0; i < ways.length; i++) {
                int way = ways[i];
                int bit = size-5-i;
                final IDirectionPredictor bp = new DirPredGshare(18, 18);
                final IBranchTargetBuffer bigBtb = new BranchTargetBuffer(18);
                final ICache ic = new Cache(bit, way, 5, 0, 2, 3), dc = new Cache(bit, way, 5, 0, 2 ,3);
                InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
                IInorderPipeline pl = new InorderPipeline(new BranchPredictor(bp, bigBtb), ic, dc);
                pl.run(uiter);
                System.out.println("size: " + size + " ways: " + way + " IPC: " + pl.getInsns()*1.0/pl.getCycles());
            }
        }
    }

}
