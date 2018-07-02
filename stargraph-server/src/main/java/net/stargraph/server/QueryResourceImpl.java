package net.stargraph.server;

/*-
 * ==========================License-Start=============================
 * stargraph-server
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

import net.stargraph.core.Namespace;
import net.stargraph.core.Stargraph;
import net.stargraph.core.query.QueryResponse;
import net.stargraph.core.query.response.AnswerSetResponse;
import net.stargraph.core.query.response.NoResponse;
import net.stargraph.core.query.response.SPARQLSelectResponse;
import net.stargraph.query.ExtQueryEngine;
import net.stargraph.rest.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class QueryResourceImpl implements QueryResource {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("server");
    private Stargraph core;
    private Map<String, ExtQueryEngine> engines;

    public QueryResourceImpl(Stargraph core) {
        this.core = Objects.requireNonNull(core);
        this.engines = new ConcurrentHashMap<>();
    }

    @Override
    public Response query(String dbId, String q) {
        return query(dbId, q, null);
    }

    @Override
    public Response query(String dbId, String q, MappingsBean betterMappings) {
        try {
            if (core.hasKB(dbId)) {
                Namespace namespace = core.getKBCore(dbId).getNamespace();
                ExtQueryEngine engine = engines.computeIfAbsent(dbId, (k) -> new ExtQueryEngine(k, core));
                if (betterMappings != null) {
                    engine.setBetterMappings(betterMappings.getMappings());
                }
                QueryResponse queryResponse = engine.query(q);
                engine.clearBetterMappings();
                return Response.status(Response.Status.OK).entity(buildUserResponse(queryResponse, dbId, namespace)).build();
            }
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        catch (Exception e) {
            logger.error(marker, "Query execution failed: '{}' on '{}'", q, dbId, e);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }






    public UserResponse buildUserResponse(QueryResponse queryResponse, String dbId, Namespace namespace) {

        if (queryResponse instanceof NoResponse) {
            return new NoUserResponse(queryResponse.getUserQuery(), queryResponse.getInteractionMode());
        }
        else if (queryResponse instanceof AnswerSetResponse) {
            AnswerSetResponse answerSet = (AnswerSetResponse) queryResponse;

            AnswerSetUserResponse response = new AnswerSetUserResponse(answerSet.getUserQuery(), answerSet.getInteractionMode());

            if (answerSet.getEntityAnswers() != null) {
                response.setEntityAnswers(EntityEntryCreator.createScoredEntityEntries(answerSet.getEntityAnswers(), dbId, namespace));
            }
            if (answerSet.getDocumentAnswers() != null) {
                response.setDocumentAnswers(EntityEntryCreator.createScoredDocumentEntries(answerSet.getDocumentAnswers(), dbId, namespace));
            }
            response.setTextAnswers(answerSet.getTextAnswers());
            response.setSparqlQuery(answerSet.getSparqlQuery());
            if (answerSet.getCoreEntity() != null) {
                response.setCoreEntity(EntityEntryCreator.createScoredEntityEntry(answerSet.getCoreEntity(), dbId, namespace));
            }
            response.setDocTypes(answerSet.getDocTypes());

            if (answerSet.getMappings() != null) {
                final Map<String, List<EntityEntry>> mappings = new HashMap<>();
                answerSet.getMappings().forEach((modelBinding, scoreList) -> {
                    List<EntityEntry> entries = EntityEntryCreator.createScoredEntityEntries(scoreList, dbId, namespace);
                    mappings.computeIfAbsent(modelBinding.getTerm(), (term) -> new ArrayList<>()).addAll(entries);
                });
                response.setMappings(mappings);
            }

            return response;
        }
        else if (queryResponse instanceof SPARQLSelectResponse) {
            SPARQLSelectResponse selectResponse = (SPARQLSelectResponse)queryResponse;

            final Map<String, List<EntityEntry>> bindings = new LinkedHashMap<>();
            selectResponse.getBindings().entrySet().forEach(e -> {
                List<EntityEntry> entries = EntityEntryCreator.createLabeledEntityEntries(e.getValue(), dbId, namespace);
                bindings.put(e.getKey(), entries);
            });

            SPARQLSelectUserResponse response = new SPARQLSelectUserResponse(selectResponse.getUserQuery(), selectResponse.getInteractionMode());
            response.setBindings(bindings);

            return response;
        }

        throw new UnsupportedOperationException("Can't create REST response");
    }
}
