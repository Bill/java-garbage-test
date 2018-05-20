This is idiomatic Clojure:

```
Bills-MacBook-Air:java-garbage-test Bill$ clojure -J-Xmx10m
Clojure 1.9.0
user=> (defn naturals [] (iterate inc 1))
#'user/naturals
user=> (for [n [1e3 1e4 1e5 1e6 1e7]]
  (time (first (drop n (naturals)))))
("Elapsed time: 29.09182 msecs"
"Elapsed time: 34.94448 msecs"
"Elapsed time: 113.86844 msecs"
"Elapsed time: 444.99815 msecs"
"Elapsed time: 3619.999604 msecs"
1001 10001 100001 1000001 10000001)
```

This project attempts to do a similar trick with the minimal amount of (not library-quality) Java. We define a `LazySeq` class and define a `Supplier` for it, called `Increment`.

Then we define `naturals()`, `drop()`, and `nth()` functions (static methods) and run two scaling tests.

In a JVM w/ a 10MB heap, with `N` = `1e6`, the `drop()` test succeeds but the `nth()` test fails with an `OutOfMemoryError`.

Take a look at `dropTest()` and `nthTest()` and see if you discern any material differences.

By `@Ignore`-ing `nthTest()` was was able to raise `N` to `1e8` and see the `dropTest()` succeed (in a little under two minutes).

I experimented with YourKit `onexit=memory` profiling: running `gc()` and exiting before returning from `drop()`. The results were confusing.

## try it

Shell:

```bash
mvn test
```

You'll see `nthTest()` fail.

Or run in IntelliJ with `-Xmx10m` added to VM options to see `nthTest()` fail.

Or run in IntelliJ with the YourKit plugin. It's most useful, in the run config, under "Startup/Connection" > "Advanced" to set "Other Startup Options..." to `onexit=memory` You might also want to experiment with running `gc()` and exiting before returning from `drop()`.

When I set `N` to a manageable number like 10, and go to the YourKit "Memory" tab, and look in "Inspections" > "Possible Leaks" > "Objects retained by inner class back references", I see the `LazySeq` whose head is `1` listed, both when running `dropTest()` (how could that test scale if it were retaining head?!?) and `nthTest()`.

When I use the "Object explorer" and search for `LazySeq`, I see about 10 instances in both cases (?!?). In both cases, both the instance with head = 1 and the instance with head = 11 are listed as `Stack Local`.

I experimented a bit with trying to clear locals, but wasn't able to improve the scalability of `nth()`.
