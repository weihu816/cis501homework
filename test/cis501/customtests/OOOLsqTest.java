package cis501.customtests;

import cis501.IOOOLoadStoreQueue;
import cis501.LoadHandle;
import cis501.StoreHandle;
import cis501.submission.OOOLoadStoreQueue;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * needs inspiration for testing ideas..
 *
 * Created by dongniwang on 11/27/16.
 */
public class OOOLsqTest {
    private IOOOLoadStoreQueue lsq;
    private final static int LOADQ_ENTRIES = 6;
    private final static int STOREQ_ENTRIES = 6;

    @Before
    public void setup() {
        lsq = new OOOLoadStoreQueue(LOADQ_ENTRIES, STOREQ_ENTRIES);
    }

    @Test
    public void testCapacity() {
        lsq.commitOldest(); // clear out preamble insn, if there was one
        for (int i = 0; i < LOADQ_ENTRIES; i++) {
            assertTrue(lsq.roomForLoad());
            lsq.dispatchLoad(1);
        }
        assertFalse(lsq.roomForLoad());

        for (int i = 0; i < STOREQ_ENTRIES; i++) {
            assertTrue(lsq.roomForStore());
            lsq.dispatchStore(1);
        }
        assertFalse(lsq.roomForStore());
    }

    @Test
    public void test1() {
        StoreHandle s = lsq.dispatchStore(1);
        assertTrue(lsq.executeStore(s, 0xA, 100).isEmpty());
        LoadHandle l = lsq.dispatchLoad(1);
        assertEquals(0, lsq.executeLoad(l, 0xB));
        assertEquals(100, lsq.executeLoad(l, 0xA));
    }

    @Test
    public void testMultiByte() {
        assertTrue(lsq.executeStore(lsq.dispatchStore(1), 0xA, 0x11).isEmpty());
        assertTrue(lsq.executeStore(lsq.dispatchStore(1), 0xB, 0x20).isEmpty());
        assertTrue(lsq.executeStore(lsq.dispatchStore(1), 0xE, 0x20).isEmpty());
        assertEquals(0x11200000, lsq.executeLoad(lsq.dispatchLoad(4), 0xA));
    }

    @Test
    public void testSquash() {
        //TODO: need to delete entries in LSQ?
    }
}
