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
import net.stargraph.core.query.QueryEngine;
import net.stargraph.core.query.QueryResponse;
import net.stargraph.core.query.filter.FilterResult;
import net.stargraph.core.query.nli.DataModelBinding;
import net.stargraph.core.query.nli.DataModelBindingContext;
import net.stargraph.core.query.response.AnswerSetResponse;
import net.stargraph.core.query.response.NoResponse;
import net.stargraph.core.query.response.SPARQLSelectResponse;
import net.stargraph.model.PassageExtraction;
import net.stargraph.model.date.TimeRange;
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
    private Map<String, QueryEngine> engines;

    public QueryResourceImpl(Stargraph core) {
        this.core = Objects.requireNonNull(core);
        this.engines = new ConcurrentHashMap<>();
    }

    @Override
    public Response query(String dbId, String q) {
        return query(dbId, q, null);
    }

    @Override
    public Response query(String dbId, String q, BetterQueryBean betterQueryBean) {
        if (!core.hasKB(dbId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Namespace namespace = core.getKBCore(dbId).getNamespace();
        QueryEngine engine = engines.computeIfAbsent(dbId, (k) -> new QueryEngine(k, core));
        try {
            if (betterQueryBean != null) {

                // query plans
                if (betterQueryBean.getQueryPlans() != null) {
                    engine.setCustomQueryPlans(betterQueryBean.getQueryPlans());
                }

                // mappings
                if (betterQueryBean.getMappings() != null) {

                    // convert to proper custom mappings
                    Map<String, Map<DataModelBindingContext, List<String>>> customMappings = new HashMap<>();
                    for (String placeholder : betterQueryBean.getMappings().keySet()) {
                        for (String c : betterQueryBean.getMappings().get(placeholder).keySet()) {
                            DataModelBindingContext context = DataModelBindingContext.valueOf(c);
                            customMappings.computeIfAbsent(placeholder, (k) -> new HashMap<>()).put(context, betterQueryBean.getMappings().get(placeholder).get(c));
                        }
                    }

                    engine.setCustomMappings(customMappings);
                }
            }

            QueryResponse queryResponse = engine.query(q);

            return Response.status(Response.Status.OK).entity(buildUserResponse(queryResponse, dbId, namespace)).build();
        }
        catch (Exception e) {
            logger.error(marker, "Query execution failed: '{}' on '{}'", q, dbId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            engine.clearCustomQueryPlans();
            engine.clearCustomMappings();
        }
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
            if (answerSet.getTextAnswers() != null) {
                response.setTextAnswers(answerSet.getTextAnswers());
            }

            if (answerSet.getQueryPlans() != null) {
                response.setQueryPlans(answerSet.getQueryPlans().stream().map(p -> p.toString()).collect(Collectors.toList()));
            }
            if (answerSet.getCoreEntity() != null) {
                response.setCoreEntity(EntityEntryCreator.createScoredEntityEntry(answerSet.getCoreEntity(), dbId, namespace));
            }
            if (answerSet.getDocTypes() != null) {
                response.setDocTypes(answerSet.getDocTypes());
            }
            if (answerSet.getQueryFilters() != null) {
                response.setQueryFilters(createQueryFilters(answerSet.getQueryFilters()));
            }
            if (answerSet.getFilterResults() != null) {
                response.setFilterResults(createFilterResults(answerSet.getFilterResults()));
            }

            if (answerSet.getSparqlQueries() != null) {
                response.setSparqlQueries(answerSet.getSparqlQueries());
            }
            if (answerSet.getBindings() != null) {
                response.setBindings(createBindings(answerSet.getBindings()));
            }
            if (answerSet.getPossibleMappings() != null) {
                response.setPossibleMappings(createMappings(answerSet.getPossibleMappings(), dbId, namespace));
            }
            if (answerSet.getMappings() != null) {
                response.setMappings(createMappings(answerSet.getMappings(), dbId, namespace));
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


    private static Map<String, String> createBindings(Map<String, DataModelBinding> bindings) {
        Map<String, String> res = new HashMap<>();
        for (String placeholder : bindings.keySet()) {
            res.put(placeholder, bindings.get(placeholder).getTerm());
        }
        return res;
    }

    private static Map<String, Map<String, Set<EntityEntry>>> createMappings(Map<String, Map<DataModelBindingContext, Set<Score>>> mappings, String dbId, Namespace namespace) {
        Map<String, Map<String, Set<EntityEntry>>> res = new HashMap<>();
        for (String placeholder : mappings.keySet()) {
            for (DataModelBindingContext context : mappings.get(placeholder).keySet()) {
                Set<EntityEntry> entries = new LinkedHashSet<>();
                EntityEntryCreator.createScoredEntityEntries(mappings.get(placeholder).get(context).stream().collect(Collectors.toList()), dbId, namespace).forEach(x -> entries.add(x));
                res.computeIfAbsent(placeholder, (k) -> new HashMap<>()).put(context.name(), entries);
            }
        }
        return res;
    }


    private static List<FilterEntry> createQueryFilters(List<PassageExtraction> queryFilters) {
        List<FilterEntry> res = new ArrayList<>();
        for (PassageExtraction queryFilter : queryFilters) {
            String rel = queryFilter.getRelation();
            List<String> args = new ArrayList<>();
            for (String s : queryFilter.getTerms()) {
                args.add(s);
            }
            for (TimeRange tr : queryFilter.getTemporals()) {
                args.add(tr.toString());
            }
            res.add(new FilterEntry(rel, args));
        }

        return res;
    }

    private static List<FilterResultEntry> createFilterResults(List<FilterResult> filterResults) {
        List<FilterResultEntry> res = new ArrayList<>();
        for (FilterResult filterResult : filterResults) {

            List<FilterResultEntry.SingleFilterResult> sfrs = new ArrayList<>();
            for (FilterResult.SingleFilterResult singleFilterResult : filterResult.getSingleFilterResults()) {

                String matchedRel = singleFilterResult.isMatchedRelation()? singleFilterResult.getMatchedRelation().getEntry().toString() : null;

                List<String> matchedArgs = new ArrayList<>();
                for (Score score : singleFilterResult.getMatchedTerms()) {
                    matchedArgs.add((score != null)? score.getEntry().toString() : null);
                }
                for (Score score : singleFilterResult.getMatchedTemporals()) {
                    matchedArgs.add((score != null)? score.getEntry().toString() : null);
                }

                sfrs.add(new FilterResultEntry.SingleFilterResult(
                        matchedRel,
                        matchedArgs
                ));
            }

            FilterResultEntry fre = new FilterResultEntry(filterResult.getDocId(), filterResult.getEntityId(), sfrs);
            res.add(fre);
        }

        return res;
    }
}
