package net.stargraph.rest;

/*-
 * ==========================License-Start=============================
 * stargraph-model
 * --------------------------------------------------------------------
 * Copyright (C) 2017 Lambda^3
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ==========================License-End===============================
 */

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * Entry point to talk with the Database.
 */
@Path("_kb")
@Produces(MediaType.APPLICATION_JSON)
public interface QueryResource {

    @GET
    @Path("{kbId}/query")
    Response query(@PathParam("kbId") String dbId, @QueryParam("q") String q);

    @POST
    @Path("{kbId}/query")
    @Consumes(MediaType.APPLICATION_JSON)
    Response query(@PathParam("kbId") String dbId, @QueryParam("q") String q, MappingsBean betterMappings);


    class MappingsBean {
        private Map<String, Map<String, List<String>>> mappings;

        public MappingsBean() {}

        @SuppressWarnings("unused") // used by jackson serializer
        public Map<String, Map<String, List<String>>> getMappings() {
            return mappings;
        }

        @SuppressWarnings("unused") // used by jackson serializer
        public void setMappings(Map<String, Map<String, List<String>>> mappings) {
            this.mappings = mappings;
        }

        @Override
        public String toString() {
            return "MappingsBean{" +
                    "mappings=" + mappings +
                    '}';
        }
    }
}
