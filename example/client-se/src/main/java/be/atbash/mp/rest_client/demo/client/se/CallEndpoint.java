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
package be.atbash.mp.rest_client.demo.client.se;

import org.eclipse.microprofile.rest.client.AbstractRestClientBuilder;

import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 */

public class CallEndpoint {

    public static void main(String[] args) throws MalformedURLException {
        HelloService helloService = AbstractRestClientBuilder.newBuilder()
                .baseUrl(new URL("http://localhost:8080/server/data"))
                .build(HelloService.class);

        System.out.println(helloService.sayHello());
        System.out.println(helloService.sayHello());  // Test connection closing

        OtherService otherService = AbstractRestClientBuilder.newBuilder()
                .baseUrl(new URL("http://localhost:8080/server/data"))
                .build(OtherService.class);

        System.out.println("otherService " + otherService.doSomething("param"));

        OtherService.Data data = otherService.sayHello("Rudy");
        System.out.println("otherService JSON " + data.getValue());

        data = new OtherService.Data();
        data.setValue("payload");
        System.out.println("otherService Send Payload " + otherService.sendData(data));

        System.out.println("otherService delete with param and header " + otherService.sendOtherValues("queryParam", "headerValue"));


    }
}
