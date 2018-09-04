import org.junit.Ignore;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.function.Supplier;

/**
 * Like ClearingArgsGarbageTest but this one has a lazier lazy sequence.
 */
public class LazierGarbageTest {

    // an int sequence: made non-generic to make these tests a little easier to understand
    interface Seq {
        int head();
        Seq tail();
    }

    // a cons cell with an int head
    static class Cons implements Seq {
        final int head;
        final Seq tail;

        public Cons(final int head, final Seq tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        public int head() {
            return head;
        }

        @Override
        public Seq tail() {
            return tail;
        }
    }

    // a lazy sequence of ints
    static class LazySeq implements Seq {
        private volatile Supplier<Seq> thunk;
        private Seq value;

        public LazySeq(final Supplier<Seq> thunk) {
            this.thunk = thunk;
            value = null;
        }

        public int head() { return realize().head();}

        public Seq tail() { return realize().tail();}

        private Seq realize() {
            if (null != thunk)
                synchronized(this) {
                    if (null != thunk) {
                        value = thunk.get();
                        thunk = null;
                    }
                }
            return value;
        }
    }

    /**
     * Explicitly declare a _static_ class rather than an anonymous inner class.
     * The latter would retain a reference to the enclosing object.
     *
     * It's arguable that we could have declared an anonymous inner class constructed
     * via a static method e.g. incrementing(),
     * but I'm erring on the defensive side as I try to figure out where my leak is.
     */
    static class Incrementing implements Supplier<Seq> {
        private final int seed;
        private Incrementing(final int seed) { this.seed = seed;}

        public static Seq createSequence(final int n) {
            return new LazySeq( new Incrementing(n));
        }

        @Override
        public Seq get() {
            return new Cons(seed, createSequence(seed+1));
        }
    }

    static Seq naturals() {
        return Incrementing.createSequence(1);
    }

    static Seq drop(
            final int n,
            Seq seqArg) {
        Seq seq = seqArg;
        seqArg = null;
        for( int i = n; i > 0 && null != seq; i -= 1) {
            seq = seq.tail();
        }
        return seq;
    }

    static int nth(final int n, Seq seqArg) {
        Seq seq = seqArg;
        seqArg = null;
        return drop(n, seq).head();
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
