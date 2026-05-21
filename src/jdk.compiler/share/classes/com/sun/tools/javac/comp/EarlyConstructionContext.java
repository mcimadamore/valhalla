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

package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree.JCExpression;

import static com.sun.tools.javac.code.Flags.HASINIT;
import static com.sun.tools.javac.code.Kinds.Kind.TYP;

/** Early-construction state carried by attribution environments. */
class EarlyConstructionContext {

    static final EarlyConstructionContext NONE =
            new EarlyConstructionContext(null, false, false, false, false, false, null);

    final ClassSymbol owner;
    final boolean onlyWarnings;
    final boolean restricted;
    final boolean ctorPrologue;
    final boolean initializer;

    private final boolean fieldAccessQualifier;
    private final JCExpression fieldAccessBase;

    private EarlyConstructionContext(ClassSymbol owner,
                                     boolean onlyWarnings,
                                     boolean restricted,
                                     boolean ctorPrologue,
                                     boolean initializer,
                                     boolean fieldAccessQualifier,
                                     JCExpression fieldAccessBase) {
        this.owner = owner;
        this.onlyWarnings = onlyWarnings;
        this.restricted = restricted;
        this.ctorPrologue = ctorPrologue;
        this.initializer = initializer;
        this.fieldAccessQualifier = fieldAccessQualifier;
        this.fieldAccessBase = fieldAccessBase;
    }

    static EarlyConstructionContext forConstructor(ClassSymbol owner,
                                                   boolean onlyWarnings,
                                                   boolean restricted) {
        return new EarlyConstructionContext(owner, onlyWarnings, restricted, !onlyWarnings, false, false, null);
    }

    static EarlyConstructionContext forFieldInitializer(VarSymbol field,
                                                       EarlyConstructionContext current,
                                                       boolean allowValueClasses) {
        if (field.owner.kind != TYP || field.isStatic() || !allowValueClasses) {
            return current;
        }
        return new EarlyConstructionContext((ClassSymbol)field.owner, !field.isStrict(), false,
                field.isStrict(), true, false, null);
    }

    EarlyConstructionContext nested() {
        return owner == null ?
                this :
                new EarlyConstructionContext(owner, onlyWarnings, true, false, initializer, false, null);
    }

    EarlyConstructionContext nestedLambda() {
        return owner == null ?
                this :
                new EarlyConstructionContext(owner, onlyWarnings, true, ctorPrologue, initializer, false, null);
    }

    EarlyConstructionContext fieldAccessQualifier() {
        return owner == null ?
                this :
                new EarlyConstructionContext(owner, onlyWarnings, restricted, ctorPrologue, initializer, true, null);
    }

    EarlyConstructionContext fieldAccess(JCExpression base) {
        return owner == null ?
                this :
                new EarlyConstructionContext(owner, onlyWarnings, restricted, ctorPrologue, initializer, false, base);
    }

    EarlyConstructionContext afterConstructorCall() {
        if (owner == null) {
            return this;
        }
        return onlyWarnings ?
                new EarlyConstructionContext(owner, onlyWarnings, restricted, ctorPrologue, initializer, false, null) :
                NONE;
    }

    boolean isActive() {
        return owner != null;
    }

    boolean isFieldAccessQualifier() {
        return fieldAccessQualifier;
    }

    JCExpression fieldAccessBase() {
        return fieldAccessBase;
    }

    boolean shouldRecordFieldReads() {
        return !onlyWarnings && ctorPrologue;
    }

    boolean allowsFieldRead(VarSymbol field) {
        return !restricted &&
                field.owner == owner &&
                (owner.isValueClass() ||
                 (field.flags_field & HASINIT) == 0 ||
                 initializer);
    }
}
