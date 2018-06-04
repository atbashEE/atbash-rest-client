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
package be.atbash.mp.rest_client.cdi;

import org.eclipse.microprofile.rest.client.AbstractRestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.annotation.RegisterProviders;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Proxy behind the interface which delegates execution to microprofile {@code RestClientBuilder} created instances.
 * It is also responsible for adding the Provider classes defined with the {@code RegisterProvider} annotations.
 */
@Dependent
public class InjectableRestClient implements InvocationHandler {
    private Map<Class, Object> restClientInvokerCache = new HashMap<>();

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Object restClientInvoker = restClientInvokerCache.get(method.getDeclaringClass());
        if (restClientInvoker == null) {
            RestClientBuilder restClientBuilder = AbstractRestClientBuilder.newBuilder();

            registerProviders(restClientBuilder, method);
            restClientInvoker = restClientBuilder.build(method.getDeclaringClass());

            restClientInvokerCache.put(method.getDeclaringClass(), restClientInvoker);
        }

        return method.invoke(restClientInvoker, args);
    }

    private void registerProviders(RestClientBuilder restClientBuilder, Method method) {
        RegisterProvider provider = method.getDeclaringClass().getAnnotation(RegisterProvider.class);
        if (provider != null) {
            registerSingleProvider(restClientBuilder, provider);
        }

        RegisterProviders registerProviders = method.getDeclaringClass().getAnnotation(RegisterProviders.class);
        if (registerProviders != null) {
            for (RegisterProvider registerProvider : registerProviders.value()) {
                registerSingleProvider(restClientBuilder, registerProvider);
            }
        }

        // TODO Providers from config com.mycompany.remoteServices.MyServiceClient/mp-rest/providers
    }

    private void registerSingleProvider(RestClientBuilder restClientBuilder, RegisterProvider provider) {
        Class<?> providerClass = provider.value();
        int priority = provider.priority();
        if (priority == -1) {
            Priority priorityAnnotation = providerClass.getAnnotation(Priority.class);
            if (priorityAnnotation != null) {
                priority = priorityAnnotation.value();
            }
        }

        // TODO Priority from config com.mycompany.remoteServices.MyServiceClient/mp-rest/providers/com.mycompany.MyProvider/priority
        if (priority == -1) {
            restClientBuilder.register(providerClass);
        } else {
            restClientBuilder.register(providerClass, priority);
        }

    }
}
