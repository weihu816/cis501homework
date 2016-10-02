package cis501.submission;
import cis501.IBranchTargetBuffer;

public class BranchTargetBuffer implements IBranchTargetBuffer {

    private int indexBitM;
    private BTBEntry[] bTBTable; // use a hashtable as BTBTable to avoid initializing empty entry

    public BranchTargetBuffer(int indexBits) {
        if(indexBits > 32) {
            System.out.print("BranchTargetBuffer: Invalid indexBits");
            System.exit(1);
        }
        this.indexBitM = (1 << indexBits) - 1;
        this.bTBTable = new BTBEntry[1<<indexBits];
        // All BTB entries are initialized with zero for their tag and target value
        for (int i = 0; i < bTBTable.length; i++) {
            bTBTable[i] = new BTBEntry();
        }
    }

    @Override
    public long predict(long pc) {
        BTBEntry entry = bTBTable[index(pc)];
        if(entry.getTag() == pc) { return entry.getTarget(); }
        return 0;
    }

    @Override
    public void train(long pc, long actual) {
        int indexed = index(pc);
        // System.out.format("%d%n indexed: %d%n", pc, indexed);
        bTBTable[indexed].tag = pc;
        bTBTable[indexed].target = actual;
    }

    public int index(long pc) {
        return (int) pc & this.indexBitM;
    }

    /**
     * The BTBEntry inside the BTB table
     */
    private class BTBEntry {
        // initialized with "empty" BTB entry: zero for their tag and target value
        long tag;
        long target;

        BTBEntry() {
            this.tag = 0;
            this.target = 0;
        }

        BTBEntry(long tag, long target) {
            this.tag = tag;
            this.target = target;
        }

        public long getTag() { return tag;}
        public long getTarget() { return target; }
    }
}
