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
package be.atbash.mp.rest_client;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Invokes the endpoints using JAX-RS rest client. Instances are used as proxy invocation handler created by the RestClientBuilderResolver implementation.
 */
public class RestClientInvoker implements InvocationHandler {
    private Client client;
    private String baseURI;
    private List<LocalProviderInfo> localProviderInstances;

    public RestClientInvoker(Client client, String baseURI, List<LocalProviderInfo> localProviderInstances) {
        this.client = client;
        this.baseURI = baseURI;
        this.localProviderInstances = localProviderInstances;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        StringBuilder serverURL = determineEndpointURL(method);

        String httpMethod = determineMethod(method);
        if (httpMethod == null) {
            throw new RuntimeException(String.format("Unknown http method at %s", method));
        }

        ParameterInfo parameterInfo = determineParameterInfo(method, args);

        UriBuilder uriBuilder = UriBuilder.fromUri(serverURL.toString());
        for (Map.Entry<String, Object> entry : parameterInfo.getQueryParameterValues().entrySet()) {
            uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        String url = uriBuilder
                .buildFromMap(parameterInfo.getPathParameterValues())
                .toString();

        Invocation.Builder request = client.target(url).request().headers(parameterInfo.getHeaderValues());

        Invocation invocation;
        if (parameterInfo.getPayload() != null) {
            invocation = request.build(httpMethod, Entity.entity(parameterInfo.getPayload(), MediaType.APPLICATION_JSON));
        } else {
            invocation = request.build(httpMethod);
        }

        Object result = null;
        Response response = invocation.invoke();
        try {

            handleExceptionMapping(response, Arrays.asList(method.getExceptionTypes()));

            if (!void.class.equals(method.getReturnType())) {
                result = response.readEntity(method.getReturnType());
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return result;
    }

    private void handleExceptionMapping(Response response, List<Class<?>> exceptionTypes) throws Throwable {
        int status = response.getStatus();
        MultivaluedMap<String, Object> headers = response.getHeaders();

        for (LocalProviderInfo localProviderInfo : localProviderInstances) {
            if (localProviderInfo.getLocalProvider() instanceof ResponseExceptionMapper) {
                ResponseExceptionMapper mapper = (ResponseExceptionMapper) localProviderInfo.getLocalProvider();
                if (mapper.handles(status, headers)) {
                    Throwable throwable = mapper.toThrowable(response);
                    if (throwable != null) {
                        throwExceptionIfAppropriate(throwable, exceptionTypes);
                    }
                }
            }
        }
    }

    private void throwExceptionIfAppropriate(Throwable throwable, List<Class<?>> exceptionTypes) throws Throwable {
        if (throwable instanceof RuntimeException) {
            throw throwable;
        }
        if (throwable instanceof Error) {
            throw throwable;
        }
        for (Class<?> exceptionType : exceptionTypes) {
            if (exceptionType.isAssignableFrom(throwable.getClass())) {
                throw throwable;
            }
        }
    }

    private String determineMethod(Method method) {
        if (method.getAnnotation(GET.class) != null) {
            return HttpMethod.GET;
        }
        if (method.getAnnotation(PUT.class) != null) {
            return HttpMethod.PUT;
        }
        if (method.getAnnotation(POST.class) != null) {
            return HttpMethod.POST;
        }
        if (method.getAnnotation(DELETE.class) != null) {
            return HttpMethod.DELETE;
        }
        return null;
    }

    private ParameterInfo determineParameterInfo(Method method, Object[] args) {
        ParameterInfo result = new ParameterInfo();
        int paramIndex = 0;
        for (Annotation[] annotations : method.getParameterAnnotations()) {

            boolean jaxrsAnnotationFound = false;
            for (Annotation annotation : annotations) {
                if (PathParam.class.equals(annotation.annotationType())) {
                    result.addPathParameterValue(((PathParam) annotation).value(), args[paramIndex]);
                    jaxrsAnnotationFound = true;
                }
                if (QueryParam.class.equals(annotation.annotationType())) {
                    result.addQueryParameterValue(((QueryParam) annotation).value(), args[paramIndex]);
                    jaxrsAnnotationFound = true;
                }
                if (HeaderParam.class.equals(annotation.annotationType())) {
                    result.addHeaderValue(((HeaderParam) annotation).value(), args[paramIndex]);
                    jaxrsAnnotationFound = true;
                }
            }
            if (!jaxrsAnnotationFound) {
                result.setPayload(args[paramIndex]);
            }

            paramIndex++;
        }
        return result;
    }

    private StringBuilder determineEndpointURL(Method method) {
        StringBuilder serverURL = new StringBuilder();
        serverURL.append(baseURI);

        Path classPathAnnotation = method.getDeclaringClass().getAnnotation(Path.class);
        if (classPathAnnotation != null) {
            String value = classPathAnnotation.value();
            if (!value.startsWith("/")) {
                serverURL.append('/');
            }
            serverURL.append(value);
        }

        Path methodPathAnnotation = method.getAnnotation(Path.class);
        if (methodPathAnnotation != null) {
            String value = methodPathAnnotation.value();
            if (!value.startsWith("/")) {
                serverURL.append('/');
            }
            serverURL.append(value);
        }
        return serverURL;
    }

}
