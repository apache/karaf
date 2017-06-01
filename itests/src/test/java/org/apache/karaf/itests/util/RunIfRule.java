/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.itests.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assume;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

@SuppressWarnings("rawtypes")
public class RunIfRule implements MethodRule {

    public interface RunIfCondition {
        boolean isSatisfied();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Inherited
    public @interface RunIf {
        Class<? extends RunIfCondition> condition();
    }

    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        List<RunIf> ignores = findRunIfs(method.getAnnotations(), new ArrayList<>(), new HashSet<>());
        if (ignores.isEmpty()) {
            return base;
        }
        for (RunIf ignore : ignores) {
            RunIfCondition condition = newCondition(ignore, target);
            if (!condition.isSatisfied()) {
                return new IgnoreStatement(condition);
            }
        }
        return base;
    }

    private List<RunIf> findRunIfs(Annotation[] annotations, List<RunIf> ignores, Set<Class> tested) {
        for (Annotation annotation : annotations) {
            if (tested.add(annotation.getClass())) {
                if (annotation instanceof RunIf) {
                    ignores.add((RunIf) annotation);
                } else {
                    findRunIfs(annotation.getClass().getAnnotations(), ignores, tested);
                    for (Class cl : annotation.getClass().getInterfaces()) {
                        findRunIfs(cl.getAnnotations(), ignores, tested);
                    }
                }
            }
        }
        return ignores;
    }

    private RunIfCondition newCondition(RunIf annotation, Object instance) {
        final Class<? extends RunIfCondition> cond = annotation.condition();
        try {
            if (cond.isMemberClass()) {
                if (Modifier.isStatic(cond.getModifiers())) {
                    return cond.getDeclaredConstructor(new Class<?>[]{}).newInstance();
                } else if (instance != null && instance.getClass().isAssignableFrom(cond.getDeclaringClass())) {
                    return cond.getDeclaredConstructor(new Class<?>[]{instance.getClass()}).newInstance(instance);
                }
                throw new IllegalArgumentException("Unable to instanciate " + cond.getClass().getName());
            } else {
                return cond.newInstance();
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class IgnoreStatement extends Statement {
        private RunIfCondition condition;

        IgnoreStatement(RunIfCondition condition) {
            this.condition = condition;
        }

        @Override
        public void evaluate() {
            Assume.assumeTrue("Ignored by " + condition.getClass().getSimpleName(), false);
        }
    }

}
