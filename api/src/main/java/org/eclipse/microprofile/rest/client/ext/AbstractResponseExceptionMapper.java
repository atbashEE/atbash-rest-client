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
package org.eclipse.microprofile.rest.client.ext;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.MultivaluedMap;

public abstract class AbstractResponseExceptionMapper<T extends Throwable> implements ResponseExceptionMapper<T> {

    /**
     * Whether or not this mapper will be used for the given response.  By default, any response code of 400 or higher will be handled.
     * Individual mappers may override this method if they want to more narrowly focus on certain response codes or headers.
     *
     * @param status  the response status code indicating the HTTP response
     * @param headers the headers from the HTTP response
     * @return whether or not this mapper can convert the Response to a Throwable
     */
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return status >= 400;
    }

    /**
     * The priority of this mapper.  By default, it will use the {@link Priority} annotation's value as the priority.
     * If no annotation is present, it uses a default priority of {@link Priorities#USER}.
     *
     * @return the priority of this mapper
     */
    public int getPriority() {
        Priority priority = getClass().getAnnotation(Priority.class);
        if (priority == null) {
            return DEFAULT_PRIORITY;
        }
        return priority.value();
    }
}
