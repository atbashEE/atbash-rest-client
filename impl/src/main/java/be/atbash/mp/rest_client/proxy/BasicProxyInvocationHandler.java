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

import org.apache.deltaspike.core.util.ReflectionUtils;
import org.apache.deltaspike.proxy.api.DeltaSpikeProxyFactory;
import org.apache.deltaspike.proxy.spi.DeltaSpikeProxy;
import org.apache.deltaspike.proxy.spi.invocation.DeltaSpikeProxyInvocationHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The {@link InvocationHandler} which will be called directly by the proxy methods.
 * For both <code>delegateMethods</code> and <code>interceptMethods</code>
 * (See: {@link org.apache.deltaspike.proxy.spi.DeltaSpikeProxyClassGenerator}).
 * <p>
 * This version overrides {@code DeltaSpikeProxyInvocationHandler} version to not use any CDI related functionality.
 * It doesn't support any interceptors though.
 */

public class BasicProxyInvocationHandler extends DeltaSpikeProxyInvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {

        DeltaSpikeProxy deltaSpikeProxy = (DeltaSpikeProxy) proxy;

        if (contains(deltaSpikeProxy.getDelegateMethods(), method)) {
            return deltaSpikeProxy.getDelegateInvocationHandler().invoke(proxy, method, parameters);
        } else {
            try {
                Method superAccessorMethod = DeltaSpikeProxyFactory.getSuperAccessorMethod(proxy, method);
                return superAccessorMethod.invoke(proxy, parameters);
            } catch (InvocationTargetException e) {
                // rethrow original exception
                throw e.getCause();
            }
        }
    }

    protected boolean contains(Method[] methods, Method method) {
        if (methods == null || methods.length == 0) {
            return false;
        }

        for (Method current : methods) {
            if (ReflectionUtils.hasSameSignature(method, current)) {
                return true;
            }
        }

        return false;
    }
}
