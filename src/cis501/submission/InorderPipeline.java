package cis501.submission;


import cis501.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Note: Stages are declared in "reverse" order to simplify iterating over them in reverse order,
 * as the simulator does.
 */
enum Stage {
    WRITEBACK(4), MEMORY(3), EXECUTE(2), DECODE(1), FETCH(0);

    public static final int NUM_STAGES = 5;
    private final int index;

    private Stage(int idx) {
        this.index = idx;
    }

    /** Returns the index of this stage within the pipeline */
    public int i() {
        return index;
    }

    public boolean toLeave() {
        return index == NUM_STAGES - 1;
    }
}

public class InorderPipeline implements IInorderPipeline {

    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
    /* Five stages herer: F D X M W */
    private Insn[] latches = new Insn[Stage.NUM_STAGES];
    /* Pipeline Parameters */
    private int additionalMemLatency = 0, currentMemTimer = 0;
    private Set<Bypass> bypasses;
    /* Running Statics */
    private int insnCounter = 0;
    private int cycleCounter = 0;

    /**
     * Create a new pipeline with the given additional memory latency.
     *
     * @param additionalMemLatency The number of extra cycles mem insns require in the M stage. If
     *                             0, mem insns require just 1 cycle in the M stage, like all other
     *                             insns. If x, mem insns require 1+x cycles in the M stage.
     * @param bypasses             Which bypasses should be modeled. For example, if this is an
     *                             empty set, then your pipeline should model no bypassing, using
     *                             stalling to resolve all data hazards.
     */
    public InorderPipeline(int additionalMemLatency, Set<Bypass> bypasses) {
        this.additionalMemLatency = additionalMemLatency;
        this.bypasses = new HashSet<>(bypasses);
    }

    @Override
    public String[] groupMembers() {
        return new String[]{"Wei Hu", "Dongni Wang"};
    }

    @Override
    public void run(Iterable<Insn> ii) {
        Iterator<Insn> insnIterator = ii.iterator();
        while (insnIterator.hasNext() || !isEmpty()) {
//            print();
            advance(insnIterator);
            cycleCounter++;
        }
    }

    @Override
    public long getInsns() {
        return insnCounter;
    }

    @Override
    public long getCycles() {
        return cycleCounter;
    }

    // ----------------------------------------------------------------------
    // Pipeline Stage Latches Operations
    // ----------------------------------------------------------------------
    public Insn getInsn(Stage stage) {
        return latches[stage.i()];
    }

    public void clear(Stage stage) {
        latches[stage.i()] = null;
    }

    public boolean isEmpty() {
        for (Insn insn : latches)
            if (insn != null) return false;
        return true;
    }

    private void advance(Stage stage) {
        if (!stage.toLeave()) {
            latches[stage.i()+1] = latches[stage.i()];
        }
        clear(stage);
    }

    private void fetchInsn(Insn insn) {
        latches[Stage.FETCH.i()] = insn;
        insnCounter++;
    }

    /**
     * Pipeline Logic
     * @param iterator
     */
    public void advance(Iterator<Insn> iterator) {
        // Current stages snapshot
        Insn wi = getInsn(Stage.WRITEBACK);
        Insn mi = getInsn(Stage.MEMORY);
        Insn xi = getInsn(Stage.EXECUTE);
        Insn di = getInsn(Stage.DECODE);
        Insn fi = getInsn(Stage.FETCH);
        /* ---------- WRITEBACK ---------- */
        advance(Stage.WRITEBACK);

        /* ----------  MEMORY   ---------- */
        // TODO: other condition
        if (!checkMemDelay(mi)) {
            currentMemTimer++;
            return;
        }
        advance(Stage.MEMORY);
        currentMemTimer = 0;

        /* ----------  EXECUTE  ---------- */
        if (getInsn(Stage.MEMORY) != null) { return; }
        advance(Stage.EXECUTE);

        /* ----------   DECODE  ---------- */
        // Stall on Load-To-Use Dependence
        if (stallOnLoadToUseDependence(di, xi) || getInsn(Stage.EXECUTE) != null) { return; }
        advance(Stage.DECODE);

        /* ----------   FETCH   ---------- */
        if (getInsn(Stage.DECODE) != null) { return; }
        advance(Stage.FETCH);
        if (iterator.hasNext()) { fetchInsn(iterator.next()); }
        else { clear(Stage.FETCH); }
    }

    public void print() {
        Insn wi = getInsn(Stage.WRITEBACK);
        Insn mi = getInsn(Stage.MEMORY);
        Insn xi = getInsn(Stage.EXECUTE);
        Insn di = getInsn(Stage.DECODE);
        Insn fi = getInsn(Stage.FETCH);
        System.out.println("--------------------------------------------------");
        System.out.println(wi == null ? "/" : wi.toString());
        System.out.println(mi == null ? "/" :mi.toString());
        System.out.println(xi == null ? "/" :xi.toString());
        System.out.println(di == null ? "/" :di.toString());
        System.out.println(fi == null ? "/" :fi.toString());
    }

    private boolean stallOnLoadToUseDependence(Insn di, Insn xi) {
        if (di == null || xi == null) return false;
        return (xi.mem == MemoryOp.Load) && ( (di.srcReg2 == xi.dstReg) ||
                ((di.srcReg1 == xi.dstReg) && (di.mem != MemoryOp.Store)));
    }

    private boolean checkMemDelay(Insn mi) {
        if (mi == null || mi.mem == null) return true;
        return currentMemTimer >= additionalMemLatency;
    }
}
