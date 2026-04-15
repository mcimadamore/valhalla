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

package java.lang;

import jdk.internal.MigratedValueClass;
import jdk.internal.ValueBased;
import jdk.internal.vm.annotation.LooselyConsistentValue;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.HexFormat;
import java.util.Objects;

@ValueBased
@MigratedValueClass
@LooselyConsistentValue
/* value */ class Bytes128 {
    private final long lo;
    private final long hi;

    private Bytes128(long lo, long hi) {
        this.lo = lo;
        this.hi = hi;
    }

    public byte getByte(int index) {
        Objects.checkIndex(index, 16);
        return (byte) (index < 8
                ? lo >>> (index << 3)
                : hi >>> ((index - 8) << 3));
    }

    public short getShort(int index) {
        Objects.checkIndex(index, 8);
        return (short) (index < 4
                ? lo >>> (index << 4)
                : hi >>> ((index - 4) << 4));
    }

    public int getInt(int index) {
        Objects.checkIndex(index, 4);
        return (int) (index < 2
                ? lo >>> (index << 5)
                : hi >>> ((index - 2) << 5));
    }

    public long getLong(int index) {
        Objects.checkIndex(index, 2);
        return index == 0 ? lo : hi;
    }

    public MemorySegment toSegment(ValueLayout laneLayout) {
        Objects.requireNonNull(laneLayout);
        return switch (laneLayout) {
            case ValueLayout.OfByte _ -> MemorySegment.ofArray(new byte[]{
                    getByte(0), getByte(1), getByte(2), getByte(3),
                    getByte(4), getByte(5), getByte(6), getByte(7),
                    getByte(8), getByte(9), getByte(10), getByte(11),
                    getByte(12), getByte(13), getByte(14), getByte(15)}).asReadOnly();
            case ValueLayout.OfShort ofShort -> MemorySegment.ofArray(new short[]{
                    maybeSwap(getShort(0), ofShort), maybeSwap(getShort(1), ofShort), maybeSwap(getShort(2), ofShort), maybeSwap(getShort(3), ofShort),
                    maybeSwap(getShort(4), ofShort), maybeSwap(getShort(5), ofShort), maybeSwap(getShort(6), ofShort), maybeSwap(getShort(7), ofShort)}).asReadOnly();
            case ValueLayout.OfInt ofInt -> MemorySegment.ofArray(new int[]{
                    maybeSwap(getInt(0), ofInt), maybeSwap(getInt(1), ofInt), maybeSwap(getInt(2), ofInt), maybeSwap(getInt(3), ofInt)}).asReadOnly();
            case ValueLayout.OfLong ofLong -> MemorySegment.ofArray(new long[]{
                    maybeSwap(getLong(0), ofLong), maybeSwap(getLong(1), ofLong)}).asReadOnly();
            default ->
                 throw new IllegalArgumentException("Unsupported lane type: " + laneLayout);
        };
    }

    private short maybeSwap(short s, ValueLayout.OfShort ofShort) {
        return ofShort.order() == ByteOrder.nativeOrder() ? s : Short.reverseBytes(s);
    }

    private int maybeSwap(int i, ValueLayout.OfInt ofInt) {
        return ofInt.order() == ByteOrder.nativeOrder() ? i : Integer.reverseBytes(i);
    }

    private long maybeSwap(long l, ValueLayout.OfLong ofLong) {
        return ofLong.order() == ByteOrder.nativeOrder() ? l : Long.reverseBytes(l);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Bytes128 that &&
                lo == that.lo &&
                hi == that.hi;
    }

    @Override
    public int hashCode() {
        return 31 * Long.hashCode(lo) + Long.hashCode(hi);
    }

    @Override
    public String toString() {
        HexFormat hex = HexFormat.of();
        return hex.toHexDigits(Long.reverseBytes(lo)) +
                hex.toHexDigits(Long.reverseBytes(hi));
    }

    public static Bytes128 pack(long lo, long hi) {
        return new Bytes128(lo, hi);
    }

    public static Bytes128 pack(int i1, int i2, int i3, int i4) {
        return new Bytes128(
                Integer.toUnsignedLong(i1) | Integer.toUnsignedLong(i2) << 32,
                Integer.toUnsignedLong(i3) | Integer.toUnsignedLong(i4) << 32);
    }

    public static Bytes128 pack(short s1, short s2, short s3, short s4,
                              short s5, short s6, short s7, short s8) {
        return new Bytes128(
                (long)Short.toUnsignedInt(s1) | (long)Short.toUnsignedInt(s2) << 16 | (long)Short.toUnsignedInt(s3) << 32 | (long)Short.toUnsignedInt(s4) << 48,
                (long)Short.toUnsignedInt(s5) | (long)Short.toUnsignedInt(s6) << 16 | (long)Short.toUnsignedInt(s7) << 32 | (long)Short.toUnsignedInt(s8) << 48);
    }

    public static Bytes128 pack(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7, byte b8,
                              byte b9, byte b10, byte b11, byte b12, byte b13, byte b14, byte b15, byte b16) {
        return new Bytes128(
                Byte.toUnsignedLong(b1) | Byte.toUnsignedLong(b2) << 8 | Byte.toUnsignedLong(b3) << 16 | Byte.toUnsignedLong(b4) << 24 |
                        Byte.toUnsignedLong(b5) << 32 | Byte.toUnsignedLong(b6) << 40 | Byte.toUnsignedLong(b7) << 48 | Byte.toUnsignedLong(b8) << 56,
                Byte.toUnsignedLong(b9) | Byte.toUnsignedLong(b10) << 8 | Byte.toUnsignedLong(b11) << 16 | Byte.toUnsignedLong(b12) << 24 |
                        Byte.toUnsignedLong(b13) << 32 | Byte.toUnsignedLong(b14) << 40 | Byte.toUnsignedLong(b15) << 48 | Byte.toUnsignedLong(b16) << 56);
    }

    public static Bytes128 fromSegment(MemorySegment segment, long offset, ValueLayout laneLayout) {
        Objects.requireNonNull(segment);
        Objects.requireNonNull(laneLayout);
        switch (laneLayout) {
            case ValueLayout.OfByte ofByte -> {
                byte b1 = segment.get(ofByte, offset);
                byte b2 = segment.get(ofByte, offset + 1);
                byte b3 = segment.get(ofByte, offset + 2);
                byte b4 = segment.get(ofByte, offset + 3);
                byte b5 = segment.get(ofByte, offset + 4);
                byte b6 = segment.get(ofByte, offset + 5);
                byte b7 = segment.get(ofByte, offset + 6);
                byte b8 = segment.get(ofByte, offset + 7);
                byte b9 = segment.get(ofByte, offset + 8);
                byte b10 = segment.get(ofByte, offset + 9);
                byte b11 = segment.get(ofByte, offset + 10);
                byte b12 = segment.get(ofByte, offset + 11);
                byte b13 = segment.get(ofByte, offset + 12);
                byte b14 = segment.get(ofByte, offset + 13);
                byte b15 = segment.get(ofByte, offset + 14);
                byte b16 = segment.get(ofByte, offset + 15);
                return Bytes128.pack(b1, b2, b3, b4, b5, b6, b7, b8,
                        b9, b10, b11, b12, b13, b14, b15, b16);
            }
            case ValueLayout.OfShort ofShort -> {
                short s1 = segment.get(ofShort, offset);
                short s2 = segment.get(ofShort, ofShort.scale(offset, 1));
                short s3 = segment.get(ofShort, ofShort.scale(offset, 2));
                short s4 = segment.get(ofShort, ofShort.scale(offset, 3));
                short s5 = segment.get(ofShort, ofShort.scale(offset, 4));
                short s6 = segment.get(ofShort, ofShort.scale(offset, 5));
                short s7 = segment.get(ofShort, ofShort.scale(offset, 6));
                short s8 = segment.get(ofShort, ofShort.scale(offset, 7));
                return Bytes128.pack(s1, s2, s3, s4, s5, s6, s7, s8);
            }
            case ValueLayout.OfInt ofInt -> {
                int i1 = segment.get(ofInt, offset);
                int i2 = segment.get(ofInt, ofInt.scale(offset, 1));
                int i3 = segment.get(ofInt, ofInt.scale(offset, 2));
                int i4 = segment.get(ofInt, ofInt.scale(offset, 3));
                return Bytes128.pack(i1, i2, i3, i4);
            }
            case ValueLayout.OfLong ofLong -> {
                long l1 = segment.get(ofLong, offset);
                long l2 = segment.get(ofLong, ofLong.scale(offset, 1));
                return Bytes128.pack(l1, l2);
            }
            default -> throw new IllegalArgumentException("Unsupported lane type: " + laneLayout);
        }
    }
}
