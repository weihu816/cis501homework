package cis501;

import java.util.Map;

public interface IOOORegisterRenamer {

    /**
     * The number of architectural registers in ARMv7. 16 general-purpose registers, +1 for
     * condition codes.
     */
    final int NUM_ARCH_REGS = 17;

    /** Treat condition codes as architectural register 16. */
    final int COND_CODE_ARCH_REG = 16;

    int availablePhysRegs();

    /**
     * Allocate a new physical register for the given architectural register ar.
     *
     * @return the new physical register that maps to ar. Returns null if there are no more physical
     * registers available.
     */
    PhysReg allocateReg(ArchReg ar);

    /** Place physical register pr at the end of the free list */
    void freeReg(PhysReg pr);

    /**
     * @return the physical register that architectural register ar currently maps to.
     */
    PhysReg a2p(ArchReg ar);

    /**
     * Rename the registers in the given insn
     *
     * @param i The insn whose register inputs and outputs should be renamed
     * @return The archreg-to-physreg mapping used for this insn. This map should only include
     * mappings for the input and output registers of this insn. Be sure to include mapping(s) for
     * the condition code register if needed.
     */
    Map<ArchReg, PhysReg> rename(Insn i);

}
