package cis501.submission;

import cis501.Direction;
import cis501.IDirectionPredictor;
import cis501.PredDirection;

import java.util.Arrays;


public class DirPredBimodal implements IDirectionPredictor {
    private int indexBitM;
    private PredDirection[] predCountTable;

    public DirPredBimodal(int indexBits) {
        this.indexBitM =  (1<<indexBits)-1;
        this.predCountTable = new PredDirection[1<<indexBits];
        // initialize the predictor to strongly not taken
        Arrays.fill(this.predCountTable, PredDirection.N);
    }

    @Override
    public Direction predict(long pc) {
        PredDirection predicted = predCountTable[index(pc)];
        System.out.println("IN DP MODAL: " + predicted);
        return predicted.predict();
    }

    @Override
    public void train(long pc, Direction actual) {
        int indexed = index(pc);
        PredDirection predReg = predCountTable[indexed];
        predCountTable[indexed] = predReg.train(actual);
        System.out.println("IN DP MODAL trained: " + pc + "    " +indexed +" :: " + predCountTable[indexed]);
    }

    public int index(long pc) { return (int) pc & indexBitM;}
}
