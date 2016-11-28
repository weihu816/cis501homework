package cis501.customtests;

import cis501.*;
import cis501.submission.OOORegisterRenamer;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static cis501.IOOORegisterRenamer.NUM_ARCH_REGS;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by dongniwang on 11/27/16.
 */
public class OOORenamerTest {
    private static final int PREGS = 60;
    private OOORegisterRenamer rr;

    private static Insn makeInsn(int dst, int src1, int src2, CondCodes cond) {
        return new Insn(dst, src1, src2,
                1, 4,
                null, 0, cond,
                null, 1, 1,
                "synthetic");
    }

    /** Runs before each test...() method */
    @Before
    public void setup() {
        rr = new OOORegisterRenamer(PREGS);
    }

    @Test
    public void testInitialAllMapping() {
        for (int i = 1; i < NUM_ARCH_REGS; i++) {
            assertEquals(i, rr.a2p(i).get());
        }
    }

    @Test
    public void testAllAllocate() {
        for (int i = 0; i < NUM_ARCH_REGS; i++) {
            assertEquals(NUM_ARCH_REGS+i, rr.allocateReg(i+1).get());
        }
        assertEquals(PREGS-2*NUM_ARCH_REGS, rr.availablePhysRegs());
    }

    @Test
    public void testAllFreeReallocate() {
        for (int i = 0; i < NUM_ARCH_REGS; i++) {
            rr.freeReg(new PhysReg(i));
        }
        for (int i = 0; i < (PREGS - NUM_ARCH_REGS); i++) { // empty the free list, except for p1
            assertEquals(NUM_ARCH_REGS + i, rr.allocateReg(i % NUM_ARCH_REGS).get());
        }
        for (int j = 0; j < NUM_ARCH_REGS; j++) {
            assertEquals(j, rr.allocateReg(j+1).get());
        }
        assertEquals(0, rr.availablePhysRegs());
    }

    @Test
    /**
     * test rename and commit with non-read/write insn
     */
    public void testXORInsn() {
        //test rename
        Map inputs = new HashMap<Short, PhysReg>();
        Map outputs = new HashMap<Short, PhysReg>();

        Insn xor = makeInsn(3, 1, 2, null);
        rr.rename(xor, inputs, outputs);
        assertEquals(rr.a2p(3).hashCode(), NUM_ARCH_REGS);
        assertEquals(inputs.get((short)1).hashCode(), 1);
        assertEquals(inputs.get((short)2).hashCode(), 2);
        assertEquals(outputs.get((short)3).hashCode(), NUM_ARCH_REGS);

        Insn add = makeInsn(4, 3, 4, null);
        rr.rename(add, inputs, outputs);
        assertEquals(rr.a2p(4).hashCode(), NUM_ARCH_REGS+1);
        assertEquals(inputs.get((short)3).hashCode(), NUM_ARCH_REGS);
        assertEquals(inputs.get((short)4).hashCode(), 4);
        assertEquals(outputs.get((short)4).hashCode(), NUM_ARCH_REGS+1);

        Insn sub = makeInsn(3, 5, 2, null);
        rr.rename(sub, inputs, outputs);
        assertEquals(rr.a2p(3).hashCode(), NUM_ARCH_REGS+2);
        assertEquals(inputs.get((short)2).hashCode(), 2);
        assertEquals(inputs.get((short)5).hashCode(), 5);
        assertEquals(outputs.get((short)3).hashCode(), NUM_ARCH_REGS+2);

        Insn addi = makeInsn(1, 3, 0, null);
        rr.rename(addi, inputs, outputs);
        assertEquals(rr.a2p(1).hashCode(), NUM_ARCH_REGS+3);
        assertEquals(inputs.get((short)3).hashCode(), NUM_ARCH_REGS+2);
        assertEquals(inputs.get((short)0).hashCode(), 0);
        assertEquals(outputs.get((short)1).hashCode(), NUM_ARCH_REGS+3);

        // test commit Free
        assertEquals(rr.availablePhysRegs(), PREGS-NUM_ARCH_REGS-4);
    }

    /**
     * test rename and commit with non-read/write insn
     */
    @Test
    public void testCondInsn() {
        Map inputs = new HashMap<Short, PhysReg>();
        Map outputs = new HashMap<Short, PhysReg>();

        Insn xor = makeInsn(3, 1, 2, CondCodes.ReadCC);
        rr.rename(xor, inputs, outputs);
        assertEquals(rr.a2p(3).hashCode(), NUM_ARCH_REGS);
        assertEquals(inputs.get((short)1).hashCode(), 1);
        assertEquals(inputs.get((short)2).hashCode(), 2);
        assertEquals(inputs.get(IOOORegisterRenamer.COND_CODE_ARCH_REG).hashCode(), IOOORegisterRenamer.COND_CODE_ARCH_REG);
        assertEquals(outputs.get((short)3).hashCode(), NUM_ARCH_REGS);

        inputs = new HashMap<Short, PhysReg>();
        outputs = new HashMap<Short, PhysReg>();
        Insn add = makeInsn(4, 3, 4, CondCodes.WriteCC);
        rr.rename(add, inputs, outputs);
        assertEquals(rr.a2p(4).hashCode(), NUM_ARCH_REGS+1);
        assertEquals(inputs.get((short)3).hashCode(), NUM_ARCH_REGS);
        assertEquals(inputs.get((short)4).hashCode(), 4);
        assertEquals(outputs.get((short)4).hashCode(), NUM_ARCH_REGS+1);
        assertEquals(outputs.get(IOOORegisterRenamer.COND_CODE_ARCH_REG).hashCode(), NUM_ARCH_REGS+2);

        inputs = new HashMap<Short, PhysReg>();
        outputs = new HashMap<Short, PhysReg>();
        Insn sub = makeInsn(3, 5, 2, CondCodes.ReadWriteCC);
        rr.rename(sub, inputs, outputs);
        assertEquals(rr.a2p(3).hashCode(), NUM_ARCH_REGS+3);
        assertEquals(inputs.get((short)2).hashCode(), 2);
        assertEquals(inputs.get((short)5).hashCode(), 5);
        assertEquals(inputs.get(IOOORegisterRenamer.COND_CODE_ARCH_REG).hashCode(), NUM_ARCH_REGS+2);
        assertEquals(outputs.get((short)3).hashCode(), NUM_ARCH_REGS+3);
        assertEquals(outputs.get(IOOORegisterRenamer.COND_CODE_ARCH_REG).hashCode(), NUM_ARCH_REGS+4);

        assertEquals(rr.availablePhysRegs(), PREGS-NUM_ARCH_REGS-5);
    }
}
