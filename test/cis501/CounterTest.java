package cis501;
import org.junit.Before;
import org.junit.Test;

import java.util.Hashtable;

import static org.junit.Assert.assertEquals;

/**
 * Created by dongniwang on 9/29/16.
 */
public class CounterTest {
    private Hashtable<Long, PredCounterValue> counters = new Hashtable<>();
    private PredCounterValue preC = PredCounterValue.N;


    @Test
    public void testAdvance() {
        assertEquals(preC, PredCounterValue.N);
        assertEquals(preC.penalize(), PredCounterValue.N);
        preC = preC.enforce();
        assertEquals(preC, PredCounterValue.n);
        preC = preC.enforce();
        assertEquals(preC, PredCounterValue.t);
        preC = preC.enforce();
        assertEquals(preC, PredCounterValue.T);
        preC = preC.enforce();
        assertEquals(preC, PredCounterValue.T);
        preC = preC.penalize();
        assertEquals(preC, PredCounterValue.t);
    }

}
