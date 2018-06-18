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
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.rank.ModifiableRankParams;
import net.stargraph.rank.ModifiableSearchParams;
import net.stargraph.rank.ParamsBuilder;
import net.stargraph.rank.Scores;
import net.stargraph.rest.SearchResource;
import net.stargraph.rest.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;

final class SearchResourceImpl implements SearchResource {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private Marker marker = MarkerFactory.getMarker("server");

    private Stargraph stargraph;
    private EntitySearcher entitySearcher;

    SearchResourceImpl(Stargraph stargraph) {
        this.stargraph = Objects.requireNonNull(stargraph);
        this.entitySearcher = stargraph.getEntitySearcher();
    }

    @Override
    public Response resourceSearch(String dbId, String resourceTerm, String model, int topk) {
        Namespace namespace = stargraph.getKBCore(dbId).getNamespace();

        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).term(resourceTerm).limit(topk);
        ModifiableRankParams rankParams = ParamsBuilder.get(model);
        Scores scores = entitySearcher.resourceSearch(searchParams, rankParams);
        List<UserResponse.EntityEntry> entityEntries = QueryResourceImpl.createScoredEntityEntries(scores, namespace);

        return Response.status(Response.Status.OK).entity(entityEntries).build();
    }

    @Override
    public Response classSearch(String dbId, String classTerm, String model, int topk) {
        Namespace namespace = stargraph.getKBCore(dbId).getNamespace();

        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).term(classTerm).limit(topk);
        ModifiableRankParams rankParams = ParamsBuilder.get(model);
        Scores scores = entitySearcher.classSearch(searchParams, rankParams);
        List<UserResponse.EntityEntry> entityEntries = QueryResourceImpl.createScoredEntityEntries(scores, namespace);

        return Response.status(Response.Status.OK).entity(entityEntries).build();
    }

    @Override
    public Response propertySearch(String dbId, String propertyTerm, String model, int topk) {
        Namespace namespace = stargraph.getKBCore(dbId).getNamespace();

        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).term(propertyTerm).limit(topk);
        ModifiableRankParams rankParams = ParamsBuilder.get(model);
        Scores scores = entitySearcher.propertySearch(searchParams, rankParams);
        List<UserResponse.EntityEntry> entityEntries = QueryResourceImpl.createScoredEntityEntries(scores, namespace);

        return Response.status(Response.Status.OK).entity(entityEntries).build();
    }

    @Override
    public Response pivotedSearch(String dbId, String id, String relationTerm, boolean incomingEdges, boolean outgoingEdges, int range, String model, int topk) {
        Namespace namespace = stargraph.getKBCore(dbId).getNamespace();

        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).term(relationTerm).limit(topk);
        ModifiableRankParams rankParams = ParamsBuilder.get(model);
        Scores scores = entitySearcher.pivotedSearch(id, searchParams, rankParams, incomingEdges, outgoingEdges, range, false);
        List<UserResponse.EntityEntry> entityEntries = QueryResourceImpl.createScoredEntityEntries(scores, namespace);

        return Response.status(Response.Status.OK).entity(entityEntries).build();
    }
}
