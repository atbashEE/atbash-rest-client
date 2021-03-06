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
package be.atbash.mp.rest_client.exception;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * The default {@code ResponseExceptionMapper} required for the specification.
 */
public class DefaultResponseExceptionMapper implements ResponseExceptionMapper {

    @Override
    public Throwable toThrowable(Response response) {
        return new WebApplicationException("Unknown error, status code " + response.getStatus(), response);
    }

    @Override
    public boolean handles(int status, MultivaluedMap headers) {
        return status >= 400;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
