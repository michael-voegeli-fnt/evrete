package org.evrete.dsl;

import org.evrete.api.*;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.PhaseListener;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.RuleSet;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.evrete.dsl.JavaDSLSourceProvider.LOGGER;

class JavaClassRuleSet {
    private final RuntimeContext<?> runtime;
    private final List<RuleMethod> ruleMethods;
    private final Class<?> ruleSetClass;
    private final Object instance;

    JavaClassRuleSet(RuntimeContext<?> runtime, Class<?> clazz) {
        MethodHandles.Lookup lookup = MethodHandles.lookup().in(clazz);
        try {
            this.instance = clazz.getConstructor().newInstance();
        } catch (Throwable t) {
            throw new MalformedResourceException("Unable to create instance of " + clazz.getName() + ". Rule set must be a pubic class with zero-argument constructor.", t);
        }
        this.runtime = runtime;
        this.ruleSetClass = clazz;
        // Scan and store methods via reflection
        this.ruleMethods = new ArrayList<>();

        Map<String, List<RuleSetMethod>> nonAnnotatedMethods = new HashMap<>();
        Collection<RuleSetMethod> ruleAnnotatedMethods = new ArrayList<>();
        Collection<RuleSetMethod> phaseAnnotatedMethods = new ArrayList<>();
        for (Method m : clazz.getMethods()) {
            RuleSetMethod methodWrapper = new RuleSetMethod(lookup, m);

            Rule ruleAnnotation = m.getAnnotation(Rule.class);
            PhaseListener listenerAnnotation = m.getAnnotation(PhaseListener.class);
            if (listenerAnnotation == null && ruleAnnotation == null) {
                nonAnnotatedMethods.computeIfAbsent(m.getName(), k -> new ArrayList<>()).add(methodWrapper);
                continue;
            }

            if (listenerAnnotation != null && ruleAnnotation != null) {
                throw new MalformedResourceException("Method '" + m.getName() + "' is annotated both as with a @Rule and as a @PhaseListener annotations.");
            }

            // When reached here, the method is either a rule method, or a listener method
            if (Modifier.isPublic(m.getModifiers())) {
                if (ruleAnnotation != null) {
                    ruleAnnotatedMethods.add(methodWrapper);
                } else {
                    phaseAnnotatedMethods.add(methodWrapper);
                }
            } else {
                throw new MalformedResourceException("Non-public method annotated as @Rule: " + m.getName());
            }
        }

        for (RuleSetMethod m : ruleAnnotatedMethods) {
            RuleMethod rm = new RuleMethod(m, instance);
            this.ruleMethods.add(rm);
        }

        if (ruleMethods.isEmpty()) {
            LOGGER.warning("No rule methods found in the source, ruleset is empty");
        } else {
            // How to sort rules when no salience is specified
            RuleSet.Sort defaultSort = Utils.deriveSort(clazz);
            ruleMethods.sort(new RuleComparator(defaultSort));
        }
    }



    List<RuleMethod> getRuleMethods() {
        return ruleMethods;
    }

    ValuesPredicate resolve(LhsBuilder<?> lhs, RuleMethod rm, MethodPredicate predicate) {
        String[] descriptor = predicate.descriptor();
        if (descriptor.length == 0) {
            throw new MalformedResourceException("Condition method must have arguments (non-empty descriptor() annotation value)");
        }
        Class<?>[] signature = new Class<?>[descriptor.length];

        ExpressionResolver expressionResolver = runtime.getExpressionResolver();
        for (int i = 0; i < descriptor.length; i++) {
            FieldReference ref = expressionResolver.resolve(descriptor[i], lhs.getFactTypeMapper());
            signature[i] = ref.field().getValueType();
        }
        MethodType methodType = MethodType.methodType(boolean.class, signature);


        String methodName = predicate.method();

        boolean staticHandle = true;
        MethodHandle handle = resolveMethod(methodName, true, methodType);

        if (!rm.staticMethod && handle == null) {
            // The condition can be either a static or an instance method.
            staticHandle = false;
            handle = resolveMethod(methodName, false, methodType);
        }

        if (handle == null) {
            throw new MalformedResourceException("Unable to find/access condition method '" + methodName + "' with signature " + methodType);
        } else {
            return staticHandle ?
                    new MethodPredicateStatic(handle, signature.length)
                    :
                    new MethodPredicateNonStatic(handle, signature.length + 1, instance)
                    ;
        }
    }

    private MethodHandle resolveMethod(String method, boolean staticMethod, MethodType type) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup().in(ruleSetClass);
            return staticMethod ?
                    lookup.findStatic(ruleSetClass, method, type)
                    :
                    lookup.findVirtual(ruleSetClass, method, type);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            return null;
        }
    }

    private static class MethodPredicateStatic extends AbstractMethodPredicate {

        MethodPredicateStatic(MethodHandle handle, int size) {
            super(handle, size);
        }

        @Override
        void init(IntToValue values) {
            for (int i = 0; i < currentValues.length; i++) {
                this.currentValues[i] = values.apply(i);
            }
        }
    }

    private static class MethodPredicateNonStatic extends AbstractMethodPredicate {

        MethodPredicateNonStatic(MethodHandle handle, int size, Object instance) {
            super(handle, size);
            this.currentValues[0] = instance;
        }

        @Override
        void init(IntToValue values) {
            for (int i = 1; i < currentValues.length; i++) {
                this.currentValues[i] = values.apply(i - 1);
            }
        }
    }

    abstract static class AbstractMethodPredicate implements ValuesPredicate {
        final Object[] currentValues;
        final MethodHandle handle;

        AbstractMethodPredicate(MethodHandle handle, int size) {
            this.handle = handle;
            this.currentValues = new Object[size];
        }

        abstract void init(IntToValue values);

        @Override
        public final boolean test(IntToValue values) {
            try {
                init(values);
                return (boolean) handle.invokeWithArguments(currentValues);
            } catch (SecurityException e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }
}
