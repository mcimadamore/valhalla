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
record EarlyConstructionContext(ClassSymbol owner,
                                boolean onlyWarnings,
                                boolean disallowEarlyReads,
                                boolean ctorPrologue,
                                JCExpression fieldAccessBase) {

    static final EarlyConstructionContext NONE = new EarlyConstructionContext(null, false, false, false, null);

    // Root early contexts

    static EarlyConstructionContext of(ClassSymbol owner,
                                       boolean onlyWarnings,
                                       boolean disallowEarlyReads) {
        return new EarlyConstructionContext(owner, onlyWarnings, disallowEarlyReads, !onlyWarnings,
                null);
    }

    // Derived early contexts (used by Attr)

    EarlyConstructionContext nested(boolean isClass) {
        if (this == NONE) {
            return this;
        }
        return new EarlyConstructionContext(owner, onlyWarnings, true,
                !isClass && ctorPrologue, null);
    }

    EarlyConstructionContext fieldAccess(JCExpression base) {
        if (this == NONE) {
            return this;
        }
        return new EarlyConstructionContext(owner, onlyWarnings, disallowEarlyReads, ctorPrologue,
                base);
    }

    // predicates (used by Resolve)

    boolean shouldTrackEarlyReads() {
        return !onlyWarnings && ctorPrologue;
    }

    boolean allowsFieldRead(VarSymbol field) {
        return !disallowEarlyReads &&
                (owner.isValueClass() ||
                 onlyWarnings || // pretend fields are strict
                 (field.flags_field & HASINIT) == 0);
    }
}
