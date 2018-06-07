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
package be.atbash.mp.rest_client.demo.client;

import org.eclipse.microprofile.rest.client.AbstractRestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/test")
public class RestServlet extends HttpServlet {

    @Inject
    @RestClient
    private HelloService helloService;

    @Inject
    @RestClient
    private OtherService otherService;

    @Inject
    @RestClient
    private ProviderService providerService;

    @Inject
    @RestClient
    private ErrorService errorService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder result = new StringBuilder();

        result.append("helloService ").append(helloService.sayHello()).append("\n");
        result.append("helloService ").append(helloService.sayHello()).append("\n");  // test connection closing
        result.append("otherService ").append(otherService.doSomething("param")).append("\n");

        OtherService.Data data = otherService.sayHello("Rudy");
        result.append("otherService JSON ").append(data.getValue()).append("\n");

        data = new OtherService.Data();
        data.setValue("payload");
        result.append("otherService Send Payload ").append(otherService.sendData(data)).append("\n");

        result.append("otherService delete with param and header ").append(otherService.sendOtherValues("queryParam", "headerValue")).append("\n");

        result.append("provider added ").append(providerService.sayHello()).append("\n");

        HelloService helloServiceProg = AbstractRestClientBuilder.newBuilder().build(HelloService.class);
        result.append("helloService (programmatic) ").append(helloServiceProg.sayHello()).append("\n");

        /*
        try {
            errorService.sayHello();
        } catch (CustomException e) {
            e.printStackTrace();
        }
        */

        resp.getWriter().append(result.toString());
    }
}
