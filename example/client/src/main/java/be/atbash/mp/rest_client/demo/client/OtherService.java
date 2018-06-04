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

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;

@Path("/other")
@RegisterRestClient
@ApplicationScoped
public interface OtherService {

    @Path("{parameter}")
    @GET
    String doSomething(@PathParam("parameter") String parameter);

    @Path("json/{parameter}")
    @GET
    Data sayHello(@PathParam("parameter") String parameter);

    @Path("data")
    @POST
    String sendData(Data data);

    @Path("others")
    @DELETE
    String sendOtherValues(@QueryParam("param") String param, @HeaderParam("Authorization") String authorizationHeader);

    class Data {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
