package cis501;

/**
 * Created by dongniwang on 9/29/16.
 */
public enum PredCounterValue {
    N, n, t, T;

    private static PredCounterValue[] vals = values();

    public PredCounterValue penalize() { return this == N ? this : vals[this.ordinal()-1];}
    public PredCounterValue enforce() { return this == T ? this : vals[this.ordinal()+1];}
}
