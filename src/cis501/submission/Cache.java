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
        assert indexBits + blockOffsetBits < 32;
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
        for (int i = 0; i < sets.length; i++) {
            this.sets[i] = new Set(this.ways);
        }
    }

    @Override
    public int access(boolean load, long address) {
        int offset = getOffset(address);
        int index = getIndex(address);
        String tag = getTag(address);
        Set set = sets[index % sets.length];
        if (set.hit(load, tag, offset)) {
            return hitLatency;
        } else if (set.cleanMiss(load, tag, offset)) {
            return cleanMissLatency;
        } else if (set.dirtyMiss(load, tag, offset)) {
            return dirtyMissLatency;
        }
        throw new IllegalArgumentException();
    }


    private int getOffset(long address) {
        return new Long(address & ((1 << blockOffsetBits) - 1)).intValue();
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
        private int next = 0; // next position

        public Set(int ways) {
            this.lines = new Line[ways];
            for (int i = 0; i < this.lines.length; i++) {
                lines[i] = new Line(i);
            }
        }

        public boolean hit(boolean load, String tag, int offset) {
            for (Line line : lines) {
                if (line.valid && line.tag.equals(tag)) {
                    if (!load) line.dirty = true;
                    updateLRU(line);
                    return true;
                }
            }
            return false;
        }

        // Call cleanMiss after hit
        public boolean cleanMiss(boolean load, String tag, int offset) {
            Line replaceLine = lines[next];
            if (replaceLine.dirty) return false;
            replace(replaceLine, load, tag);
            return true;
        }

        // Call dirtyMiss after cleanMiss
        public boolean dirtyMiss(boolean load, String tag, int offset) {
            Line replaceLine = lines[next];
            // Writeback to the memory ......
            replace(replaceLine, load, tag);
            return true;
        }

        private void replace(Line replaceLine, boolean load, String tag) {
            int prevLRU = replaceLine.LRU;
            replaceLine.valid = true;
            replaceLine.dirty = !load;
            replaceLine.tag = tag;
            updateLRU(replaceLine);
        }

        private void updateLRU(Line line) {
            int prevLRU = line.LRU;
            line.LRU = lines.length - 1;
            int cur = next;
            for (int i = 0; i < lines.length; i++) {
                if (i == cur) continue;
                if (lines[i].LRU > prevLRU) { lines[i].LRU--; }
                if (lines[i].LRU == 0) next = i;
            }
        }

        class Line {
            String tag;
            boolean valid = false;
            boolean dirty = false;
            int LRU = 0;
            public Line(int LRU) {
                this.LRU = LRU;
            }
        }
    }
}
