/*
 * Copyright 2018 Rudy De Busscher (https://www.atbash.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.atbash.mp.rest_client.proxy;

import org.apache.deltaspike.core.util.ClassUtils;
import org.apache.deltaspike.core.util.ReflectionUtils;
import org.apache.deltaspike.proxy.spi.DeltaSpikeProxyClassGenerator;
import org.apache.deltaspike.proxy.spi.DeltaSpikeProxyClassGeneratorHolder;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Factory creating proxies specifically for the RestClient in a Java SE environment. All methods abstract methods are proxied.
 * Code is copied from DeltaSpike, removing the CDI accessing parts (and thus also losing interceptor capabilities.  See also {@link RestClientProxyFactory}.
 */
// TODO Cleanup class.
public class BasicRestClientProxyFactory {

    private static final BasicRestClientProxyFactory INSTANCE = new BasicRestClientProxyFactory();
    private static final String SUPER_ACCESSOR_METHOD_SUFFIX = "$super";

    public static BasicRestClientProxyFactory getInstance() {
        return INSTANCE;
    }

    private String getProxyClassSuffix() {
        return "$$AtbashRestClientProxy";
    }

    private ArrayList<Method> getDelegateMethods(Class<?> targetClass, ArrayList<Method> allMethods) {
        ArrayList<Method> methods = new ArrayList<>();

        for (Method method : allMethods) {
            if (Modifier.isAbstract(method.getModifiers())) {
                methods.add(method);
            }
        }

        return methods;
    }

    private <T> Class<T> resolveAlreadyDefinedProxyClass(Class<T> targetClass) {
        return ClassUtils.tryToLoadClassForName(this.constructProxyClassName(targetClass), targetClass, targetClass.getClassLoader());
    }

    public <T> Class<T> getProxyClass(Class<T> targetClass) {
        Class<T> proxyClass = this.resolveAlreadyDefinedProxyClass(targetClass);
        if (proxyClass == null) {
            proxyClass = this.createProxyClass(targetClass.getClassLoader(), targetClass);
        }

        return proxyClass;
    }

    private synchronized <T> Class<T> createProxyClass(ClassLoader classLoader, Class<T> targetClass) {
        Class<T> proxyClass = this.resolveAlreadyDefinedProxyClass(targetClass);
        if (proxyClass == null) {
            ArrayList<Method> allMethods = this.collectAllMethods(targetClass);
            ArrayList<Method> interceptMethods = this.filterInterceptMethods(targetClass, allMethods);
            // TODO do we need to interceptMethods (as we don't intercept in the Java SE version.
            Method[] delegateMethods = this.getDelegateMethods(targetClass);
            if (interceptMethods != null && !interceptMethods.isEmpty()) {
                Iterator iterator = interceptMethods.iterator();

                while (iterator.hasNext()) {
                    Method method = (Method) iterator.next();
                    // TODO In original version interceptors are added. See that we can remove
                }
            }

            DeltaSpikeProxyClassGenerator proxyClassGenerator = DeltaSpikeProxyClassGeneratorHolder.lookup();
            Class<?>[] additionalInterfacesToImplement = this.getAdditionalInterfacesToImplement(targetClass);
            proxyClass = proxyClassGenerator.generateProxyClass(classLoader, targetClass, this.getProxyClassSuffix(), "$super", additionalInterfacesToImplement, delegateMethods, interceptMethods == null ? new Method[0] :  interceptMethods.toArray(new Method[interceptMethods.size()]));
        }

        return proxyClass;
    }

    private String constructProxyClassName(Class<?> clazz) {
        return clazz.getName() + this.getProxyClassSuffix();
    }

    private static String constructSuperAccessorMethodName(Method method) {
        return method.getName() + "$super";
    }

    public static Method getSuperAccessorMethod(Object proxy, Method method) throws NoSuchMethodException {
        return proxy.getClass().getMethod(constructSuperAccessorMethodName(method), method.getParameterTypes());
    }

    public boolean isProxyClass(Class<?> clazz) {
        return clazz.getName().endsWith(this.getProxyClassSuffix());
    }

    private boolean ignoreMethod(Method method, List<Method> methods) {
        // we have no interest in generics bridge methods
        if (method.isBridge()) {
            return true;
        }

        // we do not proxy finalize()
        if ("finalize".equals(method.getName())) {
            return true;
        }

        // same method...
        if (methods.contains(method)) {
            return true;
        }

        // check if a method with the same signature is already available
        for (Method currentMethod : methods) {
            if (ReflectionUtils.hasSameSignature(currentMethod, method)) {
                return true;
            }
        }

        return false;
    }

    private ArrayList<Method> collectAllMethods(Class<?> clazz) {
        ArrayList<Method> methods = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (!ignoreMethod(method, methods)) {
                methods.add(method);
            }
        }
        for (Method method : clazz.getMethods()) {
            if (!ignoreMethod(method, methods)) {
                methods.add(method);
            }
        }

        // collect methods from abstract super classes...
        Class currentSuperClass = clazz.getSuperclass();
        while (currentSuperClass != null) {
            if (Modifier.isAbstract(currentSuperClass.getModifiers())) {
                for (Method method : currentSuperClass.getDeclaredMethods()) {
                    if (!ignoreMethod(method, methods)) {
                        methods.add(method);
                    }
                }
                for (Method method : currentSuperClass.getMethods()) {
                    if (!ignoreMethod(method, methods)) {
                        methods.add(method);
                    }
                }
            }
            currentSuperClass = currentSuperClass.getSuperclass();
        }

        // sort out somewhere implemented abstract methods
        Class currentClass = clazz;
        while (currentClass != null) {
            Iterator<Method> methodIterator = methods.iterator();
            while (methodIterator.hasNext()) {
                Method method = methodIterator.next();
                if (Modifier.isAbstract(method.getModifiers())) {
                    try {
                        Method foundMethod = currentClass.getMethod(method.getName(), method.getParameterTypes());
                        // if method is implemented in the current class -> remove it
                        if (foundMethod != null && !Modifier.isAbstract(foundMethod.getModifiers())) {
                            methodIterator.remove();
                        }
                    } catch (Exception e) {
                        // ignore...
                    }
                }
            }

            currentClass = currentClass.getSuperclass();
        }

        return methods;
    }

    private ArrayList<Method> filterInterceptMethods(Class<?> targetClass, ArrayList<Method> allMethods) {
        ArrayList<Method> methods = new ArrayList<>();
        Iterator it = allMethods.iterator();

        while (it.hasNext()) {
            Method method = (Method) it.next();
            if (Modifier.isPublic(method.getModifiers()) && !Modifier.isFinal(method.getModifiers()) && !Modifier.isAbstract(method.getModifiers())) {
                methods.add(method);
            }
        }

        return methods;
    }

    private Class<?>[] getAdditionalInterfacesToImplement(Class<?> targetClass) {
        return null;
    }

    public Method[] getDelegateMethods(Class<?> targetClass) {
        ArrayList<Method> allMethods = this.collectAllMethods(targetClass);
        ArrayList<Method> delegateMethods = this.getDelegateMethods(targetClass, allMethods);
        return delegateMethods == null ? new Method[0] : delegateMethods.toArray(new Method[delegateMethods.size()]);
    }

}
