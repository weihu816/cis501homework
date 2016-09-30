package cis501.submission;


import cis501.*;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

/**
 * Note: Stages are declared in "reverse" order to simplify iterating over them in reverse order,
 * as the simulator does.
 */
enum Stage {
    WRITEBACK(4), MEMORY(3), EXECUTE(2), DECODE(1), FETCH(0);

        public static final int NUM_STAGES = 5;
        private static Stage[] vals = values();
        private final int index;

        Stage(int idx) {
            this.index = idx;
        }

    /** Returns the index of this stage within the pipeline */
    public int i() {
        return index;
    }

    public boolean toLeave() {
        return index == NUM_STAGES - 1;
    }

    /** Returns the next stage in the pipeline, e.g., next after Fetch is Decode */
    public Stage next() {
        return vals[(this.ordinal() - 1 + vals.length) % vals.length];
    }
}

public class InorderPipeline implements IInorderPipeline {

    // ----------------------------------------------------------------------
    /* Five stages herer: F D X M W */
    private Insn[] latches = new Insn[Stage.NUM_STAGES];
    /* Pipeline Parameters */
    private int additionalMemLatency = 0, currentMemTimer = 0;
    private Set<Bypass> bypasses;
    /* Branch Predictor Parameters */
    private BranchPredictor branchPredictor;
    private Hashtable<Long, Insn> pcInsnRecorder = new Hashtable<>(); // allows jump-back loop-up
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

    /**
     * Create a new pipeline with the additional memory latency and branch predictor. The pipeline
     * should model full bypassing (MX, Wx, WM).
     *
     * @param additionalMemLatency see InorderPipeline(int, Set<Bypass>)
     * @param branchPredictor                   the branch predictor to use
     */
    public InorderPipeline(int additionalMemLatency, BranchPredictor branchPredictor) {
        this.additionalMemLatency = additionalMemLatency;
        this.bypasses = new HashSet<>(Bypass.FULL_BYPASS);
        this.branchPredictor = branchPredictor;
    }

    @Override
    public String[] groupMembers() {
        return new String[]{"Wei Hu", "Dongni Wang"};
    }

    @Override
    public void run(Iterable<Insn> ii) {
        Iterator<Insn> insnIterator = ii.iterator();
        // change the routine so that the first fetch is free: no prediction
        cycleCounter++;
        if (insnIterator.hasNext()) {
            Insn tmp = insnIterator.next();
            fetchInsn(tmp);
            // add to the pcInsnRecorder
            pcInsnRecorder.put(tmp.pc, tmp);
        }
        // end of change
        while (insnIterator.hasNext() || !isEmpty()) {
            advance(insnIterator);
            cycleCounter++;
            // print(cycleCounter);
        }
        // System.out.println(count +  " !!!");
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
    private Insn getInsn(Stage stage) {
        return latches[stage.i()];
    }

    private void clear(Stage stage) {
        latches[stage.i()] = null;
    }

    private boolean isEmpty() {
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
    }

    /**
     * Pipeline Logic
     * @param iterator
     */
    private void advance(Iterator<Insn> iterator) {
        // Current stages snapshot
        final Insn insn_M = getInsn(Stage.MEMORY);
        final Insn insn_X = getInsn(Stage.EXECUTE);
        final Insn insn_D = getInsn(Stage.DECODE);

        /* ---------- WRITEBACK ---------- */
        advance(Stage.WRITEBACK);


        /* ----------  MEMORY   ---------- */
        if (!checkMemDelay(insn_M)) { return; }
        if (getInsn(Stage.WRITEBACK) != null) { return; }
        currentMemTimer = 0;
        /* ------------------------------- */
        advance(Stage.MEMORY);


        /* ----------  EXECUTE  ---------- */
        if (getInsn(Stage.MEMORY) != null) { return; }
        /* ------------------------------- */
        // at the end of EXECUTE, check if the current insn is branch
        long nextPC_X = -1; // if -1, no action; else, need to check the decode stage insn TODO: check -1
        if (insn_X != null) { //  ececute has an insn
            insnCounter++;
            if(insn_X.branch == Direction.Taken) { // is a branch and is taken
                nextPC_X = insn_X.branchTarget;
            } else { // is not a branch or is not taken
                nextPC_X = insn_X.fallthroughPC();
            }
        }
        /* ------------------------------- */
        advance(Stage.EXECUTE);


        /* ----------   DECODE  ---------- */
        if (getInsn(Stage.EXECUTE) != null || stallOnD(insn_D, insn_X, insn_M)) { return; }
        /* ------------------------------- */
        // check the decode stage insn with nextPC_E
        if (nextPC_X > 0 && (insn_D == null || nextPC_X != insn_D.pc))  { // if we made wrong branch
            // waste 2 cycles: do not advance DECODE/FETCH, only over-write FETCH (re-fetch) and clean DECODE
            clear(Stage.DECODE);
            fetchInsn(getNextInsntoFetch(nextPC_X, iterator));
            return;
        }
        // at the end of DECODE, check if the current insn is branch
        long unbranchNextPC_D = -1;
        if ( insn_D!= null && insn_D.branch == null) {
            unbranchNextPC_D = insn_D.fallthroughPC();
        }
        /* ------------------------------- */
        advance(Stage.DECODE);


        /* ----------   FETCH   ---------- */
        if (getInsn(Stage.DECODE) != null) { return; }
        /* ------------------------------- */
        // branch prediction starting at second fetch (first handled already):
        Insn lastFetchedInsn = getInsn(Stage.FETCH); //
        //System.out.println(" DEBUG: FETCH" + lastFetchedInsn.pc);
        if (lastFetchedInsn == null) { return;} // nothing in FETCH stage: no action
        if (unbranchNextPC_D > 0 && lastFetchedInsn.pc != unbranchNextPC_D) { // should not branch but did branch
            // waste a cycle: do not advance FETCH, only over-write (re-fetch)
            fetchInsn(getNextInsntoFetch(unbranchNextPC_D, iterator));
        } else { // went to the right place (no-branch) or unknown yet (branch)
            long predictedPCtoFetch = branchPredictor.predict(lastFetchedInsn.pc, lastFetchedInsn.fallthroughPC());
            advance(Stage.FETCH);
            fetchInsn(getNextInsntoFetch(predictedPCtoFetch, iterator));
        }
    }

    private Insn getNextInsntoFetch(long nextPCtoFecth, Iterator<Insn> iterator) {
        // check if it is a jump-back
        Insn nextInsntoFecth = pcInsnRecorder.get(nextPCtoFecth);
        // add jumped and next pc into recorder if not a jump-back branch
        while (nextInsntoFecth == null && iterator.hasNext()) {
            Insn tmp = iterator.next();
            pcInsnRecorder.put(tmp.pc, tmp);
            if (tmp.pc == nextPCtoFecth) {
                nextInsntoFecth = tmp;
            }
        }
        return nextInsntoFecth;
    }

    public void print(int i) {
        Insn wi = getInsn(Stage.WRITEBACK);
        Insn mi = getInsn(Stage.MEMORY);
        Insn xi = getInsn(Stage.EXECUTE);
        Insn di = getInsn(Stage.DECODE);
        Insn fi = getInsn(Stage.FETCH);
        System.out.println("-------------------------------------------------- " + i);
        System.out.println(wi == null ? "/" : wi.mem + " " + wi.toString());
        System.out.println(mi == null ? "/" : mi.mem + " " + mi.toString());
        System.out.println(xi == null ? "/" : xi.mem + " " + xi.toString());
        System.out.println(di == null ? "/" : di.mem + " " + di.toString());
        System.out.println(fi == null ? "/" : fi.mem + " " + fi.toString());
    }

    private boolean checkMemDelay(Insn mi) {
        if (mi == null || mi.mem == null) return true;
        boolean result =  currentMemTimer >= additionalMemLatency;
        if (!result) { currentMemTimer++; }
        return result;
    }

    private boolean stallOnD(Insn di, Insn xi, Insn mi) {
        if (di == null) return false;
        if (stallOnLoadToUseDependence(di, xi)) {
            return true;
        }
        if (xi != null) {
            if (xi.mem != null) { // X is Load/Store
                if (di.mem != null) { // D is Mem
                    // Store => Load
                    if (di.mem == MemoryOp.Store && xi.mem == MemoryOp.Load) {
                        if ((di.srcReg2 != -1) && di.srcReg2 == xi.dstReg) return true;
                        if ((di.srcReg1 != -1) && di.srcReg1 == xi.dstReg && !bypasses.contains(Bypass.WM)) return true;
                    } else if ((di.srcReg1 != -1) && di.mem == MemoryOp.Load && xi.mem == MemoryOp.Load && di.srcReg1 == xi.dstReg) return true;
                }
            } else { // X is ADD
                if ((xi.dstReg != -1) && (di.srcReg1 == xi.dstReg || di.srcReg2 == xi.dstReg)) {
                    if (di.mem != null) { // D is Load/Store
                        if (di.mem == MemoryOp.Load) {
                            if (di.srcReg1 == xi.dstReg && !bypasses.contains(Bypass.MX)) return true;
                        }
                        if (di.mem == MemoryOp.Store) {
                            if (di.srcReg2 == xi.dstReg && !bypasses.contains(Bypass.MX)) return true;
                            if (di.srcReg1 == xi.dstReg && !bypasses.contains(Bypass.MX) && !bypasses.contains(Bypass.WM)) return true;
                        }
                    } else { // D is ADD
                        if (!bypasses.contains(Bypass.MX)) return true;
                    }
                }
            }
        }

        if (mi != null) {
            if (mi.mem != null) { // X is Load/Store
                if (di.mem != null) { // D is MEM
                    // Store => Load
                    if (di.mem == MemoryOp.Store && mi.mem == MemoryOp.Load) {
                        if ((di.srcReg2 != -1) && di.srcReg2 == mi.dstReg && !bypasses.contains(Bypass.WX)) return true;
                    } else if ((di.srcReg1 != -1) && di.mem == MemoryOp.Load && mi.mem == MemoryOp.Load && di.srcReg1 == mi.dstReg) {
                        if (!bypasses.contains(Bypass.WX)) return true;
                    }
                }  else { // D is ADD
                    if (mi.mem == MemoryOp.Load && !bypasses.contains(Bypass.WX)) return true;
                }
            } else {
                // X is ADD
                if ((mi.dstReg != -1) && (di.srcReg1 == mi.dstReg || di.srcReg2 == mi.dstReg)) {
                    if (di.mem != null) { // D is Load/Store
                        if (di.mem == MemoryOp.Load) {
                            if (di.srcReg1 == mi.dstReg && !bypasses.contains(Bypass.WX)) return true;
                        }
                        if (di.mem == MemoryOp.Store) {
                            if ((di.srcReg2 == mi.dstReg) && !bypasses.contains(Bypass.WX)) return true;
                        }
                    } else { // D is ADD
                        if (!bypasses.contains(Bypass.WX)) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean stallOnLoadToUseDependence(Insn di, Insn xi) {
        if (di == null || xi == null) return false;
        return (xi.mem == MemoryOp.Load) && ( (di.srcReg2 != -1) && (di.srcReg2 == xi.dstReg) ||
                ((di.srcReg1 != -1) && (di.srcReg1 == xi.dstReg) && (di.mem != MemoryOp.Store)));
    }
}
