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

import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.Preview;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.LintWarnings;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import static com.sun.tools.javac.code.Flags.HASINIT;
import static com.sun.tools.javac.code.Flags.STATIC;
import static com.sun.tools.javac.code.Kinds.Kind.MTH;
import static com.sun.tools.javac.code.Kinds.Kind.TYP;
import static com.sun.tools.javac.code.Kinds.Kind.VAR;

/** Policy for early-construction checks. */
class EarlyConstructionContext {

    static final EarlyConstructionContext NONE = new EarlyConstructionContext(
            null, false, false, false, false, false, null, null, null, null, null, null, false, false);

    final ClassSymbol owner;
    final boolean onlyWarnings;
    final boolean restricted;
    final boolean ctorPrologue;
    final boolean initializer;

    private final boolean receiverQualifier;
    private final EarlyConstructionContext outer;
    private final Names names;
    private final Types types;
    private final Preview preview;
    private final Log log;
    private final LocalProxyVarsGen localProxyVarsGen;
    private final boolean allowFlexibleConstructors;
    private final boolean allowValueClasses;

    private EarlyConstructionContext(ClassSymbol owner,
                                     boolean onlyWarnings,
                                     boolean restricted,
                                     boolean ctorPrologue,
                                     boolean initializer,
                                     boolean receiverQualifier,
                                     EarlyConstructionContext outer,
                                     Names names,
                                     Types types,
                                     Preview preview,
                                     Log log,
                                     LocalProxyVarsGen localProxyVarsGen,
                                     boolean allowFlexibleConstructors,
                                     boolean allowValueClasses) {
        this.owner = owner;
        this.onlyWarnings = onlyWarnings;
        this.restricted = restricted;
        this.ctorPrologue = ctorPrologue;
        this.initializer = initializer;
        this.receiverQualifier = receiverQualifier;
        this.outer = outer == null ? NONE : outer;
        this.names = names;
        this.types = types;
        this.preview = preview;
        this.log = log;
        this.localProxyVarsGen = localProxyVarsGen;
        this.allowFlexibleConstructors = allowFlexibleConstructors;
        this.allowValueClasses = allowValueClasses;
    }

    private EarlyConstructionContext dup(boolean restricted,
                                         boolean ctorPrologue,
                                         boolean receiverQualifier) {
        return owner == null ?
                this :
                new EarlyConstructionContext(owner, onlyWarnings, restricted, ctorPrologue,
                        initializer, receiverQualifier, outer, names, types, preview, log,
                        localProxyVarsGen, allowFlexibleConstructors, allowValueClasses);
    }

    static EarlyConstructionContext forConstructor(ClassSymbol owner,
                                                   boolean onlyWarnings,
                                                   boolean restricted,
                                                   EarlyConstructionContext outer,
                                                   Names names,
                                                   Types types,
                                                   Preview preview,
                                                   Log log,
                                                   LocalProxyVarsGen localProxyVarsGen,
                                                   boolean allowFlexibleConstructors,
                                                   boolean allowValueClasses) {
        return new EarlyConstructionContext(owner, onlyWarnings, restricted, !onlyWarnings,
                false, false, outer, names, types, preview, log, localProxyVarsGen,
                allowFlexibleConstructors, allowValueClasses);
    }

    static EarlyConstructionContext forFieldInitializer(VarSymbol field,
                                                       EarlyConstructionContext outer,
                                                       Names names,
                                                       Types types,
                                                       Preview preview,
                                                       Log log,
                                                       LocalProxyVarsGen localProxyVarsGen,
                                                       boolean allowFlexibleConstructors,
                                                       boolean allowValueClasses) {
        if (field.owner.kind != TYP || field.isStatic() || !allowValueClasses) {
            return outer;
        }
        return new EarlyConstructionContext((ClassSymbol)field.owner, !field.isStrict(), false,
                field.isStrict(), true, false, outer, names, types, preview, log, localProxyVarsGen,
                allowFlexibleConstructors, allowValueClasses);
    }

    EarlyConstructionContext nested() {
        if (owner == null) {
            return this;
        }
        return new EarlyConstructionContext(owner, onlyWarnings, true, false,
                initializer, false, outer.nested(), names, types, preview, log,
                localProxyVarsGen, allowFlexibleConstructors, allowValueClasses);
    }

    EarlyConstructionContext nestedLambda() {
        if (owner == null) {
            return this;
        }
        return new EarlyConstructionContext(owner, onlyWarnings, true, ctorPrologue,
                initializer, false, outer.nestedLambda(), names, types, preview, log,
                localProxyVarsGen, allowFlexibleConstructors, allowValueClasses);
    }

    EarlyConstructionContext receiverQualifier() {
        if (owner == null) {
            return this;
        }
        return new EarlyConstructionContext(owner, onlyWarnings, restricted, ctorPrologue,
                initializer, true, outer.receiverQualifier(), names, types, preview, log,
                localProxyVarsGen, allowFlexibleConstructors, allowValueClasses);
    }

    EarlyConstructionContext afterConstructorCall() {
        return owner == null || onlyWarnings ? this : outer;
    }

    boolean isWriteOnlyAssignment(Env<AttrContext> env, JCTree tree) {
        return env.tree instanceof JCAssign assign &&
                TreeInfo.skipParens(assign.lhs) == TreeInfo.skipParens(tree);
    }

    private boolean isDirectEarlyFieldReference(Env<AttrContext> env, JCTree base, VarSymbol field) {
        if (owner == null ||
                field.owner.kind != TYP ||
                field.name == names._this ||
                field.name == names._super ||
                !field.isMemberOf(owner, types) ||
                isUnqualifiedNestedMemberOfCurrentClass(env, base, field)) {
            return false;
        }
        boolean explicitThisBase = base != null && isExplicitThisReference(env, base, null);
        return (field.flags() & STATIC) == 0 ?
                base == null || explicitThisBase :
                explicitThisBase;
    }

    void checkFieldAccess(DiagnosticPosition pos,
                          Env<AttrContext> env,
                          JCTree base,
                          VarSymbol field) {
        if (env.info.attributionMode.isSpeculative) {
            return;
        }
        if (!isDirectEarlyFieldReference(env, base, field)) {
            if (outer != null) {
                outer.checkFieldAccess(pos, env, base, field);
            }
            return;
        }
        preview.checkSourceLevel(pos, Feature.FLEXIBLE_CONSTRUCTORS);
        if ((field.flags() & STATIC) != 0) {
            if (ignoreStaticThisFieldWarning(base)) {
                return;
            }
            reportCantRef(pos, field);
        } else if (restricted || field.owner != owner) {
            reportCantRef(pos, field);
        } else if (!owner.isValueClass() &&
                (field.flags_field & HASINIT) != 0 &&
                !initializer) {
            reportCantRef(pos, field);
        } else {
            recordFieldRead(env, field);
        }
    }

    void checkFieldAssignment(DiagnosticPosition pos,
                              Env<AttrContext> env,
                              JCTree base,
                              VarSymbol field) {
        if (env.info.attributionMode.isSpeculative) {
            return;
        }
        if (!isDirectEarlyFieldReference(env, base, field)) {
            if (outer != null) {
                outer.checkFieldAssignment(pos, env, base, field);
            }
            return;
        }
        if (!allowFlexibleConstructors &&
                base == null &&
                (field.flags() & STATIC) == 0 &&
                (restricted || field.owner != owner) &&
                (field.flags_field & HASINIT) == 0) {
            return;
        }
        if (base != null) {
            preview.checkSourceLevel(pos, Feature.FLEXIBLE_CONSTRUCTORS);
        }
        if ((field.flags() & STATIC) != 0) {
            if (ignoreStaticThisFieldWarning(base)) {
                return;
            }
            if (base == null) {
                preview.checkSourceLevel(pos, Feature.FLEXIBLE_CONSTRUCTORS);
            }
            reportCantRef(pos, field);
        } else if (restricted || field.owner != owner) {
            if (base == null) {
                preview.checkSourceLevel(pos, Feature.FLEXIBLE_CONSTRUCTORS);
            }
            reportCantRef(pos, field);
        } else if ((field.flags_field & HASINIT) != 0) {
            if (base == null) {
                preview.checkSourceLevel(pos, Feature.FLEXIBLE_CONSTRUCTORS);
            }
            reportCantAssignInitialized(pos, field);
        }
    }

    void checkInstanceMemberUse(DiagnosticPosition pos, Env<AttrContext> env, JCTree base, Symbol sym) {
        if (env.info.attributionMode.isSpeculative) {
            return;
        }
        if (!isDirectInstanceMember(env, base, sym)) {
            if (outer != null) {
                outer.checkInstanceMemberUse(pos, env, base, sym);
            }
            return;
        }
        preview.checkSourceLevel(pos, Feature.FLEXIBLE_CONSTRUCTORS);
        reportCantRef(pos, sym);
    }

    void checkExplicitThis(DiagnosticPosition pos, Env<AttrContext> env, JCTree tree, Symbol sym) {
        if (env.info.attributionMode.isSpeculative) {
            return;
        }
        if (owner == null ||
                receiverQualifier ||
                !isExplicitThisReference(env, tree, sym)) {
            if (outer != null) {
                outer.checkExplicitThis(pos, env, tree, sym);
            }
            return;
        }
        preview.checkSourceLevel(pos, Feature.FLEXIBLE_CONSTRUCTORS);
        reportCantRef(pos, sym);
    }

    void checkImplicitThis(DiagnosticPosition pos, Env<AttrContext> env, Symbol sym, Symbol site) {
        if (env.info.attributionMode.isSpeculative) {
            return;
        }
        if (owner == null || sym.owner != owner || site != owner) {
            if (outer != null) {
                outer.checkImplicitThis(pos, env, sym, site);
            }
            return;
        }
        preview.checkSourceLevel(pos, Feature.FLEXIBLE_CONSTRUCTORS);
        reportCantRef(pos, site);
    }

    void checkDefaultSuper(DiagnosticPosition pos, Env<AttrContext> env, Name name) {
        if (owner == null || receiverQualifier || env.info.attributionMode.isSpeculative) {
            return;
        }
        preview.checkSourceLevel(pos, Feature.FLEXIBLE_CONSTRUCTORS);
        reportCantRef(pos, name);
    }

    void checkInnerClassCreation(DiagnosticPosition pos, Env<AttrContext> env, Type type) {
        if (env.info.attributionMode.isSpeculative) {
            return;
        }
        if (owner == null) {
            return;
        }
        if (type.tsym.isInner() &&
                type.tsym.isEnclosedBy(owner) &&
                !type.tsym.isDirectlyOrIndirectlyLocal()) {
            preview.checkSourceLevel(pos, Feature.FLEXIBLE_CONSTRUCTORS);
            reportCantRef(pos, type.getEnclosingType().tsym);
        } else if (outer != null) {
            outer.checkInnerClassCreation(pos, env, type);
        }
    }

    private boolean isDirectInstanceMember(Env<AttrContext> env, JCTree base, Symbol sym) {
        if (owner == null ||
                sym == null ||
                (sym.kind != VAR && sym.kind != MTH) ||
                sym.owner.kind != TYP ||
                !sym.isMemberOf(owner, types) ||
                isUnqualifiedNestedMemberOfCurrentClass(env, base, sym)) {
            return false;
        }
        boolean explicitThisBase = base != null && isExplicitThisReference(env, base, null);
        return (sym.flags() & STATIC) == 0 ?
                base == null || explicitThisBase :
                sym.kind == MTH && explicitThisBase;
    }

    private boolean ignoreStaticThisFieldWarning(JCTree base) {
        return onlyWarnings &&
                base != null &&
                TreeInfo.isThisOrSelectorDotThis(base);
    }

    private boolean isExplicitThisReference(Env<AttrContext> env, JCTree tree, Symbol sym) {
        tree = TreeInfo.skipParens(tree);
        if (tree.hasTag(JCTree.Tag.IDENT)) {
            Name name = ((JCTree.JCIdent)tree).name;
            if (name != names._this && name != names._super) {
                return false;
            }
            return sym != null ?
                    sym.owner == owner :
                    env.enclClass.sym == owner ||
                    tree.type != null &&
                            TreeInfo.isExplicitThisReference(types, (ClassType)owner.type, tree);
        } else if (tree.hasTag(JCTree.Tag.SELECT)) {
            JCTree.JCFieldAccess select = (JCTree.JCFieldAccess)tree;
            if (select.selected.type == null) {
                return false;
            }
            return TreeInfo.isExplicitThisReference(types, (ClassType)owner.type, tree);
        } else {
            return false;
        }
    }

    private boolean isUnqualifiedNestedMemberOfCurrentClass(Env<AttrContext> env, JCTree base, Symbol sym) {
        return base == null &&
                env.enclClass.sym != owner &&
                sym.isMemberOf(env.enclClass.sym, types);
    }

    private void reportCantRef(DiagnosticPosition pos, Symbol sym) {
        if (onlyWarnings) {
            log.warning(pos, LintWarnings.WouldNotBeAllowedInPrologue(sym));
        } else {
            log.error(pos, Errors.CantRefBeforeCtorCalled(sym));
        }
    }

    private void reportCantRef(DiagnosticPosition pos, Name name) {
        if (onlyWarnings) {
            log.warning(pos, LintWarnings.WouldNotBeAllowedInPrologue(name));
        } else {
            log.error(pos, Errors.CantRefBeforeCtorCalled(name));
        }
    }

    private void reportCantAssignInitialized(DiagnosticPosition pos, Symbol sym) {
        if (onlyWarnings) {
            log.warning(pos, LintWarnings.WouldNotBeAllowedInPrologue(sym));
        } else {
            log.error(pos, Errors.CantAssignInitializedBeforeCtorCalled(sym));
        }
    }

    private void recordFieldRead(Env<AttrContext> env, VarSymbol field) {
        if (!onlyWarnings &&
                ctorPrologue &&
                localProxyVarsGen != null &&
                env.enclMethod != null &&
                TreeInfo.isConstructor(env.enclMethod)) {
            localProxyVarsGen.addFieldReadInPrologue(env.enclMethod, field);
        }
    }
}
