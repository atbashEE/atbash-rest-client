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
package org.eclipse.microprofile.rest.client.annotation;

import java.lang.annotation.*;

/**
 * When annotation is placed at the interface level of a REST API definition, the providers listed will be registered upon proxying.
 * <p>
 * If a provider listed is not found on the classpath, it is ignored.  If a provider is listed, but is not a valid provider, then an
 * {@link IllegalArgumentException} is thrown indicating that the provider is invalid.
 * <p>
 * This class serves to act as the {@code Repeatable} implementation for {@link RegisterProvider}
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RegisterProviders {
    RegisterProvider[] value();
}
