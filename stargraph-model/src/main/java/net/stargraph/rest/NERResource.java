package net.stargraph.rest;


import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("ner")
@Produces(MediaType.APPLICATION_JSON)
public interface NERResource {

    @POST
    @Path("link")
    @Consumes(MediaType.APPLICATION_JSON)
    Response searchAndLink(@QueryParam("kbId") String dbId, @DefaultValue("en") @QueryParam("language") String language, UserText userText);

    class UserText {
        public String text;
    }
}
