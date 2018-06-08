package net.stargraph.core.impl.lucene;

import net.stargraph.StarGraphException;
import net.stargraph.core.Namespace;
import net.stargraph.core.Stargraph;
import net.stargraph.core.search.BaseSearchQueryGenerator;
import net.stargraph.core.search.SearchQueryHolder;
import net.stargraph.model.ResourceEntity;
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

    @Override
    public SearchQueryHolder entitiesWithIds(List<String> idList, ModifiableSearchParams searchParams) {
        Namespace namespace = getNamespace();

        List<Term> terms = new ArrayList<>();
        idList.stream().map(namespace::shrinkURI).forEach(id -> {
            terms.add(new Term("id", id));
        });

        Query query = new TermsQuery(terms);

        return new LuceneQueryHolder(query, searchParams);
    }

    @Override
    public SearchQueryHolder propertiesWithIds(List<String> idList, ModifiableSearchParams searchParams) {
        Namespace namespace = getNamespace();

        List<Term> terms = new ArrayList<>();
        idList.stream().map(namespace::shrinkURI).forEach(id -> {
            terms.add(new Term("id", id));
        });

        Query query = new TermsQuery(terms);

        return new LuceneQueryHolder(query, searchParams);
    }

    @Override
    public SearchQueryHolder findClassFacts(List<String> idList, boolean inSubject, ModifiableSearchParams searchParams) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findResourceInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits) {
        //Query query = new FuzzyQuery(new Term("value", searchParams.getSearchTerm()), maxEdits, 0, 50, false); // This does not take into account multiple words of the search term

        BooleanQuery query = new BooleanQuery.Builder()
                .add(fuzzyMatch("value", searchParams.getSearchTerm(), fuzzy, maxEdits), BooleanClause.Occur.SHOULD)
                .add(fuzzyMatch("otherValues", searchParams.getSearchTerm(), fuzzy, maxEdits), BooleanClause.Occur.SHOULD)
                .setMinimumNumberShouldMatch(1)
                .build();

        return new LuceneQueryHolder(query, searchParams);
    }

    @Override
    public SearchQueryHolder findClassInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits) {
        BooleanQuery query = new BooleanQuery.Builder()
                .add(boolQuery("isClass", true), BooleanClause.Occur.MUST)
                .add(fuzzyMatch("value", searchParams.getSearchTerm(), fuzzy, maxEdits), BooleanClause.Occur.SHOULD)
                .add(fuzzyMatch("otherValues", searchParams.getSearchTerm(), fuzzy, maxEdits), BooleanClause.Occur.SHOULD)
                .setMinimumNumberShouldMatch(1)
                .build();

        return new LuceneQueryHolder(query, searchParams);
    }

    @Override
    public SearchQueryHolder findPropertyInstances(ModifiableSearchParams searchParams, boolean fuzzy, int maxEdits) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findPivotFacts(ResourceEntity pivot, ModifiableSearchParams searchParams, boolean inSubject, boolean inObject) {
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

    private static Query matchQuery(String field, String searchTerm) {
        StringBuilder queryStr = new StringBuilder();
        queryStr.append(field).append(":(").append(searchTerm.trim()).append(")");

        try {
            return new ComplexPhraseQueryParser(field, new StandardAnalyzer()).parse(queryStr.toString());
        } catch (ParseException e) {
            throw new StarGraphException(e);
        }
    }

    private static Query fuzzyMatchQuery(String field, String searchTerm, int maxEdits) {
        StringBuilder queryStr = new StringBuilder();
        queryStr.append(field).append(":(");

        String words[] = searchTerm.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                queryStr.append(" AND ");
            }
            queryStr.append(ComplexPhraseQueryParser.escape(words[i])).append("~").append(maxEdits);
        }
        queryStr.append(")");

        try {
            return new ComplexPhraseQueryParser(field, new StandardAnalyzer()).parse(queryStr.toString());
        } catch (ParseException e) {
            throw new StarGraphException(e);
        }
    }

    private static Query fuzzyMatch(String field, String searchTerm, boolean fuzzy, int maxEdits) {
        if (fuzzy) {
            return fuzzyMatchQuery(field, searchTerm, maxEdits);
        } else {
            return matchQuery(field, searchTerm);
        }
    }
}
