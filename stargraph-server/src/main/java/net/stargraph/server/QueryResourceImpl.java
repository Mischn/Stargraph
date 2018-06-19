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
import net.stargraph.model.LabeledEntity;
import net.stargraph.model.ValueEntity;
import net.stargraph.query.ExtQueryEngine;
import net.stargraph.rank.Score;
import net.stargraph.rest.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    // expand URIs by convention
    public static List<UserResponse.EntityEntry> createEntityEntries(List<LabeledEntity> entities, String dbId, Namespace namespace) {
        return entities.stream().map(e -> createEntityEntry(e, dbId, namespace)).collect(Collectors.toList());
    }
    public static UserResponse.EntityEntry createEntityEntry(LabeledEntity entity, String dbId, Namespace namespace) {
        return new UserResponse.EntityEntry(
                dbId,
                (entity instanceof ValueEntity)? UserResponse.EntityType.LITERAL: UserResponse.EntityType.INSTANCE,
                namespace.expandURI(entity.getId()),
                entity.getValue()
        );
    }

    // expand URIs by convention
    public static List<UserResponse.EntityEntry> createScoredEntityEntries(List<Score> entityScores, String dbId, Namespace namespace) {
        return entityScores.stream().map(s -> createScoredEntityEntry(s, dbId, namespace)).collect(Collectors.toList());
    }
    public static UserResponse.EntityEntry createScoredEntityEntry(Score entityScore, String dbId, Namespace namespace) {
        return new UserResponse.EntityEntry(
                dbId,
                (entityScore.getEntry() instanceof ValueEntity)? UserResponse.EntityType.LITERAL: UserResponse.EntityType.INSTANCE,
                namespace.expandURI(entityScore.getRankableView().getId()),
                entityScore.getRankableView().getValue(),
                entityScore.getValue()
        );
    }

    public UserResponse buildUserResponse(QueryResponse queryResponse, String dbId, Namespace namespace) {

        if (queryResponse instanceof NoResponse) {
            return new NoUserResponse(queryResponse.getUserQuery(), queryResponse.getInteractionMode());
        }
        else if (queryResponse instanceof AnswerSetResponse) {
            AnswerSetResponse answerSet = (AnswerSetResponse) queryResponse;
            SchemaAgnosticUserResponse response =
                    new SchemaAgnosticUserResponse(answerSet.getUserQuery(), answerSet.getInteractionMode(), answerSet.getSparqlQuery());

            List<UserResponse.EntityEntry> answers = createEntityEntries(answerSet.getEntityAnswer(), dbId, namespace);
            response.setAnswers(answers);

            final Map<String, List<UserResponse.EntityEntry>> mappings = new HashMap<>();
            answerSet.getMappings().forEach((modelBinding, scoreList) -> {
                List<UserResponse.EntityEntry> entries = createScoredEntityEntries(scoreList, dbId, namespace);
                mappings.computeIfAbsent(modelBinding.getTerm(), (term) -> new ArrayList<>()).addAll(entries);
            });

            response.setMappings(mappings);
            return response;
        }
        else if (queryResponse instanceof SPARQLSelectResponse) {
            SPARQLSelectResponse selectResponse = (SPARQLSelectResponse)queryResponse;
            final Map<String, List<UserResponse.EntityEntry>> bindings = new LinkedHashMap<>();
            selectResponse.getBindings().entrySet().forEach(e -> {
                List<UserResponse.EntityEntry> entries = createEntityEntries(e.getValue(), dbId, namespace);
                bindings.put(e.getKey(), entries);
            });

            SPARQLSelectUserResponse response =
                    new SPARQLSelectUserResponse(selectResponse.getUserQuery(), selectResponse.getInteractionMode());

            response.setBindings(bindings);
            return response;
        }

        throw new UnsupportedOperationException("Can't create REST response");
    }
}
