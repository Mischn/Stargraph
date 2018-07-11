package net.stargraph.core.impl.lucene;

import net.stargraph.StarGraphException;
import net.stargraph.core.Namespace;
import net.stargraph.core.Stargraph;
import net.stargraph.core.search.BaseSearchQueryGenerator;
import net.stargraph.core.search.SearchQueryHolder;
import net.stargraph.model.InstanceEntity;
import net.stargraph.rank.ModifiableSearchParams;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermsQuery;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.util.ArrayList;
import java.util.List;

public class LuceneSearchQueryGenerator extends BaseSearchQueryGenerator {

    public LuceneSearchQueryGenerator(Stargraph stargraph, String dbId) {
        super(stargraph, dbId);
    }

    private SearchQueryHolder withIds(List<String> idList, ModifiableSearchParams searchParams) {
        Namespace namespace = getNamespace();

        List<Term> terms = new ArrayList<>();
        idList.stream().map(namespace::shrinkURI).forEach(id -> {
            terms.add(new Term("id", id));
        });

        Query query = new TermsQuery(terms);

        return new LuceneQueryHolder(query, searchParams);
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
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findClassFacts(List<String> idList, boolean inSubject, ModifiableSearchParams searchParams) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findInstanceInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits, boolean and) {
        //Query query = new FuzzyQuery(new Term("value", searchParams.getSearchTerm()), maxEdits, 0, 50, false); // Problem: This does not take into account multiple words of the search term

        BooleanQuery query = new BooleanQuery.Builder()
                .add(myMatch("value", searchParams, fuzzy, maxEdits, and), BooleanClause.Occur.SHOULD)
                .add(myMatch("otherValues", searchParams, fuzzy, maxEdits, and), BooleanClause.Occur.SHOULD)
                .setMinimumNumberShouldMatch(1)
                .build();

        return new LuceneQueryHolder(query, searchParams);
    }

    @Override
    public SearchQueryHolder findClassInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits, boolean and) {
        BooleanQuery query = new BooleanQuery.Builder()
                .add(boolQuery("isClass", true), BooleanClause.Occur.MUST)
                .add(myMatch("value", searchParams, fuzzy, maxEdits, and), BooleanClause.Occur.SHOULD)
                .add(myMatch("otherValues", searchParams, fuzzy, maxEdits, and), BooleanClause.Occur.SHOULD)
                .setMinimumNumberShouldMatch(1)
                .build();

        return new LuceneQueryHolder(query, searchParams);
    }

    @Override
    public SearchQueryHolder findPropertyInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits, boolean and) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findDocumentInstances(ModifiableSearchParams searchParams, List<String> docTypes, boolean entityDocument, boolean fuzzy, int maxEdits, boolean and) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findPivotFacts(InstanceEntity pivot, ModifiableSearchParams searchParams, boolean inSubject, boolean inObject) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findSimilarDocuments(ModifiableSearchParams searchParams, List<String> docTypes, boolean entityDocument) {
        throw new UnsupportedOperationException("Not implemented");
    }



    private static Query boolQuery(String field, boolean value) {
        StringBuilder queryStr = new StringBuilder();
        queryStr.append(field).append(":").append(value);

        try {
            return new ComplexPhraseQueryParser(field, new StandardAnalyzer()).parse(queryStr.toString());
        } catch (ParseException e) {
            throw new StarGraphException(e);
        }
    }






    private Query myMatch(String field, ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits, boolean and) {
        if (searchParams.hasPhrases()) {
            BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
            for (Query b : phraseMatch(field, searchParams.getPhrases(), fuzzy, maxEdits)) {
                if (and) {
                    boolQueryBuilder.add(b, BooleanClause.Occur.MUST);
                } else {
                    boolQueryBuilder.add(b, BooleanClause.Occur.SHOULD);
                }
            }
            if (!and) {
                boolQueryBuilder.setMinimumNumberShouldMatch(1);
            }
            return boolQueryBuilder.build();
        } else {
            return termsMatch(field, searchParams.getSearchTerm(), fuzzy, maxEdits, and);
        }
    }

    private static Query termsMatch(String field, String searchTerm, boolean fuzzy, int maxEdits, boolean and) {
        StringBuilder queryStr = new StringBuilder();
        queryStr.append(field).append(":(");

        String words[] = searchTerm.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                if (and) {
                    queryStr.append(" AND ");
                } else {
                    queryStr.append(" OR ");
                }
            }
            queryStr.append(ComplexPhraseQueryParser.escape(words[i]));
            if (fuzzy) {
                queryStr.append("~").append(maxEdits);
            }
        }
        queryStr.append(")");

        try {
            return new ComplexPhraseQueryParser(field, new StandardAnalyzer()).parse(queryStr.toString());
        } catch (ParseException e) {
            throw new StarGraphException(e);
        }
    }

    // this is not really a phrase query since the order of the terms in a phrase is not considered, but ES' MatchPhraseQuery does not support fuzziness.
    private static List<Query> phraseMatch(String field, List<String> phrases, boolean fuzzy, int maxEdits) {
        List<Query> res = new ArrayList<>();
        for (String phrase : phrases) {
            res.add(termsMatch(field, phrase, fuzzy, maxEdits, true));
        }
        return res;
    }
}
