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
package be.atbash.mp.rest_client.spec;

import be.atbash.config.ConfigOptionalValue;
import be.atbash.mp.rest_client.LocalProviderInfo;
import be.atbash.mp.rest_client.RestClientInvoker;
import be.atbash.mp.rest_client.exception.DefaultResponseExceptionMapper;
import be.atbash.mp.rest_client.proxy.BasicProxyInvocationHandler;
import be.atbash.mp.rest_client.proxy.BasicRestClientProxyFactory;
import be.atbash.mp.rest_client.proxy.RestClientProxyFactory;
import be.atbash.util.CDIUtils;
import be.atbash.util.reflection.CDICheck;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.apache.deltaspike.core.util.ExceptionUtils;
import org.apache.deltaspike.proxy.spi.DeltaSpikeProxy;
import org.apache.deltaspike.proxy.spi.invocation.DeltaSpikeProxyInvocationHandler;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.AbstractRestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.BeanManager;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.ParamConverterProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the MicroProfile {@code RestClientBuilder} API class.
 */
// Based on the WildFly swarm version
class BuilderImpl extends AbstractRestClientBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuilderImpl.class);

    private static final String DEFAULT_MAPPER_PROP = "microprofile.rest.client.disable.default.mapper";

    private static final String URI_PARAM_NAME_REGEX = "\\w[\\w\\.-]*";
    private static final String URI_PARAM_REGEX_REGEX = "[^{}][^{}]*";
    private static final String URI_PARAM_REGEX = "\\{\\s*(" + URI_PARAM_NAME_REGEX + ")\\s*(:\\s*(" + URI_PARAM_REGEX_REGEX + "))?\\}";
    private static final Pattern URI_PARAM_PATTERN = Pattern.compile(URI_PARAM_REGEX);

    private static final char openCurlyReplacement = 6;
    private static final char closeCurlyReplacement = 7;

    private ClientBuilder clientBuilder;
    private DeltaSpikeProxyInvocationHandler deltaSpikeProxyInvocationHandler;

    private BeanManager beanManager;

    private String baseURI;

    private Set<LocalProviderInfo> localProviderInstances = new HashSet<>();

    BuilderImpl() {
        clientBuilder = ClientBuilder.newBuilder();
    }

    @Override
    public RestClientBuilder baseUrl(URL url) {
        try {
            this.baseURI = url.toURI().toString().replaceFirst("/*$", "");
            return this;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public <T> T build(Class<T> targetClass) throws IllegalStateException, RestClientDefinitionException {

        verifyInterface(targetClass);

        Class<T> proxyClass;
        Method[] delegateMethods;
        if (CDICheck.withinContainer()) {
            // CDI version of the proxy. It also honours the interceptors
            RestClientProxyFactory proxyFactory = RestClientProxyFactory.getInstance();
            beanManager = CDIUtils.getBeanManager();
            proxyClass = proxyFactory.getProxyClass(beanManager, targetClass);
            delegateMethods = proxyFactory.getDelegateMethods(targetClass);

        } else {
            // Java SE version, basic version.
            BasicRestClientProxyFactory proxyFactory = BasicRestClientProxyFactory.getInstance();
            proxyClass = proxyFactory.getProxyClass(targetClass);
            delegateMethods = proxyFactory.getDelegateMethods(targetClass);
        }
        // Create proxy for methods of target.
        return create(targetClass, proxyClass, delegateMethods);
    }

    private <T> T create(Class<T> targetClass, Class<T> proxyClass, Method[] delegateMethods) {
        try {
            lazyInit();

            T instance = proxyClass.newInstance();

            DeltaSpikeProxy deltaSpikeProxy = ((DeltaSpikeProxy) instance);
            deltaSpikeProxy.setInvocationHandler(deltaSpikeProxyInvocationHandler);

            deltaSpikeProxy.setDelegateMethods(delegateMethods);

            if (baseURI == null) {
                baseUrl(ConfigProvider.getConfig().getValue(targetClass.getName() + "/mp-rest/url", URL.class));
            }

            // A RestClientInvoker uses the Rest client to invoke the endpoints
            RestClientInvoker restClientInvoker = new RestClientInvoker(clientBuilder.build(), baseURI, defineLocalProviderInstances());
            deltaSpikeProxy.setDelegateInvocationHandler(restClientInvoker);

            return instance;
        } catch (Exception e) {
            ExceptionUtils.throwAsRuntimeException(e);
        }

        // can't happen
        return null;
    }

    private List<LocalProviderInfo> defineLocalProviderInstances() {
        // Default exception mapper
        if (!isMapperDisabled()) {
            register(DefaultResponseExceptionMapper.class);
        }

        List<LocalProviderInfo> result = new ArrayList<>(localProviderInstances);
        Collections.sort(result, new Comparator<LocalProviderInfo>() {
            @Override
            public int compare(LocalProviderInfo lpi1, LocalProviderInfo lpi2) {
                Integer i1 = lpi1.getPriority();
                Integer i2 = lpi2.getPriority();
                return i1.compareTo(i2);
            }
        });
        return result;
    }

    private boolean isMapperDisabled() {
        boolean disabled = false;
        Boolean defaultMapperProp = ConfigOptionalValue.getValue(DEFAULT_MAPPER_PROP, Boolean.class);

        // disabled through config api
        if (defaultMapperProp != null && defaultMapperProp) {
            disabled = true;
        } else if (defaultMapperProp == null) {

            // disabled through jaxrs property
            // FIXME
            /*
            try {
                Object property = this.builderDelegate.getConfiguration().getProperty(DEFAULT_MAPPER_PROP);
                if (property != null) {
                    disabled = (Boolean)property;
                }
            } catch (Throwable e) {
                // ignore cast exception
            }*/
        }
        return disabled;
    }

    private void lazyInit() {
        if (deltaSpikeProxyInvocationHandler == null) {
            init();
        }
    }

    private synchronized void init() {
        if (deltaSpikeProxyInvocationHandler == null) {
            if (CDICheck.withinContainer()) {
                // CDI version
                deltaSpikeProxyInvocationHandler = BeanProvider.getContextualReference(
                        beanManager, DeltaSpikeProxyInvocationHandler.class, false);
            } else {
                // Java SE Version
                deltaSpikeProxyInvocationHandler = new BasicProxyInvocationHandler();
            }

        }
    }

    private <T> void verifyInterface(Class<T> typeDef) {

        Method[] methods = typeDef.getMethods();

        // multiple verbs
        for (Method method : methods) {
            boolean hasHttpMethod = false;
            for (Annotation annotation : method.getAnnotations()) {
                boolean isHttpMethod = (annotation.annotationType().getAnnotation(HttpMethod.class) != null);
                if (!hasHttpMethod && isHttpMethod) {
                    hasHttpMethod = true;
                } else if (hasHttpMethod && isHttpMethod) {
                    throw new RestClientDefinitionException("Ambiguous @Httpmethod defintion on type " + typeDef);
                }
            }
        }

        // invalid parameter
        Path classPathAnno = typeDef.getAnnotation(Path.class);

        Set<String> classLevelVariables = new HashSet<>();
        if (classPathAnno != null) {
            classLevelVariables.addAll(getPathParamList(classPathAnno.value()));
        }

        for (Method method : methods) {

            Path methodPathAnno = method.getAnnotation(Path.class);
            if (methodPathAnno == null) {
                continue;
            }

            Set<String> allVariables = new HashSet<>(classLevelVariables);
            allVariables.addAll(getPathParamList(methodPathAnno.value()));

            Set<String> parameterNames = new HashSet<>();
            for (Annotation[] annotations : method.getParameterAnnotations()) {
                for (Annotation annotation : annotations) {
                    if (PathParam.class.equals(annotation.annotationType())) {
                        parameterNames.add(((PathParam) annotation).value());
                    }
                }
            }

            if (allVariables.size() != parameterNames.size()) {
                throw new RestClientDefinitionException(String.format("Parameters and variables don't match on %s::%s", typeDef, method.getName()));
            }

            parameterNames.removeAll(allVariables);
            if (!parameterNames.isEmpty()) {
                throw new RestClientDefinitionException(String.format("Parameter names don't match variable names on %s::%s", typeDef, method.getName()));
            }
        }

        // TODO More and better checks

    }

    private List<String> getPathParamList(String string) {
        List<String> params = new ArrayList<>();
        Matcher matcher = URI_PARAM_PATTERN.matcher(replaceEnclosedCurlyBraces(string));
        while (matcher.find()) {
            String param = matcher.group(1);
            params.add(param);
        }
        return params;
    }

    private CharSequence replaceEnclosedCurlyBraces(String str) {

        char[] chars = str.toCharArray();
        int open = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '{') {
                if (open != 0) {
                    chars[i] = openCurlyReplacement;
                }
                open++;
            } else if (chars[i] == '}') {
                open--;
                if (open != 0) {
                    chars[i] = closeCurlyReplacement;
                }
            }
        }
        return new String(chars);
    }

    @Override
    public Configuration getConfiguration() {
        return clientBuilder.getConfiguration();
    }

    @Override
    public RestClientBuilder property(String name, Object value) {
        clientBuilder.property(name, value);
        return this;
    }

    private static Object newInstanceOf(Class clazz) {
        try {
            return clazz.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to register " + clazz, t);
        }
    }

    @Override
    public RestClientBuilder register(Class<?> aClass) {
        register(newInstanceOf(aClass));
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> aClass, int i) {

        register(newInstanceOf(aClass), i);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> aClass, Class<?>[] classes) {
        register(newInstanceOf(aClass), classes);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> aClass, Map<Class<?>, Integer> map) {
        register(newInstanceOf(aClass), map);
        return this;
    }

    @Override
    public RestClientBuilder register(Object o) {

        if (o instanceof ResponseExceptionMapper) {
            ResponseExceptionMapper mapper = (ResponseExceptionMapper) o;
            register(mapper, mapper.getPriority());
        } else if (o instanceof ParamConverterProvider) {
            register(o, Priorities.USER);
        } else {
            clientBuilder.register(o);
        }

        return this;
    }

    @Override
    public RestClientBuilder register(Object o, int priority) {
        if (o instanceof ResponseExceptionMapper) {

            // local
            ResponseExceptionMapper mapper = (ResponseExceptionMapper) o;
            registerLocalProviderInstance(mapper, priority);

        } else if (o instanceof ParamConverterProvider) {

            // local
            ParamConverterProvider converter = (ParamConverterProvider) o;
            registerLocalProviderInstance(converter, priority);

        } else {
            clientBuilder.register(o, priority);
        }
        return this;
    }

    @Override
    public RestClientBuilder register(Object o, Class<?>[] classes) {

        // local
        for (Class<?> aClass : classes) {
            if (aClass.isAssignableFrom(ResponseExceptionMapper.class)) {
                register(o);
            }
        }

        // other
        clientBuilder.register(o, classes);
        return this;
    }

    @Override
    public RestClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {

        // FIXME
        return this;
    }

    private void registerLocalProviderInstance(Object provider, int priority) {
        for (LocalProviderInfo registered : localProviderInstances) {
            if (registered.getLocalProvider().equals(provider)) {
                LOGGER.warn(String.format("Provider already registered %s", provider.getClass().getName()));
                return;
            }
        }

        localProviderInstances.add(new LocalProviderInfo(provider, priority));
    }

}
