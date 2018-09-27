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
import net.stargraph.core.search.SearchQueryGenerator;
import net.stargraph.rank.*;
import net.stargraph.rest.EntityEntry;
import net.stargraph.rest.SearchResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.ws.rs.core.Response;
import java.util.Arrays;
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
    public Response instanceSearch(String dbId, String instanceTerm, String model, int topk) {
        Namespace namespace = stargraph.getKBCore(dbId).getNamespace();

        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).searchSpaceLimit(topk); //TODO use resultLimit instead? or additional parameter?
        ModifiableSearchString searchString = ModifiableSearchString.create().searchTermsFromStr(instanceTerm);
        ModifiableRankParams rankParams = ParamsBuilder.get(model);
        Scores scores = entitySearcher.instanceSearch(searchParams, searchString, rankParams);
        List<EntityEntry> entityEntries = EntityEntryCreator.createScoredEntityEntries(scores, dbId, namespace);

        return Response.status(Response.Status.OK).entity(entityEntries).build();
    }

    @Override
    public Response classSearch(String dbId, String classTerm, String model, int topk) {
        Namespace namespace = stargraph.getKBCore(dbId).getNamespace();

        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).searchSpaceLimit(topk); //TODO use resultLimit instead? or additional parameter?
        ModifiableSearchString searchString = ModifiableSearchString.create().searchTermsFromStr(classTerm);
        ModifiableRankParams rankParams = ParamsBuilder.get(model);
        Scores scores = entitySearcher.classSearch(searchParams, searchString, rankParams);
        List<EntityEntry> entityEntries = EntityEntryCreator.createScoredEntityEntries(scores, dbId, namespace);

        return Response.status(Response.Status.OK).entity(entityEntries).build();
    }

    @Override
    public Response propertySearch(String dbId, String propertyTerm, String model, int topk) {
        Namespace namespace = stargraph.getKBCore(dbId).getNamespace();

        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).searchSpaceLimit(topk); //TODO use resultLimit instead? or additional parameter?
        ModifiableSearchString searchString = ModifiableSearchString.create().searchTermsFromStr(propertyTerm);
        ModifiableRankParams rankParams = ParamsBuilder.get(model);
        Scores scores = entitySearcher.propertySearch(searchParams, searchString, rankParams);
        List<EntityEntry> entityEntries = EntityEntryCreator.createScoredEntityEntries(scores, dbId, namespace);

        return Response.status(Response.Status.OK).entity(entityEntries).build();
    }

    @Override
    public Response pivotedSearch(String dbId, String id, String relationTerm, boolean incomingEdges, boolean outgoingEdges, int range, String model, int topk) {
        Namespace namespace = stargraph.getKBCore(dbId).getNamespace();

        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).searchSpaceLimit(topk); //TODO use resultLimit instead? or additional parameter?
        String rankString = relationTerm;
        ModifiableRankParams rankParams = ParamsBuilder.get(model);
        Scores scores = entitySearcher.pivotedSearch(id, searchParams, rankString, rankParams, incomingEdges, outgoingEdges, range, Arrays.asList(), Arrays.asList(SearchQueryGenerator.PropertyType.TYPE, SearchQueryGenerator.PropertyType.NON_TYPE), true, false);
        List<EntityEntry> entityEntries = EntityEntryCreator.createScoredEntityEntries(scores, dbId, namespace);

        return Response.status(Response.Status.OK).entity(entityEntries).build();
    }
}
