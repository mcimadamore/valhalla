/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/*
 * @test
 * @enablePreview
 * @modules java.base/jdk.internal.value
 * @summary Smoke test for value class support
 * @run junit TestUnsignedInt
 */

import jdk.internal.value.ValueClass;
import org.junit.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TestUnsignedInt {
    @Test
    public void testUnsignedInt() {
        UnsignedInt[] arr = (UnsignedInt[])ValueClass.newNullRestrictedNonAtomicArray(UnsignedInt.class, 10, UnsignedInt.valueOf(0));
        MemorySegment seg = MemorySegment.ofArray(arr);

        assertEquals(40, seg.byteSize());
        assertEquals(UnsignedInt.ZERO, seg.getAtIndex(ValueLayout.JAVA_UNSIGNED_INT, 0));
        assertEquals(UnsignedInt.ZERO, seg.getAtIndex(ValueLayout.JAVA_UNSIGNED_INT, 9));

        int[] alt = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        MemorySegment.copy(alt, 0, seg, ValueLayout.JAVA_INT, 0, 10);

        assertEquals(UnsignedInt.ZERO, seg.getAtIndex(ValueLayout.JAVA_UNSIGNED_INT, 0));
        assertEquals(UnsignedInt.valueOf(9), seg.getAtIndex(ValueLayout.JAVA_UNSIGNED_INT, 9));

        for (int i = 0 ; i < 10 ; i++) {
            arr[9 - i] = UnsignedInt.valueOf(i);
        }
        MemorySegment.copy(arr, 0, seg, ValueLayout.JAVA_UNSIGNED_INT, 0, 10);

        assertEquals(UnsignedInt.valueOf(9), seg.getAtIndex(ValueLayout.JAVA_UNSIGNED_INT, 0));
        assertEquals(UnsignedInt.ZERO, seg.getAtIndex(ValueLayout.JAVA_UNSIGNED_INT, 9));

        arr = (UnsignedInt[])ValueClass.newNullRestrictedNonAtomicArray(UnsignedInt.class, 10, UnsignedInt.valueOf(0));
        MemorySegment.copy(seg, ValueLayout.JAVA_UNSIGNED_INT, 0, arr, 0, 10);

        assertEquals(UnsignedInt.valueOf(9), arr[0]);
        assertEquals(UnsignedInt.ZERO, arr[9]);

        seg = Arena.ofAuto().allocateFrom(ValueLayout.JAVA_UNSIGNED_INT,
                UnsignedInt.valueOf(0), UnsignedInt.valueOf(1), UnsignedInt.valueOf(2), UnsignedInt.valueOf(3), UnsignedInt.valueOf(4),  UnsignedInt.valueOf(5),
                UnsignedInt.valueOf(6), UnsignedInt.valueOf(7), UnsignedInt.valueOf(8), UnsignedInt.valueOf(9));

        assertEquals(40, seg.byteSize());
        assertEquals(UnsignedInt.ZERO, seg.getAtIndex(ValueLayout.JAVA_UNSIGNED_INT, 0));
        assertEquals(UnsignedInt.valueOf(9), seg.getAtIndex(ValueLayout.JAVA_UNSIGNED_INT, 9));

        arr = seg.toArray(ValueLayout.JAVA_UNSIGNED_INT);
        assertEquals(10, arr.length);
        assertEquals(UnsignedInt.ZERO, arr[0]);
        assertEquals(UnsignedInt.valueOf(9), arr[9]);
    }

    @Test
    public void testUnsignedConversion() {
        MemorySegment segment = Arena.ofAuto().allocateFrom(ValueLayout.JAVA_INT, -1);
        assertEquals(UnsignedInt.MAX_VALUE, segment.get(ValueLayout.JAVA_UNSIGNED_INT, 0));
    }

    @Test
    public void testBadOfArray() {
        assertThrows(IllegalArgumentException.class,
                () -> MemorySegment.ofArray(new UnsignedInt[10])); // not a flat array
    }

    @Test
    public void testBadArraySourceSegmentCopy() {
        MemorySegment to = Arena.ofAuto().allocate(ValueLayout.JAVA_UNSIGNED_INT, 10);
        assertThrows(IllegalArgumentException.class,
                () -> MemorySegment.copy(new UnsignedInt[10], 0, to, ValueLayout.JAVA_UNSIGNED_INT, 0, 10)); // not a flat array source
    }

    @Test
    public void testBadArrayTargetSegmentCopy() {
        MemorySegment from = Arena.ofAuto().allocate(ValueLayout.JAVA_UNSIGNED_INT, 10);
        assertThrows(IllegalArgumentException.class,
                () -> MemorySegment.copy(from, ValueLayout.JAVA_UNSIGNED_INT, 0, new UnsignedInt[10], 0, 10)); // not a flat array target
    }
}
