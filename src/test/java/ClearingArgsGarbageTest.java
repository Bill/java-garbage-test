import org.junit.Ignore;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.function.Supplier;

/**
 * Like GarbageTest, but this one's drop() and nth() set their parameters to null
 * before making any calls.
 */
public class ClearingArgsGarbageTest {

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
            LazyishSeq lazySeqArg) {
        LazyishSeq lazySeqLocal = lazySeqArg;
        lazySeqArg = null;
        for( int i = n; i > 0 && null != lazySeqLocal; i -= 1) {
            lazySeqLocal = lazySeqLocal.tail();
        }
        return lazySeqLocal;
    }

    static int nth(final int n, /*final*/ LazyishSeq lazySeqArg) {
        LazyishSeq lazySeqLocal = lazySeqArg;
        lazySeqArg = null;
        return drop(n,ret1(lazySeqLocal, lazySeqLocal=null)).head();
    }

    static int N = (int)1e6;

    // succeeds @ N = (int)1e8 with java -Xmx10m
    @Test
    public void dropTest() {
        assertThat( drop(N, naturals()).head(), is(N+1));
    }

    // succeeds @ N = (int)1e8 with java -Xmx10m
    @Test
    public void nthTest() {
        assertThat( nth(N, naturals()), is(N+1));
    }

    private static LazyishSeq ret1(final LazyishSeq seq, final Object _ignored) {
        return seq;
    }

}
