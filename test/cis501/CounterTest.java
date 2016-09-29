package cis501;
import cis501.submission.BranchTargetBuffer;
import org.junit.Before;
import org.junit.Test;

import java.util.Hashtable;

import static org.junit.Assert.assertEquals;

/**
 * Created by dongniwang on 9/29/16.
 */
public class CounterTest {
    private Hashtable<Long, PredDirection> counters = new Hashtable<>();
    private PredDirection preC = PredDirection.N;
    private IBranchTargetBuffer btb;

    @Before
    public void setUp() throws Exception {
        btb = new BranchTargetBuffer(3);
    }

    @Test
    public void testAdvance() {
        assertEquals(preC, PredDirection.N);
        assertEquals(preC.penalize(), PredDirection.N);
        preC = preC.enforce();
        assertEquals(preC, PredDirection.n);
        preC = preC.enforce();
        assertEquals(preC, PredDirection.t);
        preC = preC.enforce();
        assertEquals(preC, PredDirection.T);
        preC = preC.enforce();
        assertEquals(preC, PredDirection.T);
        preC = preC.penalize();
        assertEquals(preC, PredDirection.t);
    }

    @Test
    public void testTrain() {
        Direction not = Direction.NotTaken;
        Direction taken = Direction.Taken;
        preC = preC.train(not);
        assertEquals(preC, PredDirection.N);
        preC = preC.train(taken);
        assertEquals(preC, PredDirection.n);
        preC = preC.train(taken);
        assertEquals(preC, PredDirection.t);
        preC = preC.train(taken);
        assertEquals(preC, PredDirection.T);
        preC = preC.train(taken);
        assertEquals(preC, PredDirection.T);
        preC = preC.train(not);
        assertEquals(preC, PredDirection.t);
    }
}
