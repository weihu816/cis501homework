package cis501.submission;

import cis501.Direction;

public class DirPredGshare extends DirPredBimodal {
    private int historyLimitMng;
    private int historyRegister;

    public DirPredGshare(int indexBits, int historyBits) {
        super(indexBits);
        this.historyLimitMng = (1 << historyBits) - 1;
    }

    @Override
    public Direction predict(long pc) {
        return super.predict(pc ^ this.historyRegister);
    }

    @Override
    public void train(long pc, Direction actual) {
        updateHistory(actual);
        super.train(pc ^ this.historyRegister, actual);
    }

    public void updateHistory(Direction actual) {
        // shift the history register and add the last bit (taken = 1)
        int lastBit = actual == Direction.Taken ? 1 : 0;
        this.historyRegister = (this.historyRegister << 1) + lastBit;
        // limit the length of history register: only hinstoryBits length
        this.historyRegister = this.historyRegister & historyLimitMng;
    }
}
