package jdk.incubator.foreign.pointer.api;

import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;

import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public inline class c_int {
    private final int value;

    public c_int(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    static final VarHandle handle = MemoryHandles.varHandle(int.class, ByteOrder.nativeOrder());

    public static final ForeignType<c_int.ref> TYPE = new ForeignType<c_int.ref>() {

        @Override
        public MemoryLayout layout() {
            return MemoryLayouts.JAVA_INT;
        }

        @Override
        c_int.ref get(MemorySegment base, long offset) {
            int value = (int)handle.get(base, offset);
            return new c_int(value);
        }

        @Override
        void set(MemorySegment base, long offset, c_int.ref c_int) {
            handle.set(base, offset, c_int.value());
        }
    };
}
