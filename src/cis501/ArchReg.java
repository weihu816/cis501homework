package cis501;

/** A class representing an architectural register */
public class ArchReg extends Reg {
    public ArchReg(int r) {
        super(r);
    }

    @Override
    public String toString() {
        return "r" + String.valueOf(reg);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ArchReg) {
            return this.reg == ((ArchReg)o).reg;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.reg;
    }

}
