/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.bench.java.util.stream.ops.ref;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.Arrays;
import java.util.stream.Gatherer;
import static org.openjdk.bench.java.util.stream.ops.ref.BenchmarkGathererImpls.filter;
import static org.openjdk.bench.java.util.stream.ops.ref.BenchmarkGathererImpls.findLast;
import static org.openjdk.bench.java.util.stream.ops.ref.BenchmarkGathererImpls.map;

/**
 * Benchmark for misc operations implemented as Gatherer, with the default map implementation of Stream as baseline.
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 4, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 7, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(jvmArgsAppend = "--enable-preview", value = 1)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class GatherMiscSeq {

    /**
     * Implementation notes:
     *   - parallel version requires thread-safe sink, we use the same for sequential version for better comparison
     *   - operations are explicit inner classes to untangle unwanted lambda effects
     *   - the result of applying consecutive operations is the same, in order to have the same number of elements in sink
     */

    @Param({"10","100","1000000"})
    private int size;

    private static final Function<Long, Long> TIMES_TWO, SQUARED;
    private static final Predicate<Long> EVENS, ODDS;

    private static final Gatherer<Long, ?, Long> GATHERED;
    private static final Gatherer<Long, ?, Long> GA_FILTER_ODDS;
    private static final Gatherer<Long, ?, Long> GA_MAP_TIMES_TWO;
    private static final Gatherer<Long, ?, Long> GA_MAP_SQUARED;
    private static final Gatherer<Long, ?, Long> GA_FILTER_EVENS;

    private Long[] cachedInputArray;

    static {
        TIMES_TWO = new Function<Long, Long>() { @Override public Long apply(Long l) {
            return l*2;
        } };
        SQUARED = new Function<Long, Long>() { @Override public Long apply(Long l) { return l*l; } };

        EVENS = new Predicate<Long>() { @Override public boolean test(Long l) {
            return l % 2 == 0;
        } };
        ODDS = new Predicate<Long>() { @Override public boolean test(Long l) {
            return l % 2 != 0;
        } };


        GA_FILTER_ODDS = filter(ODDS);
        GA_MAP_TIMES_TWO = map(TIMES_TWO);
        GA_MAP_SQUARED = map(SQUARED);
        GA_FILTER_EVENS = filter(EVENS);

        GATHERED = GA_FILTER_ODDS.andThen(GA_MAP_TIMES_TWO).andThen(GA_MAP_SQUARED).andThen(GA_FILTER_EVENS);
    }

    @Setup
    public void setup() {
        cachedInputArray = new Long[size];
        for(int i = 0;i < size;++i)
            cachedInputArray[i] = Long.valueOf(i);
    }

    @Benchmark
    public long seq_misc_baseline() {
        return Arrays.stream(cachedInputArray)
                .filter(ODDS)
                .map(TIMES_TWO)
                .map(SQUARED)
                .filter(EVENS)
                .collect(findLast()).orElseThrow();
    }

    @Benchmark
    public long seq_misc_gather() {
        return Arrays.stream(cachedInputArray)
                .gather(filter(ODDS))
                .gather(map(TIMES_TWO))
                .gather(map(SQUARED))
                .gather(filter(EVENS))
                .collect(findLast()).orElseThrow();
    }

    @Benchmark
    public long seq_misc_gather_preallocated() {
        return Arrays.stream(cachedInputArray)
                .gather(GA_FILTER_ODDS)
                .gather(GA_MAP_TIMES_TWO)
                .gather(GA_MAP_SQUARED)
                .gather(GA_FILTER_EVENS)
                .collect(findLast()).orElseThrow();
    }

    @Benchmark
    public long seq_misc_gather_composed() {
        return Arrays.stream(cachedInputArray)
                .gather(filter(ODDS)
                        .andThen(map(TIMES_TWO))
                        .andThen(map(SQUARED))
                        .andThen(filter(EVENS))
                )
                .collect(findLast()).orElseThrow();
    }

    @Benchmark
    public long seq_misc_gather_composed_preallocated() {
        return Arrays.stream(cachedInputArray)
                .gather(GA_FILTER_ODDS
                        .andThen(GA_MAP_TIMES_TWO)
                        .andThen(GA_MAP_SQUARED)
                        .andThen(GA_FILTER_EVENS)
                )
                .collect(findLast()).orElseThrow();
    }

    @Benchmark
    public long seq_misc_gather_precomposed() {
        return Arrays.stream(cachedInputArray)
                .gather(GATHERED)
                .collect(findLast()).orElseThrow();
    }
}
