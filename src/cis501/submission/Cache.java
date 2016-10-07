package cis501.submission;

import cis501.ICache;

import java.util.Arrays;

public class Cache implements ICache {

    private int indexBits, ways, blockOffsetBits, hitLatency, cleanMissLatency, dirtyMissLatency;
    private Set[] sets;

    public Cache(int indexBits, int ways, int blockOffsetBits,
                 final int hitLatency, final int cleanMissLatency, final int dirtyMissLatency) {
        assert indexBits >= 0;
        assert ways > 0;
        assert blockOffsetBits >= 0;
        assert indexBits + blockOffsetBits < 64;
        assert hitLatency >= 0;
        assert cleanMissLatency >= 0;
        assert dirtyMissLatency >= 0;

        this.indexBits = indexBits;
        this.ways = ways;
        this.blockOffsetBits = blockOffsetBits;
        this.hitLatency = hitLatency;
        this.cleanMissLatency = cleanMissLatency;
        this.dirtyMissLatency = dirtyMissLatency;

        this.sets = new Set[1 << indexBits];
        Arrays.fill(this.sets, new Set(this.ways));
    }

    @Override
    public int access(boolean load, long address) {
        int index = getIndex(address);
        String tag = getTag(address);
        long offset = getOffset(address);
        Set set = sets[index % sets.length];
        if (set.hit(load, tag, offset)) {
            return hitLatency;
        } else if (set.cleanMiss(load, tag, offset)) {
            return cleanMissLatency;
        } else if (set.dirtyMiss(load, tag, offset)) {
            return dirtyMissLatency;
        }
        return 0;
    }


    private long getOffset(long address) {
        return address & ((1 << blockOffsetBits) - 1);
    }

    private int getIndex(long address) {
        // Assume the index bits will not exceed 32, return integer
        Long index = (address & (((1 << indexBits) - 1) << blockOffsetBits)) >> blockOffsetBits;
        return index.intValue();
    }

    private String getTag(long address) {
        return Long.toHexString(address >>> (indexBits + blockOffsetBits));
    }


    class Set {
        private Line[] lines;
        private int size = 0;

        public Set(int ways) {
            this.lines = new Line[ways];
        }

        public boolean hit(boolean load, String tag, long offset) {
            for (Line line : lines) {
                if (line.valid && line.tag.equals(tag)) {
                    if (!load) line.dirty = true;
                    return true;
                }
            }
            return false;
        }

        // Call cleanMiss after hit
        public boolean cleanMiss(boolean load, String tag, long offset) {

            return false;
        }

        // Call dirtyMiss after cleanMiss
        public boolean dirtyMiss(boolean load, String tag, long offset) {
            return true;
        }

        class Line {
            private String tag;
            private boolean valid = false;
            private boolean dirty = false;
            private int LRUBit = 0;
        }
    }
}
