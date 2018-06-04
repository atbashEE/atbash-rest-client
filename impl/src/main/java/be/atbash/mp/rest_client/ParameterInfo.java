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

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps parameter info of a method which will be delegated to the MP Rest client and remote endpoint.
 */
class ParameterInfo {

    private Map<String, Object> pathParameterValues = new HashMap<>();
    private Map<String, Object> queryParameterValues = new HashMap<>();
    private MultivaluedMap<String, Object> headerValues = new MultivaluedHashMap<>();
    private Object payload = null;


    void addPathParameterValue(String name, Object value) {
        pathParameterValues.put(name, value);
    }

    void addQueryParameterValue(String name, Object value) {
        queryParameterValues.put(name, value);
    }

    void addHeaderValue(String name, Object value) {
        headerValues.add(name, value);
    }

    void setPayload(Object payload) {
        this.payload = payload;
    }

    Map<String, Object> getPathParameterValues() {
        return pathParameterValues;
    }

    Map<String, Object> getQueryParameterValues() {
        return queryParameterValues;
    }

    MultivaluedMap<String, Object> getHeaderValues() {
        return headerValues;
    }

    Object getPayload() {
        return payload;
    }
}
