package cis501.submission;

import cis501.ArchReg;
import cis501.IOOORegisterRenamer;
import cis501.Insn;
import cis501.PhysReg;

import java.util.Map;

public class OOORegisterRenamer implements IOOORegisterRenamer {

    public OOORegisterRenamer(int pregs) {}

    @Override
    public int availablePhysRegs() {
        return 0;
    }

    @Override
    public PhysReg allocateReg(ArchReg ar) {
        return null;
    }

    @Override
    public void freeReg(PhysReg pr) {

    }

    @Override
    public PhysReg a2p(ArchReg ar) {
        return null;
    }

    @Override
    public Map<ArchReg, PhysReg> rename(Insn i) {
        return null;
    }
}
