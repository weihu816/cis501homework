package cis501.submission;

import cis501.IOOORegisterRenamer;
import cis501.Insn;
import cis501.PhysReg;

import java.util.LinkedList;
import java.util.Map;

public class OOORegisterRenamer implements IOOORegisterRenamer {

    int pregs;
    /**
     * Map table: to map an architectural register to a physical register
     */
    PhysReg[] mapTable = new PhysReg[NUM_ARCH_REGS];

    /**
     * FIFO free list: a queue of physical register names
     */
    LinkedList<PhysReg> freeList = new LinkedList<>();

    public OOORegisterRenamer(int pregs) {
        this.pregs = pregs;
        for (int i = 0; i < pregs; i++) {
            freeList.add(new PhysReg(i));
        }
        for (int i = 0; i < this.NUM_ARCH_REGS; i++) {
            mapTable[i] =  freeList.removeFirst();
        }
    }

    @Override
    public int availablePhysRegs() {
        return freeList.size();
    }

    @Override
    public PhysReg allocateReg(int ar) {
        if (availablePhysRegs() == 0) return null;
        return freeList.removeFirst();
    }

    @Override
    public void freeReg(PhysReg pr) {
        freeList.addLast(pr);
    }

    @Override
    public PhysReg a2p(int ar) {
        return mapTable[ar];
    }

    @Override
    public void rename(Insn i, Map<Short, PhysReg> inputs, Map<Short, PhysReg> outputs) {

    }

}
