package cis501.submission;

import cis501.IBranchTargetBuffer;

import java.util.Hashtable;

public class BranchTargetBuffer implements IBranchTargetBuffer {
    private int indexBits;
    private Hashtable<Long, BTBEntry> bTBTable; // use a hashtable as BTBTable to avoid initializing empty entry

    public BranchTargetBuffer(int indexBits) {
        if(indexBits > 63) System.out.print("BranchTargetBuffer: Invalid indexBits"); System.exit(1);
        this.indexBits = indexBits;
        this.bTBTable = new Hashtable<>();
    }

    @Override
    public long predict(long pc) {
        BTBEntry entry = bTBTable.get(index(pc));
        if( entry!= null && entry.getTag() == pc) { return entry.getTarget(); }
        return 0;
    }

    @Override
    public void train(long pc, long actual) {
        long index = index(pc);
        bTBTable.put(index, new BTBEntry(pc, actual));
    }

    public long index(long pc) { return pc & (1<<(indexBits+1)-1);}

    /**
     * The BTBEntry inside the BTB table
     */
    private class BTBEntry {
        // initialized with "empty" BTB entry: zero for their tag and target value
        long tag;
        long target;

        BTBEntry(long tag, long target) {
            this.tag = tag;
            this.target = target;
        }

        public long getTag() { return tag;}
        public long getTarget() { return target; }
}
}
