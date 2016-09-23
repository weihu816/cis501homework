package cis501.submission;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import cis501.CondCodes;
import cis501.ITraceAnalyzer;
import cis501.Insn;
import cis501.MemoryOp;

public class TraceAnalyzer implements ITraceAnalyzer {

    private final String IC_LOAD = "load";
    private final String IC_STORE = "store";
    private final String IC_UNCONDITIONALBRANCH = "unconditionalbranch";
    private final String IC_CONDITIONALBRANCH = "conditionalbranch";
    private final String IC_OTHER = "other";

    private long numInsn = 0;
    private long totalInsnSizeBytes = 0;
    private long insnBandwidthIncrease = 0;
    private Map<String, AtomicLong> insnCategoryCount = new HashMap<>();
    private Map<Integer, AtomicLong> branchOffsetEncodeCount = new HashMap<>();

    public TraceAnalyzer() {
        insnCategoryCount.put(IC_LOAD, new AtomicLong(0));
        insnCategoryCount.put(IC_STORE, new AtomicLong(0));
        insnCategoryCount.put(IC_UNCONDITIONALBRANCH, new AtomicLong(0));
        insnCategoryCount.put(IC_CONDITIONALBRANCH, new AtomicLong(0));
        insnCategoryCount.put(IC_OTHER, new AtomicLong(0));
    }

    @Override
    public String author() {
        return "Wei Hu";
    }

    @Override
    public void run(Iterable<Insn> iiter) {
        for (Insn insn : iiter) {
            numInsn++;
            totalInsnSizeBytes += insn.insnSizeBytes;
            if (insn.insnSizeBytes != 4) {
                insnBandwidthIncrease += (4 - insn.insnSizeBytes);
            }

            if (insn.mem != null && insn.mem == MemoryOp.Load) {
                insnCategoryCount.get(IC_LOAD).incrementAndGet();
            } else if (insn.mem != null && insn.mem == MemoryOp.Store) {
                insnCategoryCount.get(IC_STORE).incrementAndGet();
            } else if (insn.branch != null && (insn.condCode == null || insn.condCode == CondCodes.WriteCC)) {
                insnCategoryCount.get(IC_UNCONDITIONALBRANCH).incrementAndGet();
            } else if (insn.branch != null && (insn.condCode == CondCodes.ReadCC || insn.condCode == CondCodes.ReadWriteCC)) {
                insnCategoryCount.get(IC_CONDITIONALBRANCH).incrementAndGet();
            } else {
                insnCategoryCount.get(IC_OTHER).incrementAndGet();
            }

            if (insn.branch != null) {
                double result = 2 + Math.floor(Math.log(Math.abs(insn.pc - insn.branchTarget))/Math.log(2));
                int numEncodeOffset = new Double(result).intValue();
                if (!branchOffsetEncodeCount.containsKey(numEncodeOffset)) branchOffsetEncodeCount.put(numEncodeOffset, new AtomicLong());
                branchOffsetEncodeCount.get(numEncodeOffset).incrementAndGet();
            }
        }
    }

    @Override
    public double avgInsnSize() {
        return (double) totalInsnSizeBytes / numInsn;
    }

    @Override
    public double insnBandwidthIncreaseWithoutThumb() {
        return (double) (insnBandwidthIncrease + totalInsnSizeBytes) / totalInsnSizeBytes;
    }

    @Override
    public String mostCommonInsnCategory() {
        Entry<String, AtomicLong> mostCommonEntry = null;
        for (Entry<String, AtomicLong> entry : insnCategoryCount.entrySet()) {
            if (mostCommonEntry == null || entry.getValue().get() > mostCommonEntry.getValue().get()) {
                mostCommonEntry = entry;
            }
        }
        return mostCommonEntry != null ? mostCommonEntry.getKey() : null;
    }

    @Override
    public double fractionOfBranchOffsetsLteNBits(int bits) {
        double count = 0, total = 0;
        for (Entry<Integer, AtomicLong> entry : branchOffsetEncodeCount.entrySet()) {
            total += entry.getValue().get();
            if (entry.getKey() <= bits) {
                count += entry.getValue().get();
            }
        }
        return count / total;
    }

}