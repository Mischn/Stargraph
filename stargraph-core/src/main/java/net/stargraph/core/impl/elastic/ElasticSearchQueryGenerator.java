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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

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
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public SearchQueryHolder findInstanceInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits, boolean and) {
        QueryBuilder queryBuilder = boolQuery()
                .should(myMatch("value", searchParams, fuzzy, maxEdits, and))
                .should(myMatch("otherValues", searchParams, fuzzy, maxEdits, and))
                .minimumNumberShouldMatch(1);

        return new ElasticQueryHolder(queryBuilder, searchParams);
    }

    @Override
    public SearchQueryHolder findClassInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits, boolean and) {
        QueryBuilder queryBuilder = boolQuery()
                .must(termQuery("isClass", true))
                .should(myMatch("value", searchParams, fuzzy, maxEdits, and))
                .should(myMatch("otherValues", searchParams, fuzzy, maxEdits, and))
                .minimumNumberShouldMatch(1);

        return new ElasticQueryHolder(queryBuilder, searchParams);
    }

    @Override
    public SearchQueryHolder findPropertyInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits, boolean and) {
        QueryBuilder queryBuilder = boolQuery()
                .should(nestedQuery("hyponyms",
                        myMatch("hyponyms.word", searchParams, fuzzy, maxEdits, and), ScoreMode.Max))
                .should(nestedQuery("hypernyms",
                        myMatch("hypernyms.word", searchParams, fuzzy, maxEdits, and), ScoreMode.Max))
                .should(nestedQuery("synonyms",
                        myMatch("synonyms.word", searchParams, fuzzy, maxEdits, and), ScoreMode.Max))
                .minimumNumberShouldMatch(1);

        return new ElasticQueryHolder(queryBuilder, searchParams);
    }

    @Override
    public SearchQueryHolder findDocumentInstances(ModifiableSearchParams searchParams, List<String> docTypes, boolean entityDocument, boolean fuzzy, int maxEdits, boolean and) {
        BoolQueryBuilder queryBuilder = boolQuery();
        if (docTypes != null) {
            queryBuilder.must(termsQuery("type", docTypes));
        }
        if (entityDocument) {
            queryBuilder.must(existsQuery("entity")).mustNot(termQuery("entity", "null"));
        }
        queryBuilder.should(myMatch("text", searchParams, fuzzy, maxEdits, and))
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
        String[] txts = (searchParams.hasPhrases())? searchParams.getPhrases().toArray(new String[searchParams.getPhrases().size()]) : new String[]{ searchParams.getSearchTerm() };
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




    private QueryBuilder myMatch(String field, ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits, boolean and) {
        if (searchParams.hasPhrases()) {
            BoolQueryBuilder boolQueryBuilder = boolQuery();
            for (MatchQueryBuilder b : phraseMatch(field, searchParams.getPhrases(), fuzzy, maxEdits)) {
                if (and) {
                    boolQueryBuilder.must(b);
                } else {
                    boolQueryBuilder.should(b);
                }
            }
            if (!and) {
                boolQueryBuilder.minimumNumberShouldMatch(1);
            }
            return boolQueryBuilder;
        } else {
            return termsMatch(field, searchParams.getSearchTerm(), fuzzy, maxEdits, and);
        }
    }

    private static MatchQueryBuilder termsMatch(String field, String searchTerms, boolean fuzzy, int maxEdits, boolean and) {
        MatchQueryBuilder queryBuilder = matchQuery(field, searchTerms);
        if (fuzzy) {
            queryBuilder.fuzziness(maxEdits).fuzzyTranspositions(false);
        }
        queryBuilder.operator((and) ? Operator.AND : Operator.OR);
        return queryBuilder;
    }

    // this is not really a phrase query since the order of the terms in a phrase is not considered, but ES' MatchPhraseQuery does not support fuzziness.
    private static List<MatchQueryBuilder> phraseMatch(String field, List<String> phrases, boolean fuzzy, int maxEdits) {
        List<MatchQueryBuilder> res = new ArrayList<>();
        for (String phrase : phrases) {
            res.add(termsMatch(field, phrase, fuzzy, maxEdits, true));
        }
        return res;
    }
}
