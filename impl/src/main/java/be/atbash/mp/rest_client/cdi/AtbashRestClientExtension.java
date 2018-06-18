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

import org.apache.deltaspike.core.util.bean.BeanBuilder;
import org.apache.deltaspike.partialbean.impl.PartialBeanProxyFactory;
import org.apache.deltaspike.proxy.api.DeltaSpikeProxyContextualLifecycle;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CDI extension to create instances (rest client instances) for each interface which has the {@code RegisterRestClient} annotation.
 */
public class AtbashRestClientExtension implements Extension {

    private List<Bean<?>> restClientBeans = new ArrayList<>();

    /**
     * Search for all
     * @param pat
     * @param beanManager
     * @param <T>
     */
    public <T> void processAnnotatedType(@Observes @WithAnnotations({RegisterRestClient.class}) ProcessAnnotatedType<T> pat, BeanManager beanManager) {

        Class<T> javaClass = pat.getAnnotatedType().getJavaClass();

        // Create Lifecycle for CDI bean, based
        // javaClass -> instance will be assignable to this (interface)
        // InjectableRestClient -> proxy which will execute the methods.
        DeltaSpikeProxyContextualLifecycle lifecycle = new DeltaSpikeProxyContextualLifecycle(javaClass,
                InjectableRestClient.class,
                PartialBeanProxyFactory.getInstance(),
                beanManager);

        BeanBuilder<T> beanBuilder = new BeanBuilder<T>(beanManager)
                .readFromType(pat.getAnnotatedType())
                .qualifiers(new RestClientLiteral())
                .passivationCapable(true)
                .beanLifecycle(lifecycle);

        // Keep bean definition in a list for the moment
        restClientBeans.add(beanBuilder.create());


    }

    public void afterBean(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {

        for (Bean<?> restClientBean : restClientBeans) {
            afterBeanDiscovery.addBean(restClientBean);
        }

    }

}
