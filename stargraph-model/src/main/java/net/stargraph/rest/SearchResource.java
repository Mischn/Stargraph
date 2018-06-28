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

/**
 * Entry point to search with the Database.
 */
@Path("_kb")
@Produces(MediaType.APPLICATION_JSON)
public interface SearchResource {

    @GET
    @Path("{kbId}/search/instance")
    Response instanceSearch(@PathParam("kbId") String dbId, @QueryParam("term") String instanceTerm,
                            @DefaultValue("LEVENSHTEIN") @QueryParam("model") String model,
                            @DefaultValue("10") @QueryParam("topk") int topk);

    @GET
    @Path("{kbId}/search/class")
    Response classSearch(@PathParam("kbId") String dbId, @QueryParam("term") String classTerm,
                            @DefaultValue("LEVENSHTEIN") @QueryParam("model") String model,
                            @DefaultValue("10") @QueryParam("topk") int topk);

    @GET
    @Path("{kbId}/search/property")
    Response propertySearch(@PathParam("kbId") String dbId, @QueryParam("term") String propertyTerm,
                         @DefaultValue("LEVENSHTEIN") @QueryParam("model") String model,
                         @DefaultValue("10") @QueryParam("topk") int topk);

    @GET
    @Path("{kbId}/search/pivoted")
    Response pivotedSearch(@PathParam("kbId") String dbId, @QueryParam("id") String id,
                                    @QueryParam("term") String relationTerm,
                                    @DefaultValue("false") @QueryParam("incoming") boolean incomingEdges,
                                    @DefaultValue("true") @QueryParam("outgoing") boolean outgoingEdges,
                                    @DefaultValue("1") @QueryParam("range") int range,
                                    @DefaultValue("W2V") @QueryParam("model") String model,
                                    @DefaultValue("10") @QueryParam("topk") int topk);

}
