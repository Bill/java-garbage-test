import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.function.Supplier;

public class GarbageTest {

    static class LazySeq {
        final int head;

        volatile Supplier<LazySeq> tailThunk;
        LazySeq tailValue;

        LazySeq(final int head, final Supplier<LazySeq> tailThunk) {
            this.head = head;
            this.tailThunk = tailThunk;
            tailValue = null;
        }

        int head() {
            return head;
        }

        LazySeq tail() {
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

    static class Increment implements Supplier<LazySeq> {
        final int seed;
        Increment(final int seed) {
            this.seed = seed;
        }
        @Override
        public LazySeq get() {
            return new LazySeq(seed, new Increment(seed+1));
        }
    }

    static LazySeq naturals() {
        return new Increment(1).get();
    }

    static LazySeq drop(
            final int n,
            final LazySeq lazySeqArg) {
        LazySeq lazySeq = lazySeqArg;
        for( int i = n; i > 0 && null != lazySeq; i -= 1) {
            lazySeq = lazySeq.tail();
        }

//System.gc();
//System.exit(1);

        return lazySeq;
    }

    static int nth(final int n, final LazySeq lazySeq) {
        return drop(n, lazySeq).head();
    }

    static int SIZE = (int)1e6;

    // succeeds @ SIZE = (int)1e8 with java -Xmx10m
    @Test
    public void dropTest() {
        assertThat( drop(SIZE, naturals()).head(), is(SIZE+1));
    }

    // fails with OutOfMemoryError @ SIZE = (int)1e6 with java -Xmx10m
    @Test
    public void nthTest() {
        assertThat( nth(SIZE, naturals()), is(SIZE+1));
    }

}