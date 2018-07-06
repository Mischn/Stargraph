package net.stargraph.core.impl.elastic;

/*-
 * ==========================License-Start=============================
 * stargraph-core
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
import net.stargraph.core.search.BaseSearchQueryGenerator;
import net.stargraph.core.search.SearchQueryHolder;
import net.stargraph.model.InstanceEntity;
import net.stargraph.rank.ModifiableSearchParams;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;

public class ElasticSearchQueryGenerator extends BaseSearchQueryGenerator {

    public ElasticSearchQueryGenerator(Stargraph stargraph, String dbId) {
        super(stargraph, dbId);
    }

    private SearchQueryHolder withIds(List<String> idList, ModifiableSearchParams searchParams) {
        Namespace namespace = getNamespace();
        QueryBuilder queryBuilder = termsQuery("id", idList.stream().map(namespace::shrinkURI).collect(Collectors.toList()));

        return new ElasticQueryHolder(queryBuilder, searchParams);
    }

    @Override
    public SearchQueryHolder entitiesWithIds(List<String> idList, ModifiableSearchParams searchParams) {
        return withIds(idList, searchParams);
    }

    @Override
    public SearchQueryHolder propertiesWithIds(List<String> idList, ModifiableSearchParams searchParams) {
        return withIds(idList, searchParams);
    }

    @Override
    public SearchQueryHolder documentsWithIds(List<String> idList, ModifiableSearchParams searchParams) {
        return withIds(idList, searchParams);
    }

    @Override
    public SearchQueryHolder documentsForEntityIds(List<String> idList, List<String> docTypes, ModifiableSearchParams searchParams) {
        Namespace namespace = getNamespace();
        BoolQueryBuilder queryBuilder = boolQuery();
        if (docTypes != null) {
            queryBuilder.must(termsQuery("type", docTypes));
        }
        queryBuilder.should(termsQuery("entity", idList.stream().map(namespace::shrinkURI).collect(Collectors.toList())))
        .minimumNumberShouldMatch(1);

        return new ElasticQueryHolder(queryBuilder, searchParams);
    }

    @Override
    public SearchQueryHolder findClassFacts(List<String> idList, boolean inSubject, ModifiableSearchParams searchParams) {
//        Namespace namespace = getNamespace();
//        QueryBuilder queryBuilder;
//        if (inSubject) {
//            queryBuilder = boolQuery()
//                    .must(nestedQuery("s",
//                            constantScoreQuery(termsQuery("s.id", idList.stream().map(namespace::shrinkURI).collect(Collectors.toList()))), ScoreMode.Max))
//                    .must(nestedQuery("p",
//                            termQuery("p.id", FactClassifierProcessor.CLASS_RELATION_STR), ScoreMode.Max));
//        } else {
//            queryBuilder = boolQuery()
//                    .must(nestedQuery("p",
//                            termQuery("p.id", FactClassifierProcessor.CLASS_RELATION_STR), ScoreMode.Max))
//                    .must(nestedQuery("o",
//                            constantScoreQuery(termsQuery("o.id", idList.stream().map(namespace::shrinkURI).collect(Collectors.toList()))), ScoreMode.Max));
//        }
//
//        return new ElasticQueryHolder(queryBuilder, searchParams);
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public SearchQueryHolder findInstanceInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits) {
        QueryBuilder queryBuilder = boolQuery()
                .should(fuzzyMatch(matchQuery("value", searchParams.getSearchTerm()), fuzzy, maxEdits))
                .should(fuzzyMatch(matchQuery("otherValues", searchParams.getSearchTerm()), fuzzy, maxEdits))
                .minimumNumberShouldMatch(1);

        return new ElasticQueryHolder(queryBuilder, searchParams);
    }

    @Override
    public SearchQueryHolder findClassInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits) {
        QueryBuilder queryBuilder = boolQuery()
                .must(termQuery("isClass", true))
                .should(fuzzyMatch(matchQuery("value", searchParams.getSearchTerm()), fuzzy, maxEdits))
                .should(fuzzyMatch(matchQuery("otherValues", searchParams.getSearchTerm()), fuzzy, maxEdits))
                .minimumNumberShouldMatch(1);

        return new ElasticQueryHolder(queryBuilder, searchParams);
    }

    @Override
    public SearchQueryHolder findPropertyInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits) {
        QueryBuilder queryBuilder = boolQuery()
                .should(nestedQuery("hyponyms",
                        fuzzyMatch(matchQuery("hyponyms.word", searchParams.getSearchTerm()), fuzzy, maxEdits), ScoreMode.Max))
                .should(nestedQuery("hypernyms",
                        fuzzyMatch(matchQuery("hypernyms.word", searchParams.getSearchTerm()), fuzzy, maxEdits), ScoreMode.Max))
                .should(nestedQuery("synonyms",
                        fuzzyMatch(matchQuery("synonyms.word", searchParams.getSearchTerm()), fuzzy, maxEdits), ScoreMode.Max))
                .minimumNumberShouldMatch(1);

        return new ElasticQueryHolder(queryBuilder, searchParams);
    }

    @Override
    public SearchQueryHolder findDocumentInstances(ModifiableSearchParams searchParams, List<String> docTypes, boolean entityDocument, boolean fuzzy, int maxEdits) {
        BoolQueryBuilder queryBuilder = boolQuery();
        if (docTypes != null) {
            queryBuilder.must(termsQuery("type", docTypes));
        }
        if (entityDocument) {
            queryBuilder.must(existsQuery("entity")).mustNot(termQuery("entity", "null"));
        }
        queryBuilder.should(fuzzyMatch(matchQuery("text", searchParams.getSearchTerm()), fuzzy, maxEdits))
                .minimumNumberShouldMatch(1);

        return new ElasticQueryHolder(queryBuilder, searchParams);
    }

    @Override
    // There is a clear performance-loss when both inSubject and inObject are enabled together.
    public SearchQueryHolder findPivotFacts(InstanceEntity pivot, ModifiableSearchParams searchParams, boolean inSubject, boolean inObject) {
        Namespace namespace = getNamespace();
        String id = namespace.shrinkURI(pivot.getId());

        BoolQueryBuilder queryBuilder = boolQuery();
        if (inSubject) {
            queryBuilder.should(nestedQuery("s", termQuery("s.id", id), ScoreMode.Max));
        }
        if (inObject) {
            queryBuilder.should(nestedQuery("o", termQuery("o.id", id), ScoreMode.Max));
        }
        queryBuilder.minimumNumberShouldMatch(1);

        return new ElasticQueryHolder(queryBuilder, searchParams);
    }

    @Override
    public SearchQueryHolder findSimilarDocuments(ModifiableSearchParams searchParams, List<String> docTypes, boolean entityDocument) {
        String[] fields = {"text"};
        String[] txts = searchParams.getSearchTerms().toArray(new String[searchParams.getSearchTerms().size()]);
        MoreLikeThisQueryBuilder.Item[] items = null;

        BoolQueryBuilder queryBuilder = boolQuery();
        if (docTypes != null) {
            queryBuilder.must(termsQuery("type", docTypes));
        }
        if (entityDocument) {
            queryBuilder.must(existsQuery("entity")).mustNot(termQuery("entity", "null"));
        }
        queryBuilder.should(moreLikeThisQuery(fields, txts, items)
                .minTermFreq(1)
                .maxQueryTerms(12)
                .minDocFreq(1))
                .minimumNumberShouldMatch(1);


        return new ElasticQueryHolder(queryBuilder, searchParams);
    }

    private static MatchQueryBuilder fuzzyMatch(MatchQueryBuilder matchQueryBuilder, boolean fuzzy, int maxEdits) {
        if (fuzzy) {
            return matchQueryBuilder.fuzziness(maxEdits).fuzzyTranspositions(false).operator(Operator.AND);
        } else {
            return matchQueryBuilder;
        }
    }
}
