package cis501;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Hashtable;

/**
 * Created by dongniwang on 10/1/16.
 */
public class TraceTest {
    private static final String TRACE_FILE = "/Users/dongniwang/Desktop/CIS_501/501hw2/streamcluster-10M-v1.trace.gz";
    private static InsnIterator uiter;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Run the trace before any tests are run. Then, test the results of the run.

        // TODO: replace the name of trace file here
        uiter = new InsnIterator(TRACE_FILE, -1);
    }

    @Test
    public void testDup() {
        Hashtable<Long, Insn> pcInsnRecorder = new Hashtable<>();
        while (uiter.hasNext()) {
            Insn tmp = uiter.next();
            Insn old = pcInsnRecorder.put(tmp.pc, tmp);
            if (old != null && old.branch != null) {
                //print("new: " + tmp.branch + " " + tmp.branchTarget);
                //print("old: " + old.branch + " " + old.branchTarget);
                if (tmp.branch != old.branch) {
                    print("old: " + old.toString());
                    print("new: " + tmp.toString());
                }
            }
        }
    }

    private static void print(String s) {
        System.out.println(s);
    }
}
