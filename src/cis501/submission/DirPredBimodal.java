package cis501.submission;

import cis501.Direction;
import cis501.IDirectionPredictor;
import cis501.PredDirection;

import java.util.Hashtable;

public class DirPredBimodal implements IDirectionPredictor {
    private int indexBits;
    private Hashtable<Long, PredDirection> predCountTable;

    public DirPredBimodal(int indexBits) {
        this.indexBits = indexBits;
        this.predCountTable = new Hashtable<>();
    }

    @Override
    public Direction predict(long pc) {
        PredDirection predicted = predCountTable.get(index(pc));
        if(predicted == null) return Direction.NotTaken;
        return predicted.predict();
    }

    @Override
    public void train(long pc, Direction actual) {
        long indexed = index(pc);
        PredDirection predicted = predCountTable.get(indexed);
        if(predicted == null) predicted = PredDirection.N;
        predicted = predicted.train(actual);
        predCountTable.put(indexed, predicted);
    }

    public long index(long pc) { return pc & (1<<indexBits-1);}
}
