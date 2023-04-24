/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.invoke;

import java.lang.foreign.ComplexDouble;
import jdk.internal.foreign.AbstractMemorySegmentImpl;
import jdk.internal.misc.ScopedMemoryAccess;
import jdk.internal.vm.annotation.ForceInline;

import java.lang.foreign.MemorySegment;

import java.util.Objects;

import static java.lang.invoke.MethodHandleStatics.UNSAFE;

final class VarHandleSegmentAsComplexDoubles extends VarHandleSegmentViewBase {

    static final boolean BE = UNSAFE.isBigEndian();

//    static final long RE_FIELD = UNSAFE.objectFieldOffset(ComplexDouble.class, "re");
//    static final long IM_FIELD = UNSAFE.objectFieldOffset(ComplexDouble.class, "im");
//
//    static final ComplexDouble DEFAULT_VALUE = UNSAFE.uninitializedDefaultValue(ComplexDouble.class);

    static final ScopedMemoryAccess SCOPED_MEMORY_ACCESS = ScopedMemoryAccess.getScopedMemoryAccess();

    static final int VM_ALIGN = Double.BYTES - 1;

    static final VarForm FORM = new VarForm(VarHandleSegmentAsComplexDoubles.class, MemorySegment.class, ComplexDouble.class, long.class);

    VarHandleSegmentAsComplexDoubles(boolean be, long length, long alignmentMask, boolean exact) {
        super(FORM, be, length, alignmentMask, exact);
    }

    @Override
    final MethodType accessModeTypeUncached(VarHandle.AccessType accessType) {
        return accessType.accessModeType(MemorySegment.class, ComplexDouble.class, long.class);
    }

    @Override
    public VarHandleSegmentAsComplexDoubles withInvokeExactBehavior() {
        return hasInvokeExactBehavior() ?
                this :
                new VarHandleSegmentAsComplexDoubles(be, length, alignmentMask, true);
    }

    @Override
    public VarHandleSegmentAsComplexDoubles withInvokeBehavior() {
        return !hasInvokeExactBehavior() ?
                this :
                new VarHandleSegmentAsComplexDoubles(be, length, alignmentMask, false);
    }

    @ForceInline
    static long convEndian(boolean big, double v) {
        long rv = Double.doubleToRawLongBits(v);
        return big == BE ? rv : Long.reverseBytes(rv);
    }

    @ForceInline
    static double convEndian(boolean big, long rv) {
        rv = big == BE ? rv : Long.reverseBytes(rv);
        return Double.longBitsToDouble(rv);
    }

    @ForceInline
    static AbstractMemorySegmentImpl checkAddress(Object obb, long offset, long length, boolean ro) {
        AbstractMemorySegmentImpl oo = (AbstractMemorySegmentImpl)Objects.requireNonNull(obb);
        oo.checkAccess(offset, length, ro);
        return oo;
    }

    @ForceInline
    static long offset(AbstractMemorySegmentImpl bb, long offset, long alignmentMask) {
        long address = offsetNoVMAlignCheck(bb, offset, alignmentMask);
        if ((address & VM_ALIGN) != 0) {
            throw VarHandleSegmentViewBase.newIllegalArgumentExceptionForMisalignedAccess(address);
        }
        return address;
    }

    @ForceInline
    static long offsetNoVMAlignCheck(AbstractMemorySegmentImpl bb, long offset, long alignmentMask) {
        long base = bb.unsafeGetOffset();
        long address = base + offset;
        long maxAlignMask = bb.maxAlignMask();
        if (((address | maxAlignMask) & alignmentMask) != 0) {
            throw VarHandleSegmentViewBase.newIllegalArgumentExceptionForMisalignedAccess(address);
        }
        return address;
    }

    @ForceInline
    static Object get(VarHandle ob, Object obb, long base) {
        VarHandleSegmentViewBase handle = (VarHandleSegmentViewBase)ob;
        AbstractMemorySegmentImpl bb = checkAddress(obb, base, handle.length, true);
        // alignment check should be performed only once
        long offset = offsetNoVMAlignCheck(bb, base, handle.alignmentMask);
        long rawValue = SCOPED_MEMORY_ACCESS.getLongUnaligned(bb.sessionImpl(),
                bb.unsafeGetBase(),
                offset,
                handle.be);
        double re = Double.longBitsToDouble(rawValue);
        rawValue = SCOPED_MEMORY_ACCESS.getLongUnaligned(bb.sessionImpl(),
                bb.unsafeGetBase(),
                offset + 8,
                handle.be);

        double im = Double.longBitsToDouble(rawValue);
//        ComplexDouble buf = UNSAFE.makePrivateBuffer(DEFAULT_VALUE);
//        UNSAFE.putDouble(buf, RE_FIELD, re);
//        UNSAFE.putDouble(buf, IM_FIELD, im);
//        return UNSAFE.finishPrivateBuffer(buf);
        return new ComplexDouble(re, im);
    }

    @ForceInline
    static void set(VarHandle ob, Object obb, long base, Object value) {
        VarHandleSegmentViewBase handle = (VarHandleSegmentViewBase)ob;
        AbstractMemorySegmentImpl bb = checkAddress(obb, base, handle.length, false);
        // alignment check should be performed only once
        long offset = offsetNoVMAlignCheck(bb, base, handle.alignmentMask);
        ComplexDouble cd = (ComplexDouble)value;
        double re = cd.re();
        double im = cd.im();
        SCOPED_MEMORY_ACCESS.putLongUnaligned(bb.sessionImpl(),
                bb.unsafeGetBase(),
                offset,
                Double.doubleToRawLongBits(re),
                handle.be);
        SCOPED_MEMORY_ACCESS.putLongUnaligned(bb.sessionImpl(),
                bb.unsafeGetBase(),
                offset + 8,
                Double.doubleToRawLongBits(im),
                handle.be);
    }
}
