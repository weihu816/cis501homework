package cis501.submission;

import cis501.Direction;
import cis501.IDirectionPredictor;

public class DirPredTournament extends DirPredBimodal {
    private IDirectionPredictor predictorNT;
    private IDirectionPredictor predictorT;

    public DirPredTournament(int chooserIndexBits, IDirectionPredictor predictorNT, IDirectionPredictor predictorT) {
        super(chooserIndexBits); // re-use DirPredBimodal as the chooser table
        this.predictorNT = predictorNT;
        this.predictorT = predictorT;
    }

    @Override
    public Direction predict(long pc) {
        Direction predictorChoosed = super.predict(pc);
        if(predictorChoosed == Direction.Taken) return this.predictorT.predict(pc);
        return this.predictorNT.predict(pc);
    }

    @Override
    public void train(long pc, Direction actual) {
        Direction predictedNT = this.predictorNT.predict(pc);
        Direction predictedT = this.predictorT.predict(pc);
        if(predictedNT != predictedT) {
            if(predictedT == actual) {
                super.train(pc, Direction.Taken);
            } else {
                super.train(pc, Direction.NotTaken);
            }
        }
        this.predictorNT.train(pc, actual);
        this.predictorT.train(pc, actual);
    }

}
