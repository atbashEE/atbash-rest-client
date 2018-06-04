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
package be.atbash.mp.rest_client.demo.server;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/other")
public class OtherResource {

    @Produces(MediaType.TEXT_PLAIN)
    @GET
    @Path("{param}")
    public String sayHello(@PathParam("param") String name) {
        return "Data From Other end point with parameter " + name;
    }

    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Path("json/{param}")
    public Data getData(@PathParam("param") String name) {
        Data result = new Data();
        result.setValue("Hello " + name);
        return result;
    }

    @Path("data")
    @POST
    public String sendData(Data data) {
        return "Received data " + data.getValue();
    }

    @Path("others")
    @DELETE
    public String sendOtherValues(@QueryParam("param") String param, @HeaderParam("Authorization") String authorizationHeader) {
        return String.format("Received values param : %s, header : %s", param, authorizationHeader);
    }


    public static class Data {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
