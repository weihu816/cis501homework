package cis501.submission;

import cis501.IOOOLoadStoreQueue;
import cis501.LoadHandle;
import cis501.StoreHandle;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class OOOLoadStoreQueue implements IOOOLoadStoreQueue {

    private int loadQCapacity, storeQCapacity;
    private AtomicInteger count = new AtomicInteger(0);

    /**
     * Load queue: detects ordering violations
     */

    private Queue<LoadHandle> loadQ;

    /**
     * Store queue: handles forwarding, allows OoO stores
     */
    private Queue<StoreHandle> storeQ;

    /**
     * OOOLoadStoreQueue Constructor
     * @param loadQCapacity
     * @param storeQCapacity
     */
    public OOOLoadStoreQueue(int loadQCapacity, int storeQCapacity) {
        this.loadQCapacity = loadQCapacity;
        this.storeQCapacity = storeQCapacity;
        loadQ = new ArrayDeque<>(loadQCapacity);
        storeQ = new ArrayDeque<>(storeQCapacity);
    }

    @Override
    public boolean roomForLoad() {
        return loadQ.size() < loadQCapacity;
    }

    @Override
    public boolean roomForStore() {
        return storeQ.size() < storeQCapacity;
    }

    @Override
    public void commitOldest() {
        // causes the single oldest insn to leave the LSQ if there is such an insn, otherwise it does nothing.
        if (loadQ.isEmpty() && storeQ.isEmpty()) return;
        if (!loadQ.isEmpty() && (storeQ.isEmpty() || loadQ.peek().getBday() < storeQ.peek().getBday())) {
            loadQ.poll();
        } else {
            storeQ.poll();
        }
    }

    @Override
    public LoadHandle dispatchLoad(int size) {
        if (!roomForLoad()) return null;
        LoadHandle loadHandle = new LoadHandleImpl(count.getAndIncrement(), size);
        loadQ.offer(loadHandle);
        return loadHandle;
    }

    @Override
    public StoreHandle dispatchStore(int size) {
        if (!roomForStore()) return null;
        StoreHandle storeHandle = new StoreHandleImpl(count.getAndIncrement(), size);
        storeQ.offer(storeHandle);
        return storeHandle;
    }

    @Override
    public long executeLoad(LoadHandle handle, long address) {
        handle.setAddress(address);
        handle.done();

        long result = 0;
        final int size = handle.getSize();
        for (int i = 0; i < size; i++) {
            long curAddress = address + i;
            // Select store that is: 1. to same address as load 2. Older than the load and youngest
            StoreHandle storeHandle = null;
            Iterator<StoreHandle> iterator = storeQ.iterator();
            while (iterator.hasNext()) {
                StoreHandle next = iterator.next();
                if (next.isDone() && next.containAddress(curAddress) && next.getBday() < handle.getBday()) {
                    if (storeHandle == null || next.getBday() > storeHandle.getBday()) storeHandle = next;
                }
            }
            result = result << 8;
            if (storeHandle != null) {
                handle.addForwardFrom(curAddress, storeHandle.getBday());
                // Extract the byte value
                result |= storeHandle.getByte(curAddress);
            }
        }
        return result;
    }

    @Override
    public Collection<? extends LoadHandle> executeStore(StoreHandle handle, long address, long value) {
        Set<LoadHandle> set = new HashSet<>();
        if (handle.isDone()) return set;
        handle.setValue(value);
        handle.setAddress(address);
        handle.done();

        final int size = handle.getSize();
        for (int i = 0; i < size; i++) {
            long curAddress = address + i;
            Iterator<LoadHandle> iterator = loadQ.iterator();
            while (iterator.hasNext()) {
                LoadHandle next = iterator.next();
                if (next.isDone() && next.containAddress(curAddress) && next.getBday() > handle.getBday()) {
                    if (handle.getBday() > next.getLatestForwardFrom(curAddress)) {
                        set.add(next);
                    }
                }
            }
        }
        return set;
    }
}

/**
 * LoadHandleImpl
 */
class LoadHandleImpl implements LoadHandle {
    int size;
    long addr, bday;
    boolean committed = false, isDone = false;

    Map<Long, Long> forwardFrom = new HashMap<>();
    List<Long> addresses = new Vector<>();

    public LoadHandleImpl(long bday, int size) {
        this.bday = bday;
        this.size = size;
    }

    @Override
    public void setAddress(long addr) {
        this.addr = addr;
        addresses.clear();
        for (int i = 0; i < this.size; i++) {
            addresses.add(addr + i);
        }
    }

    @Override
    public void addForwardFrom(long addr, long fromBday) { this.forwardFrom.put(addr, fromBday); }

    @Override
    public void done() { this.isDone = true; }

    @Override
    public long getAddress() { return this.addr;}

    @Override
    public boolean containAddress(long addr) {
        return addresses.contains(addr);
    }

    @Override
    public long getLatestForwardFrom(long addr) {
        if (!forwardFrom.containsKey(addr)) return -1;
        return forwardFrom.get(addr);
    }

    @Override
    public long getBday() { return this.bday; }

    @Override
    public boolean isDone() { return this.isDone; }

    @Override
    public int getSize() { return this.size; }
}


/**
 * StoreHandleImpl
 */
class StoreHandleImpl implements StoreHandle {
    int size;
    long addr, val;
    long bday;
    boolean isDone = false;

    List<Long> addresses = new Vector<>();

    public StoreHandleImpl(long bday, int size) {
        this.bday = bday;
        this.size = size;
    }

    @Override
    public void setValue(long val) { this.val = val; }

    @Override
    public void setAddress(long addr) {
        this.addr = addr;
        addresses.clear();
        for (int i = 0; i < this.size; i++) {
            addresses.add(addr + i);
        }
    }

    @Override
    public void done() { this.isDone = true; }

    @Override
    public int getSize() { return this.size; }

    @Override
    public long getAddress() {
        return this.addr;
    }

    @Override
    public long getByte(long addr) {
        int index = addresses.indexOf(addr);
        long mark = 0xFF;
        return (val >> (8 * (size-index-1))) & mark;
    }

    @Override
    public boolean containAddress(long addr) {
        return addresses.contains(addr);
    }

    @Override
    public long getValue() { return this.val; }

    @Override
    public long getBday() { return this.bday; }

    @Override
    public boolean isDone() { return this.isDone; }

}