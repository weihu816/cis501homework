package cis501.submission;

import cis501.CondCodes;
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
     * FIFO toFree List: a queue of used physical register names
     */
    LinkedList<PhysReg> freeList = new LinkedList<>();
    LinkedList<PhysReg> toFreeList = new LinkedList<>();

    /**
     * Constructor
     * @param pregs - number of physical registers
     */
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

    /**
     * Free the oldest committed physical register
     */
    private void commitFreeHelper() {
        if (toFreeList.size() == 0) return;
        PhysReg toFree = toFreeList.removeFirst();
        freeReg(toFree);
    }

    @Override
    public PhysReg a2p(int ar) {
        return mapTable[ar];
    }

    @Override
    public void rename(Insn i, Map<Short, PhysReg> inputs, Map<Short, PhysReg> outputs) {
        // inputs
        if(i.srcReg1 != -1) inputs.put(i.srcReg1, a2p(i.srcReg1));
        if(i.srcReg2 != -1) inputs.put(i.srcReg2, a2p(i.srcReg2));
        if(i.condCode == CondCodes.ReadCC || i.condCode == CondCodes.ReadWriteCC) {
            inputs.put(IOOORegisterRenamer.COND_CODE_ARCH_REG,a2p(IOOORegisterRenamer.COND_CODE_ARCH_REG));
        }

        // outputs
        if (i.dstReg != -1) renameOutputEle(i.dstReg, outputs);
        if(i.condCode == CondCodes.WriteCC || i.condCode == CondCodes.ReadWriteCC) {
            renameOutputEle(IOOORegisterRenamer.COND_CODE_ARCH_REG, outputs);
        }
    }

    /**
     * helper function: rename output
     * @param archReg the arch register to free
     * @param outputs
     */
    public void renameOutputEle(Short archReg, Map<Short, PhysReg> outputs) {
        toFreeList.addLast(a2p(archReg));
        if (availablePhysRegs() == 0) commitFreeHelper();
        mapTable[archReg] = allocateReg(archReg);
        outputs.put(archReg,a2p(archReg));
    }

}
