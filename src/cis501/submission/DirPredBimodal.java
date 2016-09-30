package cis501.submission;

import cis501.Direction;
import cis501.IDirectionPredictor;
import cis501.PredDirection;


public class DirPredBimodal implements IDirectionPredictor {
    private int indexBitM;
    private PredDirection[] predCountTable;

    public DirPredBimodal(int indexBits) {
        this.indexBitM =  (1<<indexBits-1);
        this.predCountTable = new PredDirection[1<<indexBits];
    }

    @Override
    public Direction predict(long pc) {
        PredDirection predicted = predCountTable[index(pc)];
        if(predicted == null) return Direction.NotTaken;
        return predicted.predict();
    }

    @Override
    public void train(long pc, Direction actual) {
        int indexed = index(pc);
        PredDirection predReg = predCountTable[indexed];
        if(predReg == null) predReg = PredDirection.N;
        predReg = predReg.train(actual);
        predCountTable[indexed] = predReg;
    }

    public int index(long pc) { return (int) pc&indexBitM;}
}
