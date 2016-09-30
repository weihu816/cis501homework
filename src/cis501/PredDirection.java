package cis501;

/**
 * Created by dongniwang on 9/29/16.
 */
public enum PredDirection {
    N, n, t, T;

    private static PredDirection[] vals = values();

    /** predict a Direction */
    public Direction predict() {
        return (this.ordinal() >> 1) == 1 ? Direction.Taken : Direction.NotTaken;
    }

    /** train moves toward the actual direction */
    public PredDirection train(Direction actual) {
        return actual == Direction.Taken ? this.enforce() : this.penalize();
    }

    /** penalize moves towards N */
    public PredDirection penalize() { return this == N ? this : vals[this.ordinal()-1];}
    /** enforce moves towards T */
    public PredDirection enforce() { return this == T ? this : vals[this.ordinal()+1];}
}
