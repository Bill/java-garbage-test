import org.junit.Ignore;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.function.Supplier;

public class GarbageTest {

    /**
     * a not-perfectly-lazy lazy sequence of ints. see LazierGarbageTest for a lazier one
     */
    static class LazyishSeq {
        final int head;

        volatile Supplier<LazyishSeq> tailThunk;
        LazyishSeq tailValue;

        LazyishSeq(final int head, final Supplier<LazyishSeq> tailThunk) {
            this.head = head;
            this.tailThunk = tailThunk;
            tailValue = null;
        }

        int head() {
            return head;
        }

        LazyishSeq tail() {
            if (null != tailThunk)
                synchronized(this) {
                    if (null != tailThunk) {
                        tailValue = tailThunk.get();
                        tailThunk = null;
                    }
                }
            return tailValue;
        }
    }

    static class Incrementing implements Supplier<LazyishSeq> {
        final int seed;
        private Incrementing(final int seed) { this.seed = seed;}

        public static LazyishSeq createSequence(final int n) {
            return new LazyishSeq( n, new Incrementing(n+1));
        }

        @Override
        public LazyishSeq get() {
            return createSequence(seed);
        }
    }

    static LazyishSeq naturals() {
        return Incrementing.createSequence(1);
    }

    static LazyishSeq drop(
            final int n,
            final LazyishSeq lazySeqArg) {
        LazyishSeq lazySeq = lazySeqArg;
        for( int i = n; i > 0 && null != lazySeq; i -= 1) {
            lazySeq = lazySeq.tail();
        }
// uncomment these two lines and run with YourKit onexit=memory to diagnose heap growth:
//        System.gc();
//        System.exit(1);
        return lazySeq;
    }

    static int nth(final int n, final LazyishSeq lazySeq) {
        return drop(n, lazySeq).head();
    }

    static int N = (int)1e6;

    // succeeds @ N = (int)1e8 with java -Xmx10m
    @Test
    /**
     * This (should be) exactly the same functionality as nthTest(). The only change is that
     * the nth() call has been inlined via IntelliJ refactoring.
     */
    public void dropTest() {
        assertThat( drop(N, naturals()).head(), is(N+1));
    }

    // fails with OutOfMemoryError @ N = (int)1e6 with java -Xmx10m
    // unless you also add -Xcomp to force compilation (prevent interpretation)
    @Test
    public void nthTest() {
        assertThat( nth(N, naturals()), is(N+1));
    }

}
