package net.stargraph.core.query;

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

import net.stargraph.StarGraphException;
import net.stargraph.core.KBCore;
import net.stargraph.core.Namespace;
import net.stargraph.core.Stargraph;
import net.stargraph.core.graph.GraphSearcher;
import net.stargraph.core.query.nli.*;
import net.stargraph.core.query.response.AnswerSetResponse;
import net.stargraph.core.query.response.NoResponse;
import net.stargraph.core.query.response.SPARQLSelectResponse;
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.Document;
import net.stargraph.model.InstanceEntity;
import net.stargraph.model.LabeledEntity;
import net.stargraph.model.PassageExtraction;
import net.stargraph.model.date.TimeRange;
import net.stargraph.query.InteractionMode;
import net.stargraph.query.Language;
import net.stargraph.rank.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static net.stargraph.query.InteractionMode.*;

public class QueryEngine {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("query");

    protected Stargraph stargraph;
    protected String dbId;
    protected KBCore core;
    protected Analyzers analyzers;
    protected GraphSearcher graphSearcher;
    protected EntitySearcher entitySearcher;
    protected InteractionModeSelector modeSelector;
    protected Namespace namespace;
    protected Language language;

    public QueryEngine(String dbId, Stargraph stargraph) {
        this.stargraph = stargraph;
        this.dbId = Objects.requireNonNull(dbId);
        this.core = Objects.requireNonNull(stargraph.getKBCore(dbId));
        this.analyzers = new Analyzers(stargraph, dbId);
        this.graphSearcher = core.getGraphSearcher();
        this.entitySearcher = stargraph.getEntitySearcher();
        this.namespace = core.getNamespace();
        this.language = core.getLanguage();
        this.modeSelector = new InteractionModeSelector(stargraph.getMainConfig(), language);
    }

    public QueryResponse query(String query) {
        final InteractionMode mode = modeSelector.detect(query);
        QueryResponse response = new NoResponse(mode, query);

        long startTime = System.currentTimeMillis();
        try {
            switch (mode) {
                case SA_SPARQL: //TODO ?
                case SA_SIMPLE_SPARQL: //TODO ?
                case NLI:
                    response = nliQuery(query, language);
                    break;
                case SPARQL:
                    response = sparqlQuery(query);
                    break;
                case ENTITY_SIMILARITY:
                    response = entitySimilarityQuery(query, language);
                    break;
                case LIKE_THIS:
                    response = likeThisQuery(query, language);
                    break;
                case FILTER:
                    response = filterQuery(query, language);
                    break;
                case DEFINITION:
                    response = definitionQuery(query, language);
                    break;
                default:
                    throw new StarGraphException("Input type not yet supported");
            }

            return response;

        }
        catch (Exception e) {
            logger.error(marker, "Query Error '{}'", query, e);
            throw new StarGraphException("Query Error", e);
        }
        finally {
            long millis = System.currentTimeMillis() - startTime;
            logger.info(marker, "Query Engine took {}s Response: {}",  millis / 1000.0, response);
        }
    }

    private QueryResponse sparqlQuery(String userQuery) {
        Map<String, List<LabeledEntity>> vars = graphSearcher.select(userQuery);
        if (!vars.isEmpty()) {
            return new SPARQLSelectResponse(SPARQL, userQuery, vars);
        }
        return new NoResponse(SPARQL, userQuery);
    }

    private QueryResponse nliQuery(String userQuery, Language language) {
        QuestionAnalyzer analyzer = this.analyzers.getQuestionAnalyzer(language);
        QuestionAnalysis analysis = analyzer.analyse(userQuery);
        SPARQLQueryBuilder queryBuilder = analysis.getSPARQLQueryBuilder();
        queryBuilder.setNS(namespace);

        QueryPlanPatterns triplePatterns = queryBuilder.getTriplePatterns();
        List<DataModelBinding> bindings = queryBuilder.getBindings();

        triplePatterns.forEach(triplePattern -> {
            Triple triple = asTriple(triplePattern, bindings);
            resolve(triple, queryBuilder);
        });

        String sparqlQueryStr = queryBuilder.build();

        Map<String, List<LabeledEntity>> vars = graphSearcher.select(sparqlQueryStr);

        if (!vars.isEmpty()) {
            AnswerSetResponse answerSet = new AnswerSetResponse(NLI, userQuery, queryBuilder);

            Set<Score> entityAnswers = vars.get("VAR_1").stream()
                    .map(e -> new Score(namespace.expand(e), 1)).collect(Collectors.toSet());

            answerSet.setEntityAnswers(new ArrayList<>(entityAnswers)); // convention, answer must be bound to the first var
            answerSet.setMappings(queryBuilder.getMappings());
            answerSet.setSPARQLQuery(sparqlQueryStr);

            System.out.println("-----> " + answerSet.getMappings());
            //
            //if (triplePattern.getTypes().contains("VARIABLE TYPE CLASS")) {
            //    entities = core.getEntitySearcher().searchByTypes(new HashSet<String>(Arrays.asList(triplePattern.objectLabel.split(" "))), true, 100);
            //}

            return answerSet;
        }

        return new NoResponse(NLI, userQuery);
    }

    private QueryResponse entitySimilarityQuery(String userQuery, Language language) {
        List<String> docTypes = core.getDocTypes();

        EntityQueryBuilder queryBuilder = new EntityQueryBuilder();
        EntityQuery query = queryBuilder.parse(userQuery, ENTITY_SIMILARITY);
        Scores coreEntityScores = resolveScoredInstance(query.getCoreEntity());
        Score coreEntityScore = coreEntityScores.get(0);

        Scores entityScores = entitySearcher.similarInstanceSearch(dbId, (InstanceEntity)coreEntityScore.getEntry(), docTypes, null);
        if (!entityScores.isEmpty()) {
            AnswerSetResponse answerSet = new AnswerSetResponse(ENTITY_SIMILARITY, userQuery);

            answerSet.setEntityAnswers(entityScores);
            answerSet.setCoreEntity(coreEntityScore);
            answerSet.setDocTypes(docTypes);

            // create mappings for core entity
            Map<DataModelBinding, List<Score>> mappings = new HashMap<>();
            mappings.put(new DataModelBinding(DataModelType.INSTANCE, query.getCoreEntity(), "INSTANCE_1"), coreEntityScores);
            answerSet.setMappings(mappings);

            return answerSet;
        }

        return new NoResponse(NLI, userQuery);
    }

    public QueryResponse definitionQuery(String userQuery, Language language) {
        final List<String> definitionDocTypes = Arrays.asList("definition", "description"); //TODO remove magic strings

        EntityQueryBuilder queryBuilder = new EntityQueryBuilder();
        EntityQuery query = queryBuilder.parse(userQuery, DEFINITION);
        Scores coreEntityScores = resolveScoredInstance(query.getCoreEntity());
        Score coreEntityScore = coreEntityScores.get(0);

        List<Document> documents = entitySearcher.getDocumentsForResourceEntity(dbId, ((InstanceEntity)coreEntityScore.getEntry()).getId(), definitionDocTypes);
        if (!documents.isEmpty()) {
            AnswerSetResponse answerSet = new AnswerSetResponse(DEFINITION, userQuery);

            answerSet.setDocumentAnswers(documents.stream().map(d -> new Score(d, 1)).collect(Collectors.toList()));
            answerSet.setTextAnswers(documents.stream().map(d -> d.getText()).collect(Collectors.toList()));

            answerSet.setCoreEntity(coreEntityScore);
            answerSet.setDocTypes(definitionDocTypes);

            // create mappings for core entity
            Map<DataModelBinding, List<Score>> mappings = new HashMap<>();
            mappings.put(new DataModelBinding(DataModelType.INSTANCE, query.getCoreEntity(), "INSTANCE_1"), coreEntityScores);
            answerSet.setMappings(mappings);

            return answerSet;
        }

        return new NoResponse(DEFINITION, userQuery);
    }

    public QueryResponse likeThisQuery(String userQuery, Language language) {
        List<String> docTypes = core.getDocTypes();

        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).term(userQuery);
        Scores entityScores = entitySearcher.likeThisInstanceSearch(searchParams, docTypes);
        if (!entityScores.isEmpty()) {
            AnswerSetResponse answerSet = new AnswerSetResponse(LIKE_THIS, userQuery);

            answerSet.setEntityAnswers(entityScores);
            answerSet.setDocTypes(docTypes);

            return answerSet;
        }

        return new NoResponse(LIKE_THIS, userQuery);
    }

    private double determineScore(FilterResult filterResult) {
        double maxScore = 0;
        double score = 0;

        for (FilterResult.SingleFilterResult sfr : filterResult.getSingleFilterResults()) {

            // relation
            maxScore += 1;
            score += (sfr.isMatchedRelation()) ? 1 * sfr.getMatchedRelation().getValue() : 0;

            // terms
            maxScore += sfr.getFilter().getTerms().size();
            for (Score s : sfr.getMatchedTerms()) {
                if (s != null) {
                    score += s.getValue();
                }
            }

            // temporals
            maxScore += sfr.getFilter().getTemporals().size();
            for (Score s : sfr.getMatchedTemporals()) {
                if (s != null) {
                    score += s.getValue();
                }
            }
        }

        return (maxScore == 0)? 0 : (score / maxScore);
    }

    private Score rerankDocuments(Score documentScore, List<PassageExtraction> queryFilters, ModifiableRankParams relationRankParams, ModifiableRankParams termRankParams, List<FilterResult> filterResults) {
        Document document = (Document) documentScore.getEntry();

        logger.debug(marker, "Re-Rank document {} with respect to filters", document.getId());

        FilterResult filterResult = new FilterResult(queryFilters, document.getId(), document.getEntity());
        for (FilterResult.SingleFilterResult sfr : filterResult.getSingleFilterResults()) {
            PassageExtraction f = sfr.getFilter();

            logger.debug(marker, "Check query passage-extraction: {}", f);

            // rank document's extractions to the target extraction
            Scores passageExScores = new Scores(document.getPassageExtractions().stream().map(pe -> new Score(pe, 0)).collect(Collectors.toList()));
            Scores ranked = Rankers.apply(passageExScores, relationRankParams, f);

            if (ranked.size() > 0) {
                // matched relation
                PassageExtraction matchedEx = (PassageExtraction) ranked.get(0).getEntry();
                sfr.setMatchedRelation(matchedEx.getRelation(), ranked.get(0).getValue());

                for (int idx = 0; idx < f.getTerms().size(); idx++) {
                    String termFilter = f.getTerms().get(idx);
                    for (String term : matchedEx.getTerms()) {
                        Double sim = Rankers.matchSimilarity(termFilter.trim().toLowerCase(), term.trim().toLowerCase(), termRankParams);
                        if (sim != null) {
                            // matched (relation &) term
                            sfr.addMatchedTerm(idx, term, sim);
                            break;
                        }
                    }
                }

                for (int idx = 0; idx < f.getTemporals().size(); idx++) {
                    TimeRange temporalFilter = f.getTemporals().get(idx);
                    for (TimeRange temporal : matchedEx.getTemporals()) {
                        if (temporalFilter.containsInterval(temporal)) {
                            // matched (relation &) temporal
                            sfr.addMatchedTemporal(idx, temporal, 1);
                            break;
                        }
                    }
                }
            }

            logger.debug(marker, sfr.toString());
        }

        filterResults.add(filterResult);

        // calculate final score (take into account the old document score, a small factor is sufficient to have at least some ranking even if no filters match)
        final double oldScoreFactor = 0.1;
        double finalScore = (oldScoreFactor * documentScore.getValue()) + ((1. - oldScoreFactor) * determineScore(filterResult));

        return new Score(document, finalScore);
    }

    public QueryResponse filterQuery(String userQuery, Language language) {
        final int LIMIT = 50;
        List<String> docTypes = core.getDocTypes();

        FilterQueryBuilder queryBuilder = new FilterQueryBuilder(stargraph, dbId);
        FilterQuery filterQuery = queryBuilder.parse(userQuery, FILTER);

        List<PassageExtraction> queryFilters = filterQuery.getExtractionFilters();
        queryFilters.forEach(ef -> {
            logger.info(marker, "Extraction-Filter: {}", ef);
        });

        List<String> searchTerms = new ArrayList<>();
        for (PassageExtraction extractionFilter : queryFilters) {
            searchTerms.add(extractionFilter.getRelation());
            searchTerms.addAll(extractionFilter.getTerms());
            for (TimeRange timeRange : extractionFilter.getTemporals()) {
                searchTerms.add(String.valueOf(timeRange.getFrom().getYear()));
                if (timeRange.getFrom().getYear() != timeRange.getTo().getYear()) {
                    searchTerms.add(String.valueOf(timeRange.getTo().getYear()));
                }
            }
        }
        logger.debug(marker, "Search-terms: {}", searchTerms);

        Scores documentScores;
        if (searchTerms.size() > 0) {
            ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).phrases(searchTerms);
            documentScores = new Scores(entitySearcher.documentSearch(searchParams, docTypes, true, false));
        } else {
            ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).term(userQuery);
            documentScores = new Scores(entitySearcher.similarDocumentSearch(searchParams, docTypes, true));
        }

        // re-rank
        ModifiableRankParams relationRankParams = ParamsBuilder.word2vec().threshold(Threshold.min(0.2));
        if (relationRankParams instanceof ModifiableIndraParams) {
            core.configureDistributionalParams((ModifiableIndraParams) relationRankParams);
        }
        ModifiableRankParams termRankParams = ParamsBuilder.levenshtein().threshold(Threshold.max(3));
        if (termRankParams instanceof ModifiableIndraParams) {
            core.configureDistributionalParams((ModifiableIndraParams) termRankParams);
        }
        List<FilterResult> filterResults = new ArrayList<>();
        documentScores = new Scores(documentScores.stream().map(s -> rerankDocuments(s, queryFilters, relationRankParams, termRankParams, filterResults)).collect(Collectors.toList()));
        documentScores.sort(true);

        // limit
        documentScores = new Scores(documentScores.stream().limit(LIMIT).collect(Collectors.toList()));

        // now map documents back to their entities
        Scores entityScores = new Scores();
        for (Score score : documentScores) {
            Document doc = (Document)score.getEntry();

            InstanceEntity ent = entitySearcher.getInstanceEntity(dbId, doc.getEntity());
            if (ent != null) {
                entityScores.add(new Score(ent, score.getValue()));
            }
        }

        if (!entityScores.isEmpty()) {
            AnswerSetResponse answerSet = new AnswerSetResponse(FILTER, userQuery);

            answerSet.setEntityAnswers(entityScores);
            answerSet.setDocTypes(docTypes);
            answerSet.setQueryFilters(queryFilters);
            answerSet.setFilterResults(filterResults);

            return answerSet;
        }

        return new NoResponse(FILTER, userQuery);
    }

    public QueryResponse clueQuery(String userQuery, Language language) {

//      These filters will be used very soon
//      ClueAnalyzer clueAnalyzer = new ClueAnalyzer();
//      String pronominalAnswerType = clueAnalyzer.getPronominalAnswerType(userQuery);
//      String lexicalAnswerType = clueAnalyzer.getLexicalAnswerType(userQuery);
//      String abstractLexicalAnswerType = clueAnalyzer.getAbstractType(lexicalAnswerType);

//      Get documents containing the keywords
//      Map<Document, Double> documents = core.getDocumentSearcher().searchDocuments(userQuery, 3);

        Scores entityScores = new Scores();
        if(!entityScores.isEmpty()) {
            AnswerSetResponse answerSet = new AnswerSetResponse(CLUE, userQuery);
            answerSet.setEntityAnswers(entityScores);
            return answerSet;
        }

        return new NoResponse(CLUE, userQuery);
    }

    private void resolve(Triple triple, SPARQLQueryBuilder builder) {
        logger.debug(marker, "Resolve triple {}", triple);

        if (triple.p.getModelType() != DataModelType.TYPE) {
            // if predicate is not a type assume: I (C|P) V pattern
            logger.debug(marker, "Assume 'I (C|P) V' pattern");

            boolean subjPivot = true;
            InstanceEntity pivot = resolvePivot(triple.s, builder);
            if (pivot == null) {
                subjPivot = false;
                pivot = resolvePivot(triple.o, builder);
            }

            resolvePredicate(pivot, !subjPivot, subjPivot, triple.p, builder);
        }
        else {
            // Probably is: V T C
            logger.debug(marker, "Assume 'V T C' pattern");

            DataModelBinding binding = triple.s.getModelType() == DataModelType.VARIABLE ? triple.o : triple.s;
            resolveClass(binding, builder);
        }
    }

    private boolean isURI(String str) {
        return (str.toLowerCase().startsWith("http://") || str.toLowerCase().startsWith("https://"));
    }

    private InstanceEntity resolveUri(String str) {
        InstanceEntity instance = null;
        if (isURI(str.trim())) {
            instance = entitySearcher.getInstanceEntity(dbId, str.trim());
        }
        return instance;
    }

    public Scores rescoreClasses(Scores scores) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);

        List<String> noRootIds = new ArrayList<>();
        for (Score score : scores) {
            String id = score.getRankableView().getId();
            List<String> otherIds = scores.stream().map(s -> s.getRankableView().getId()).filter(i -> !i.equals(id)).collect(Collectors.toList());
            if (entitySearcher.isClassMember(id, otherIds, searchParams)) {
                noRootIds.add(id);
            }
        }

        // boost roots
        Scores rescored = new Scores();
        for (Score score : scores) {
            double s = (!noRootIds.contains(score.getRankableView().getId()))? 1 : 0;
            double newScore = (0.5 * score.getValue()) + (0.5 * s);
            rescored.add(new Score(score.getEntry(), newScore));
        }

        // order
        rescored.sort(true);

        return rescored;
    }

    private void resolveClass(DataModelBinding binding, SPARQLQueryBuilder builder) {
        if (binding.getModelType() == DataModelType.CLASS) {
            final int PRE_LIMIT = 50;
            final int LIMIT = 3;
            Scores scores;

            // check for concrete URI
            InstanceEntity resolvedUri = resolveUri(binding.getTerm());
            if (resolvedUri != null) {
                scores = new Scores();
                scores.add(new Score(resolvedUri, 1));
            } else {
                logger.debug(marker, "Resolve class, searching for term '{}'", binding.getTerm());
                scores = searchClass(binding);

                // re-rank classes
                scores = rescoreClasses(new Scores(scores.stream().limit(PRE_LIMIT).collect(Collectors.toList()))); // Limit, because otherwise, the SPARQL-Query gets too long -> StackOverflow
                logger.debug(marker, "Results: {}", scores);
            }

            logger.debug(marker, "Map {} to binding {}", scores.stream().limit(LIMIT).collect(Collectors.toList()), binding);
            builder.addMapping(binding, scores.stream().limit(LIMIT).collect(Collectors.toList()));
        }
    }

    protected Scores searchClass(DataModelBinding binding) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).phrase(binding.getTerm());
        ModifiableRankParams rankParams = ParamsBuilder.word2vec();
        return entitySearcher.classSearch(searchParams, rankParams);
    }

    private void resolvePredicate(InstanceEntity pivot, boolean incomingEdges, boolean outgoingEdges, DataModelBinding binding, SPARQLQueryBuilder builder) {
        if ((binding.getModelType() == DataModelType.CLASS
                || binding.getModelType() == DataModelType.PROPERTY) && !builder.isResolved(binding)) {
            final int LIMIT = 6;
            Scores scores;

            // check for concrete URI
            InstanceEntity resolvedUri = resolveUri(binding.getTerm());
            if (resolvedUri != null) {
                scores = new Scores();
                scores.add(new Score(resolvedUri, 1));
            } else {
                logger.debug(marker, "Resolve predicate for pivot {}, searching for term '{}'", pivot, binding.getTerm());
                scores = searchPredicate(pivot, incomingEdges, outgoingEdges, binding);
                logger.debug(marker, "Results: {}", scores);
            }

            logger.debug(marker, "Map {} to binding {}", scores.stream().limit(LIMIT).collect(Collectors.toList()), binding);
            builder.addMapping(binding, scores.stream().limit(LIMIT).collect(Collectors.toList()));
        }
    }

    protected Scores searchPredicate(InstanceEntity pivot, boolean incomingEdges, boolean outgoingEdges, DataModelBinding binding) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).phrase(binding.getTerm());
        ModifiableRankParams rankParams = ParamsBuilder.word2vec();
        return entitySearcher.pivotedSearch(pivot, searchParams, rankParams, incomingEdges, outgoingEdges, 1, false);
    }

    private InstanceEntity resolvePivot(DataModelBinding binding, SPARQLQueryBuilder builder) {
        List<Score> mappings = builder.getMappings(binding);
        if (!mappings.isEmpty()) {
            logger.debug(marker, "Pivot was already resolved");
            logger.debug(marker, "Return " + mappings.get(0));
            return (InstanceEntity)mappings.get(0).getEntry();
        }

        if (binding.getModelType() == DataModelType.INSTANCE) {
            Scores scores;

            // check for concrete URI
            InstanceEntity resolvedUri = resolveUri(binding.getTerm());
            if (resolvedUri != null) {
                scores = new Scores();
                scores.add(new Score(resolvedUri, 1));
            } else {
                logger.debug(marker, "Resolve pivot, searching for term '{}'", binding.getTerm());
                scores = searchPivot(binding);
                logger.debug(marker, "Results: {}", scores);
            }

            logger.debug(marker, "Map {} to binding {}", scores.get(0), binding);
            InstanceEntity instance = (InstanceEntity) scores.get(0).getEntry();
            builder.addMapping(binding, Collections.singletonList(scores.get(0)));
            return instance;
        }
        return null;
    }

    protected Scores searchPivot(DataModelBinding binding) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).phrase(binding.getTerm());
        ModifiableRankParams rankParams = ParamsBuilder.levenshtein(); // threshold defaults to auto
        return entitySearcher.instanceSearch(searchParams, rankParams);
    }

    private Scores resolveScoredInstance(String instanceTerm) {
        final int LIMIT = 6;
        Scores scores;

        // check for concrete URI
        InstanceEntity resolvedUri = resolveUri(instanceTerm);
        if (resolvedUri != null) {
            scores = new Scores();
            scores.add(new Score(resolvedUri, 1));
        } else {
            logger.debug(marker, "Resolve instance/resource, searching for term '{}'", instanceTerm);
            scores = searchInstance(instanceTerm);
            logger.debug(marker, "Results: {}", scores);
        }

        logger.debug(marker, "Return: {}", scores.stream().limit(LIMIT).collect(Collectors.toList()));
        return new Scores(scores.stream().limit(LIMIT).collect(Collectors.toList()));
    }

    protected Scores searchInstance(String instanceTerm) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).phrase(instanceTerm);
        ModifiableRankParams rankParams = ParamsBuilder.levenshtein(); // threshold defaults to auto
        return entitySearcher.instanceSearch(searchParams, rankParams);
    }

    private Triple asTriple(TriplePattern pattern, List<DataModelBinding> bindings) {
        String[] components = pattern.getPattern().split("\\s");
        return new Triple(map(components[0], bindings), map(components[1], bindings), map(components[2], bindings));
    }

    private DataModelBinding map(String placeHolder, List<DataModelBinding> bindings) {
        if (placeHolder.startsWith("?VAR") || placeHolder.startsWith("TYPE")) {
            DataModelType type = placeHolder.startsWith("?VAR") ? DataModelType.VARIABLE : DataModelType.TYPE;
            return new DataModelBinding(type, placeHolder, placeHolder);
        }

        return bindings.stream()
                .filter(b -> b.getPlaceHolder().equals(placeHolder))
                .findAny().orElseThrow(() -> new StarGraphException("Unmapped placeholder '" + placeHolder + "'"));
    }

    private static class Triple {
        Triple(DataModelBinding s, DataModelBinding p, DataModelBinding o) {
            this.s = s;
            this.p = p;
            this.o = o;
        }

        public DataModelBinding s;
        public DataModelBinding p;
        public DataModelBinding o;

        @Override
        public String toString() {
            return String.format("<%s %s %s>     (%s='%s', %s='%s', %s='%s')", s.getPlaceHolder(), p.getPlaceHolder(), o.getPlaceHolder(),
                    s.getPlaceHolder(), s.getTerm(),
                    p.getPlaceHolder(), p.getTerm(),
                    o.getPlaceHolder(), o.getTerm());
        }
    }
}
