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
}
