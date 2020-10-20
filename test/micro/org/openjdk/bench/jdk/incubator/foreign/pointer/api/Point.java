package jdk.incubator.foreign.pointer.api;

import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

public inline class Point {
    
    private final MemorySegment segment;

    Point(MemorySegment segment) {
        this.segment = segment;
    }

    static final MemoryLayout LAYOUT = MemoryLayout.ofStruct(
            c_int.TYPE.layout().withName("x"),
            c_int.TYPE.layout().withName("y"));
    
    static final long X_OFFSET = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("x"));
    static final long Y_OFFSET = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("y"));
    
    public c_int x$get() {
        return c_int.TYPE.get(segment, X_OFFSET);
    }

    public c_int y$get() {
        return c_int.TYPE.get(segment, Y_OFFSET);
    }

    public void x$set(c_int x) {
        c_int.TYPE.set(segment, X_OFFSET, x);
    }

    public void y$set(c_int y) {
        c_int.TYPE.set(segment, Y_OFFSET, y);
    }

    static inline class PointType extends ForeignType<Point.ref> {
        @Override
        public MemoryLayout layout() {
            return LAYOUT;
        }

        @Override
        Point.ref get(MemorySegment base, long offset) {
            MemorySegment segment = pointSlice(base, offset);
            return new Point(segment);
        }

        @Override
        void set(MemorySegment address, long offset, Point.ref p) {
            pointSlice(address, offset).copyFrom(p.segment);
        }

        static MemorySegment pointSlice(MemorySegment base, long offset) {
            return base.asSlice(offset, LAYOUT.byteSize());
        }
    };

    public static ForeignType<Point.ref> TYPE = new PointType();
}
