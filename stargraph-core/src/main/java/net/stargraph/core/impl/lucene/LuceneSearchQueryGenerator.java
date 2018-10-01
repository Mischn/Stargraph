package net.stargraph.core.impl.lucene;

import net.stargraph.StarGraphException;
import net.stargraph.core.Namespace;
import net.stargraph.core.Stargraph;
import net.stargraph.core.search.BaseSearchQueryGenerator;
import net.stargraph.core.search.SearchQueryHolder;
import net.stargraph.model.InstanceEntity;
import net.stargraph.rank.ModifiableSearchParams;
import net.stargraph.rank.ModifiableSearchString;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermsQuery;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    public SearchQueryHolder findClassFacts(List<String> subjIdList, List<String> objIdList, ModifiableSearchParams searchParams) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findInstanceInstances(ModifiableSearchParams searchParams, ModifiableSearchString searchString, boolean fuzzy, int maxEdits, boolean mustPhrases) {
        //Query query = new FuzzyQuery(new Term("value", searchParams.getSearchTerm()), maxEdits, 0, 50, false); // Problem: This does not take into account multiple words of the search term

        BooleanQuery query = new BooleanQuery.Builder()
                .add(myMatch("value", searchString, fuzzy, maxEdits, mustPhrases), BooleanClause.Occur.SHOULD)
                .add(myMatch("otherValues", searchString, fuzzy, maxEdits, mustPhrases), BooleanClause.Occur.SHOULD)
                .setMinimumNumberShouldMatch(1)
                .build();

        return new LuceneQueryHolder(query, searchParams);
    }

    @Override
    public SearchQueryHolder findClassInstances(ModifiableSearchParams searchParams, ModifiableSearchString searchString, boolean fuzzy, int maxEdits, boolean mustPhrases) {
        BooleanQuery query = new BooleanQuery.Builder()
                .add(boolQuery("isClass", true), BooleanClause.Occur.MUST)
                .add(myMatch("value", searchString, fuzzy, maxEdits, mustPhrases), BooleanClause.Occur.SHOULD)
                .add(myMatch("otherValues", searchString, fuzzy, maxEdits, mustPhrases), BooleanClause.Occur.SHOULD)
                .setMinimumNumberShouldMatch(1)
                .build();

        return new LuceneQueryHolder(query, searchParams);
    }

    @Override
    public SearchQueryHolder findPropertyInstances(ModifiableSearchParams searchParams, ModifiableSearchString searchString, boolean fuzzy, int maxEdits, boolean mustPhrases) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findDocumentInstances(ModifiableSearchParams searchParams, ModifiableSearchString searchString, List<String> docTypes, boolean entityDocument, boolean fuzzy, int maxEdits, boolean mustPhrases) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findPivotFacts(InstanceEntity pivot, ModifiableSearchParams searchParams, boolean inSubject, boolean inObject, PropertyTypes propertyTypes) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public SearchQueryHolder findSimilarDocuments(ModifiableSearchParams searchParams, ModifiableSearchString searchString, List<String> docTypes, boolean entityDocument) {
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





    private Query myMatch(String field, ModifiableSearchString searchString, boolean fuzzy, int maxEdits, boolean mustPhrases) {
        if (searchString.hasSearchPhrases()) {
            return phrasesMatch(field, searchString.getSearchPhrases(), fuzzy, maxEdits, mustPhrases);
        } else {
            return termsMatch(field, searchString.getSearchTerms(), fuzzy, maxEdits);
        }
    }

    private static Query termsMatch(String field, List<String> searchTerms, boolean fuzzy, int maxEdits) {
        return matchHelper(field, searchTerms.stream().collect(Collectors.joining(" ")), fuzzy, maxEdits, false);
    }

    // this is not really a phrase query since the order of the terms in a phrase is not considered, but ES' MatchPhraseQuery does not support fuzziness.
    private static Query phrasesMatch(String field, List<ModifiableSearchString.Phrase> phrases, boolean fuzzy, int maxEdits, boolean mustPhrases) {
        BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();

        for (ModifiableSearchString.Phrase phrase : phrases) {
            Query b = matchHelper(field, phrase.getText(), fuzzy, maxEdits, true);

            // boost
            b = new BoostQuery(b, phrase.getBoost());

            if (mustPhrases) {
                boolQueryBuilder.add(b, BooleanClause.Occur.MUST);
            } else {
                boolQueryBuilder.add(b, BooleanClause.Occur.SHOULD);
            }
        }

        if (!mustPhrases) {
            boolQueryBuilder.setMinimumNumberShouldMatch(1);
        }

        return boolQueryBuilder.build();
    }

    private static Query matchHelper(String field, String searchTerm, boolean fuzzy, int maxEdits, boolean and) {
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

}
