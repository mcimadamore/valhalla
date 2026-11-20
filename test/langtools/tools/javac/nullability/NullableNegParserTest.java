/*
 * @test /nodynamiccopyright/
 * @enablePreview
 * @summary smoke test for negative parser test for null restrictions
 * @compile/fail/ref=NullableNegParserTest.out -XDrawDiagnostics NullableNegParserTest.java
 */

class NullableNegParserTest {
    static class Foo extends Bar? { } // not valid, superclass
    static class Foo implements Bar? { } // not valid, superinterface

    class Foo<X extends String?> { } // not valid, type-param bound
    <X extends String?> void foo() { } // not valid, type-param bound

    void foo() throws Error? { } // not valid, throws type

    void testNew() {
        new Foo?(); // bad, class creation expression
    }

    void testNewArray() {
        var z = new Foo?[2]; // ok
        var y = new Foo?[2]?; // bad, ques can't appear at the end
        var x = new Foo?[2][][][]?; // bad, ques can't appear at the end
        var x = new Foo?[2][]?[][]?; // bad, ques can't appear in the middle or at the end
        var x = new Foo?[2][1][1][1]?; // bad, ques can't appear at the end
        var x = new Foo?[2][1]?[1][1]?; // bad, ques can't appear at the end
        var x = new Bar?.Foo?[2][1]?[1][1]?; // bad, ques can't appear at the end, and bad qualifier
    }

    void testNewArrayWithInit() {
        var z = new Foo?[] { null }; // ok
        var y = new Foo?[]? { null }; // bad, ques can't appear at the end
        var x = new Foo?[]?[] { null }; // bad, ques can't appear in the middle
        var x = new Foo?[]?[]? { null }; // bad, ques can't appear in the middle or at the end
    }

    void testNoBangInQualifiedTypeNames() {
        a?.x x = ""; // bad, ques before '.'
        a?.b?.x x = ""; // bad, ques before '.'
        a?.m(); // bad, ques before '.'
        a.b?.m(); // bad, ques before '.'
    }

    void testNoBangInMrefQualifier() {
        Runnable r = Foo?<String>::m;
        Runnable r = Foo<String?>::m;
        Runnable r = Foo<String>?::m;
        Runnable r = Foo<String>?[]::m;
        Runnable r = Foo<String>?[]?::m;
        Runnable r = Foo<String>.Foo?<Integer>::m;
        Runnable r = Foo<String>.Foo<Integer?>::m;
        Runnable r = Foo<String>.Foo<Integer>?::m;
        Runnable r = Foo<String>.Foo?<Integer>::m;
        Runnable r = Foo<String>.Foo<Integer?>::m;
        Runnable r = Foo<String>.Foo<Integer>?[]::m;
        Runnable r = Foo<String>.Foo<Integer>?[]?::m;
    }

    void testInnerClassCreator() {
        encl.new Foo?();
        encl.new Foo<String?>();
        encl.new Foo<String>?();
    }

    void testQualifiedType() {
        A<String>?.B<Integer> a;
        A?<String>.B<Integer> a;
        A<String>.B?<Integer> a;
        A<String>.B<Integer?> a;
        A<String?>.B<Integer> a;
    }

    static class TestConstructor {
        TestConstructor?() { } // bad, no ques in constructor type
    }

    void testVarargs(Foo?... args) { // bad, quess and varargs
        Foo foo = (A a, B?... bs) -> { }; // bad, quess and varargs
        record R(A a, B?... bs) { } // bad, quess and varargs
    }
}
