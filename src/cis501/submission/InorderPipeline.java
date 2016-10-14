package cis501.submission;


import cis501.*;

import java.util.*;

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

    private static final boolean DEBUG = false;
    // ----------------------------------------------------------------------
    /* Five stages herer: F D X M W */
    private Insn[] latches = new Insn[Stage.NUM_STAGES];
    /* Pipeline Parameters */
    private int additionalMemLatency = 0, memLatency = 0, currentMemTimer = 0, fetchLatency = 0;
    private Set<Bypass> bypasses;
    /* Branch Predictor Parameters */
    private BranchPredictor branchPredictor;
    /* Running Statics */
    private int insnCounter = 0;
    private int cycleCounter = 0;
    private Map<Insn, StringBuilder> timingTrace = new HashMap<>();
    private Queue<Insn> queue = new LinkedList<>();
    /* Cache */
    private ICache insnCache, dataCache;

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

    /** Cache homework ctor */
    public InorderPipeline(BranchPredictor bp, ICache ic, ICache dc) {
        this.branchPredictor = bp;
        this.insnCache = ic;
        this.dataCache = dc;
        this.bypasses = new HashSet<>(Bypass.FULL_BYPASS);
    }

    @Override
    public String[] groupMembers() {
        return new String[]{"Wei Hu", "Dongni Wang"};
    }

    @Override
    public void run(Iterable<Insn> ii) {
        Iterator<Insn> insnIterator = ii.iterator();
        // the first fetch is free: no prediction
        if (insnIterator.hasNext()) {
            Insn tmp = insnIterator.next();
            fetchInsn(tmp);
        }
        print(cycleCounter);
        cycleCounter++;
        while (insnIterator.hasNext() || !isEmpty()) {
            advance(insnIterator);
            cycleCounter++;
            print(cycleCounter);
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
        if (DEBUG && insn != null) timingTrace.put(insn, new StringBuilder(String.valueOf(cycleCounter)));
        latches[Stage.FETCH.i()] = insn;
        if (insn != null && insnCache != null) {
            fetchLatency = insnCache.access(true, insn.pc);
        }
    }

    /**
     * Pipeline Logic
     * @param iterator
     */
    private void advance(Iterator<Insn> iterator) {
        // Current stages snapshot
        final Insn insn_W = getInsn(Stage.WRITEBACK);
        final Insn insn_M = getInsn(Stage.MEMORY);
        final Insn insn_X = getInsn(Stage.EXECUTE);
        final Insn insn_D = getInsn(Stage.DECODE);
        final Insn insn_F = getInsn(Stage.FETCH);

        /* ---------- WRITEBACK ---------- */

        if (DEBUG && insn_W != null) { cleanAndPrintStageTimes(insn_W); }
        advance(Stage.WRITEBACK);


        /* ----------  MEMORY   ---------- */
        int memDelay = checkMemDelay(insn_M);
        if (memDelay <= 0) {
            currentMemTimer = 0;
            if (getInsn(Stage.WRITEBACK) != null) { throw new IllegalArgumentException(); }
            if (DEBUG && insn_M != null) timingTrace.get(insn_M).append(" " + cycleCounter);
            advance(Stage.MEMORY);
        }

        /* ----------  EXECUTE  ---------- */
        // Only train at the first time
        if (memDelay == additionalMemLatency || memDelay == -1) { train(insn_X); }
        /* ------------------------------- */
        if (getInsn(Stage.MEMORY) == null) {
            if (DEBUG && insn_X != null) timingTrace.get(insn_X).append(" " + cycleCounter);
            advance(Stage.EXECUTE);
        }
        // Also do flush the next two stages if needed (there are fake Insn not null now)
        if (getInsn(Stage.FETCH).isFake) {
            clear(Stage.DECODE);
            clear(Stage.FETCH);
        }


        /* ----------   DECODE  ---------- */
        if (stallOnD(insn_D, insn_X, insn_M, memDelay)) { return; }
        /* ------------------------------- */
        if (getInsn(Stage.EXECUTE) == null) {
            if (DEBUG && insn_D != null) timingTrace.get(insn_D).append(" " + cycleCounter);
            advance(Stage.DECODE);
        }


        /* ----------   FETCH   ---------- */
        if (fetchLatency > 0) {
            fetchLatency--;
            return;
        }
        if (getInsn(Stage.DECODE) == null) {
            if (DEBUG && insn_F != null) timingTrace.get(insn_F).append(" " + cycleCounter);
            advance(Stage.FETCH);
        }
        if (insn_F == null && insn_D == null) {
            fetchCorrect(iterator);
        }else {
            fetch(insn_F, iterator);
        }
    }

    /**
     * Train the predictor at the X stage
     */
    private void train(Insn insn_X) {
        if (insn_X != null) {
            insnCounter++;
            if(insn_X.branch == Direction.Taken) { // is a branch and is taken
                long nextPC_X = insn_X.branchTarget;
                branchPredictor.train(insn_X.pc, nextPC_X, Direction.Taken);
            } else {
                long nextPC_X = insn_X.fallthroughPC();
                if (insn_X.branch!= null) { // train only if it's a branch insn
                    branchPredictor.train(insn_X.pc, nextPC_X, Direction.NotTaken);
                }
            }
        }

    }

    /**
     * fetch the next insn if last fetch was valid (right prediction)
     *  or if unkonwn yet
     * @param insn_F
     * @param iterator
     */
    private void fetch(Insn insn_F, Iterator<Insn> iterator) {
        long predNextPC = 0;
        if (insn_F != null) {
            predNextPC = branchPredictor.predict(insn_F.pc, insn_F.fallthroughPC());
        }
        if (!insn_F.isFake && iterator.hasNext()) { // known last one is not fake, do forward iterator
            Insn nextIns = iterator.next();
            if (predNextPC == nextIns.pc) { // made right prediction
                fetchInsn(nextIns);
                return;
            } else {
                if (DEBUG && insn_F != null) timingTrace.get(insn_F).append(" " + "{bmispred}");
                queue.offer(nextIns);
            }
        }
        fetchInsn(new Insn(predNextPC));
    }

    /**
     * fetch the correct insn after made wrong prediction
     *  and have been penalized
     * @param iterator
     */
    private void fetchCorrect(Iterator<Insn> iterator) {
        Insn nextIns = null;
        if (queue.isEmpty()) {
            if (iterator.hasNext()) { nextIns = iterator.next(); }
        } else {
            nextIns = queue.poll();
        }
        fetchInsn(nextIns);
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

    /**
     * Return 0 if the pipeline is ok to proceed
     * If additionalMemLatency is 3, it will return 3 2 1 0
     */
    private int checkMemDelay(Insn mi) {
        if (mi == null || mi.mem == null) return -1;
        if (currentMemTimer == 0) {
            memLatency = additionalMemLatency;
            if (dataCache != null) memLatency += dataCache.access(mi.mem == MemoryOp.Load, mi.memAddress);
        }
        boolean result = currentMemTimer >= memLatency;
        if (result) return 0;
        int diff = memLatency - currentMemTimer;
        if (!result) { currentMemTimer++; }
        return diff;
    }

    private boolean stallOnD(Insn di, Insn xi, Insn mi, int memDelay) {
        if (di == null) return false;
        if (stallOnLoadToUseDependence(di, xi) || (stallOnLoadToUseDependence(di, mi) && memDelay > 0)) {
            if (DEBUG) timingTrace.get(di).append(" {load-use}");
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
            if (mi.mem != null) { // M is Load/Store
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
                // M is ADD
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

    private void cleanAndPrintStageTimes(Insn insn_W) {
        String temp = timingTrace.get(insn_W).toString();
        StringBuilder sb = new StringBuilder();
        boolean isMisPred = false, isLoadUse = false;
        if (temp.contains(" {bmispred}")) {
            isMisPred = true;
            temp = temp.replace(" {bmispred}", "");
        }
        if (temp.contains(" {load-use}")) {
            isLoadUse = true;
            temp = temp.replace(" {load-use}", "");
        }
        sb.append(temp + " " + insn_W.asm);
        if (isMisPred && isLoadUse) sb.append(" {load-use, bmispred}");
        else if (isMisPred) sb.append(" {bmispred}");
        else if (isLoadUse) sb.append(" {load-use}");
        else sb.append(" {}");
        System.out.println(sb.toString());
        timingTrace.remove(insn_W);
    }
}
