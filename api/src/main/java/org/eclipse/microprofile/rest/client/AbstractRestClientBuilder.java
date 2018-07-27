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
package org.eclipse.microprofile.rest.client;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderListener;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;

public abstract class AbstractRestClientBuilder implements RestClientBuilder {
    public static RestClientBuilder newBuilder() {
        final RestClientBuilder builder = RestClientBuilderResolver.instance().newBuilder();
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                                          @Override
                                          public Void run() {
                                              for (RestClientBuilderListener listener : ServiceLoader.load(RestClientBuilderListener.class)) {
                                                  listener.onNewBuilder(builder);
                                              }
                                              return null;
                                          }
                                      }
        );
        return builder;
    }
}
