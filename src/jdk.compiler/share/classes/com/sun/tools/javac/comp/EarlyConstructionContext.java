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

/** Early-construction state carried by attribution environments. */
class EarlyConstructionContext {

    // A dummy early construction context
    static final EarlyConstructionContext NONE =
            new EarlyConstructionContext(null, false, false, false, false, false, null) {
                @Override
                EarlyConstructionContext nested(boolean isClass) {
                    return this;
                }

                @Override
                EarlyConstructionContext fieldAccessQualifier() {
                    return this;
                }

                @Override
                EarlyConstructionContext fieldAccess(JCExpression base) {
                    return this;
                }
            };

    final ClassSymbol owner;
    final boolean onlyWarnings;
    final boolean disallowEarlyReads;
    final boolean ctorPrologue;
    final boolean initializer;

    private final boolean fieldAccessQualifier;
    private final JCExpression fieldAccessBase;

    private EarlyConstructionContext(ClassSymbol owner,
                                     boolean onlyWarnings,
                                     boolean disallowEarlyReads,
                                     boolean ctorPrologue,
                                     boolean initializer,
                                     boolean fieldAccessQualifier,
                                     JCExpression fieldAccessBase) {
        this.owner = owner;
        this.onlyWarnings = onlyWarnings;
        this.disallowEarlyReads = disallowEarlyReads;
        this.ctorPrologue = ctorPrologue;
        this.initializer = initializer;
        this.fieldAccessQualifier = fieldAccessQualifier;
        this.fieldAccessBase = fieldAccessBase;
    }

    // Root early contexts

    static EarlyConstructionContext ofConstructor(ClassSymbol owner,
                                                  boolean onlyWarnings,
                                                  boolean disallowEarlyReads) {
        return new EarlyConstructionContext(owner, onlyWarnings, disallowEarlyReads, !onlyWarnings,
                false, false, null);
    }

    static EarlyConstructionContext ofFieldInitializer(VarSymbol field) {
        return new EarlyConstructionContext((ClassSymbol)field.owner, !field.isStrict(), false,
                field.isStrict(), true, false, null);
    }

    // Derived early contexts (used by Attr)

    EarlyConstructionContext nested(boolean isClass) {
        return new EarlyConstructionContext(owner, onlyWarnings, true,
                !isClass && ctorPrologue, initializer, false, null);
    }

    EarlyConstructionContext fieldAccessQualifier() {
        return new EarlyConstructionContext(owner, onlyWarnings, disallowEarlyReads, ctorPrologue,
                initializer, true, null);
    }

    EarlyConstructionContext fieldAccess(JCExpression base) {
        return new EarlyConstructionContext(owner, onlyWarnings, disallowEarlyReads, ctorPrologue,
                initializer, false, base);
    }

    // predicates (used by Resolve)

    boolean isFieldAccessQualifier() {
        return fieldAccessQualifier;
    }

    JCExpression fieldAccessBase() {
        return fieldAccessBase;
    }

    boolean shouldTrackEarlyReads() {
        return !onlyWarnings && ctorPrologue;
    }

    boolean allowsFieldRead(VarSymbol field) {
        return !disallowEarlyReads &&
                field.owner == owner &&
                (owner.isValueClass() ||
                 onlyWarnings || // pretend fields are strict
                 (field.flags_field & HASINIT) == 0 ||
                 initializer);
    }
}
