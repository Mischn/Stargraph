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

/**
 * Entry point to fetch objects from the Database.
 */
@Path("_kb")
@Produces(MediaType.APPLICATION_JSON)
public interface FetchResource {

    @GET
    @Path("{kbId}/fetch/instance")
    Response getInstance(@PathParam("kbId") String dbId, @QueryParam("id") String id);

    @GET
    @Path("{kbId}/fetch/property")
    Response getProperty(@PathParam("kbId") String dbId, @QueryParam("id") String id);

    @GET
    @Path("{kbId}/fetch/document")
    Response getDocument(@PathParam("kbId") String dbId, @QueryParam("id") String id);

    @GET
    @Path("{kbId}/fetch/documents")
    Response getDocuments(@PathParam("kbId") String dbId, @QueryParam("entityId") String entityId, @DefaultValue("null") @QueryParam("docTypes") List<String> docTypes, @DefaultValue("false") @QueryParam("inOrder") boolean inOrder);

    @GET
    @Path("{kbId}/fetch/linked")
    Response getLinkedNodes(@PathParam("kbId") String dbId, @QueryParam("entityId") String entityId, @DefaultValue("true") @QueryParam("incoming") boolean incoming, @DefaultValue("true") @QueryParam("outgoing") boolean outgoing);

}
