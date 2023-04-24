/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang.foreign;

import java.lang.foreign.ComplexDouble;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout.*;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import sun.misc.Unsafe;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import java.util.*;

import static java.lang.foreign.ValueLayout.*;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsAppend = { "--enable-preview", "--enable-native-access=ALL-UNNAMED" })
public class LoopOverNonConstantComplexDouble extends JavaLayouts {

    static final Unsafe unsafe = Utils.unsafe;

    static final int ELEM_SIZE = 1_000_000;
    static final int HALF_ELEM_SIZE = ELEM_SIZE / 2;
    static final int CARRIER_SIZE = (int)JAVA_DOUBLE.byteSize();
    static final int ALLOC_SIZE = ELEM_SIZE * CARRIER_SIZE;

    Arena arena;
    MemorySegment segment;

    @Setup
    public void setup() {
        arena = Arena.openConfined();
        segment = MemorySegment.allocateNative(ALLOC_SIZE, arena.scope());
        for (int i = 0; i < ELEM_SIZE; i++) {
            segment.setAtIndex(JAVA_DOUBLE, i, i);
        }

        // sanity check

        List<Double> expected = new ArrayList<>();
        for (int i = 0; i < ELEM_SIZE; i++) {
            expected.add(segment.getAtIndex(JAVA_DOUBLE, i));
        }

        List<Double> actuals = new ArrayList<>();
        for (int i = 0; i < HALF_ELEM_SIZE; i++) {
            ComplexDouble cd = segment.getAtIndex(COMPLEX_DOUBLE, i);
            actuals.add(cd.re());
            actuals.add(cd.im());
        }

        if (!actuals.equals(expected)) {
            throw new AssertionError();
        }
    }

    @TearDown
    public void tearDown() {
        arena.close();
    }

    @Benchmark
    public long segment_loop_double() {
        long sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += (long)segment.getAtIndex(JAVA_DOUBLE, i);
        }
        return sum;
    }

    @Benchmark
    public double segment_loop_complex_double() {
        long sum = 0;
        for (int i = 0; i < HALF_ELEM_SIZE; i++) {
            ComplexDouble cd = segment.getAtIndex(COMPLEX_DOUBLE, i);
            sum += (long)cd.re();
            sum += (long)cd.im();
        }
        return sum;
    }
}
