This is idiomatic Clojure:

```
Bills-MacBook-Air:java-garbage-test Bill$ clojure -J-Xmx10m
Clojure 1.9.0
user=> (defn naturals [] (iterate inc 1))
#'user/naturals
user=> (for [n [1e3 1e4 1e5 1e6 1e7 1e8]]
  (time (first (drop n (naturals)))))
("Elapsed time: 40.11968 msecs"
"Elapsed time: 92.93135 msecs"
"Elapsed time: 137.836554 msecs"
"Elapsed time: 824.371466 msecs"
"Elapsed time: 5806.061034 msecs"
"Elapsed time: 36581.824392 msecs"
1001 10001 100001 1000001 10000001 100000001)
```

We iterated through a hundred million and one items with only a 10MB heap, in around 37 seconds.

This project attempts to do a similar trick with the minimal amount of (not library-quality) Java. The `GarbageTest` class defines a `LazyishSeq` class and a `Supplier` for it, called `Incrementing`.

Then we define `naturals()`, `drop()`, and `nth()` functions (static methods) and run two scaling tests.

In a JVM w/ a 10MB heap, with `N` = `1e6`, the `GarbageTest.dropTest()` succeeds but the `GarbageTest.nthTest()` test fails with an `OutOfMemoryError` unless the `-Xcomp` option is specified to the Java VM (see `pom.xml`)

Take a look at `dropTest()` and `nthTest()` and see if you discern any material differences.

With the `-Xcomp` JVM option, I was able to raise `N` to `1e8` and see both test succeed.

Have a look at `ClearingArgsGarbageTest` for a better solution to the problem (than the `-Xcomp` JVM option). That test uses a trick from the Clojure library: `clojure.lang.Util.ret1()` to pass a (sequence) parameter and also clear the variable holding that parameter--in the same _statement_. Quite a trick!

## try it

Shell:

```bash
mvn test
```

## history

Before [Holger][1] answered my StackExchange question [_why does this Java method leak—and why does inlining it fix the leak?
_][2] and suggested I add the `-Xcomp` JVM option, `nthTest()` failed for `N` = `1e6`.

Before that, I experimented with null-ing method parameters before calling other methods. See `ClearingArgsGarbageTest.java` for those changes.

It turns out those changes weren't even needed though, once I added the `-Xcomp` JVM option. With that option, all the tests on all three test classes pass.

Before `-Xcomp` I also experimented with YourKit `onexit=memory` profiling: running `gc()` and exiting before returning from `drop()`. The results were confusing.

Also ran in IntelliJ with the YourKit plugin. It's most useful, in the run config, under "Startup/Connection" > "Advanced" to set "Other Startup Options..." to set `onexit=memory` You might also want to experiment with running `gc()` and exiting before returning from `drop()`.

When I set `N` to a manageable number like 10, and go to the YourKit "Memory" tab, and look in "Inspections" > "Possible Leaks" > "Objects retained by inner class back references", I see the `LazySeq` whose head is `1` listed, both when running `dropTest()` (how could that test scale if it were retaining head?!?) and `nthTest()`.

When I use the "Object explorer" and search for `LazySeq`, I see about 10 instances in both cases (?!?). In both cases, both the instance with head = 1 and the instance with head = 11 are listed as `Stack Local`.

I experimented a bit with trying to clear locals, but wasn't able to improve the scalability of `nth()` without resorting to `-Xcomp`, until [Michał Marczyk][3] came along and [shared the trick from `clojure.lang.Util.ret1()`][4]. See `ClearingArgsGarbageTest.ret1()`.


[1]: https://stackoverflow.com/users/2711488/holger
[2]: https://stackoverflow.com/questions/52169895/why-does-this-java-method-leak-and-why-does-inlining-it-fix-the-leak
[3]: https://stackoverflow.com/users/232707/micha%C5%82-marczyk
[4]: https://stackoverflow.com/a/52173822/156550