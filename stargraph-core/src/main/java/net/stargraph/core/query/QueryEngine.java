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
import net.stargraph.core.query.entity.EntityQuery;
import net.stargraph.core.query.entity.EntityQueryBuilder;
import net.stargraph.core.query.filter.FilterQuery;
import net.stargraph.core.query.filter.FilterQueryBuilder;
import net.stargraph.core.query.filter.FilterQueryEngine;
import net.stargraph.core.query.filter.FilterResult;
import net.stargraph.core.query.nli.*;
import net.stargraph.core.query.response.AnswerSetResponse;
import net.stargraph.core.query.response.NoResponse;
import net.stargraph.core.query.response.SPARQLSelectResponse;
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.Document;
import net.stargraph.model.InstanceEntity;
import net.stargraph.model.NodeEntity;
import net.stargraph.model.PassageExtraction;
import net.stargraph.query.InteractionMode;
import net.stargraph.query.Language;
import net.stargraph.rank.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    protected Namespace namespace;
    protected Language language;
    protected InteractionModeSelector modeSelector;
    protected Resolver resolver; // currently, all interactions share this one resolver. Could be customized for each interaction mode
    protected Map<String, Map<DataModelBindingContext, List<String>>> customMappings;

    public QueryEngine(String dbId, Stargraph stargraph) {
        this.stargraph = stargraph;
        this.dbId = Objects.requireNonNull(dbId);
        this.core = Objects.requireNonNull(stargraph.getKBCore(dbId));
        this.analyzers = new Analyzers(stargraph, dbId);
        this.graphSearcher = core.getGraphSearcher();
        this.entitySearcher = stargraph.getEntitySearcher();
        this.namespace = core.getNamespace();
        this.language = core.getLanguage();
        this.modeSelector = new InteractionModeSelector(stargraph.createPOSAnnotatorFactory().create(), language);
        this.resolver = new Resolver(entitySearcher, namespace, dbId);
        this.customMappings = new ConcurrentHashMap<>();
    }

    public void setCustomMappings(Map<String, Map<DataModelBindingContext, List<String>>> customMappings) {
        this.customMappings = customMappings;
    }

    public void clearCustomMappings() {
        this.customMappings.clear();
    }

    public QueryResponse query(String query) {
        final InteractionMode mode = modeSelector.detect(query);
        QueryResponse response = new NoResponse(mode, query);

        long startTime = System.currentTimeMillis();
        try {
            switch (mode) {
                case SA_SPARQL: //TODO not implemented
                    logger.debug(marker, "Interaction-Mode: SA_SPARQL");
                    response = new NoResponse(SA_SPARQL, query);
                case SA_SIMPLE_SPARQL: //TODO not implemented
                    logger.debug(marker, "Interaction-Mode: SA_SIMPLE_SPARQL");
                    response = new NoResponse(SA_SIMPLE_SPARQL, query);
                case NLI:
                    logger.debug(marker, "Interaction-Mode: NLI");
                    response = nliQuery(query, language);
                    break;
                case SPARQL:
                    logger.debug(marker, "Interaction-Mode: SPARQL");
                    response = sparqlQuery(query);
                    break;
                case ENTITY_SIMILARITY:
                    logger.debug(marker, "Interaction-Mode: ENTITY_SIMILARITY");
                    response = entitySimilarityQuery(query, language);
                    break;
                case LIKE_THIS:
                    logger.debug(marker, "Interaction-Mode: LIKE_THIS");
                    response = likeThisQuery(query, language);
                    break;
                case FILTER:
                    logger.debug(marker, "Interaction-Mode: FILTER");
                    response = filterQuery(query, language);
                    break;
                case DEFINITION:
                    logger.debug(marker, "Interaction-Mode: DEFINITION");
                    response = definitionQuery(query, language);
                    break;
                case CLUE:
                    logger.debug(marker, "Interaction-Mode: CLUE");
                    response = clueQuery(query, language);
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
        Map<String, List<NodeEntity>> vars = graphSearcher.select(userQuery);
        if (!vars.isEmpty()) {
            return new SPARQLSelectResponse(SPARQL, userQuery, vars);
        }
        return new NoResponse(SPARQL, userQuery);
    }

    private QueryResponse nliQuery(String userQuery, Language language) {
        QuestionAnalyzer analyzer = this.analyzers.getQuestionAnalyzer(language);
        QuestionAnalysis analysis = analyzer.analyse(userQuery);
        QueryPlannerPattern queryPlannerPattern = analysis.getQueryPlannerPattern();

        AnswerSetResponse answerSet = new AnswerSetResponse(NLI, userQuery, analysis.getQueryType());
        answerSet.setBindings(analysis.getBindings());
        answerSet.setQueryPlans(queryPlannerPattern.getQueryPlans());

        Map<String, Map<DataModelBindingContext, Set<Score>>> mappings = new HashMap<>();
        Map<String, Map<DataModelBindingContext, Set<Score>>> possibleMappings = new HashMap<>();
        Set<Score> entityAnswers = new LinkedHashSet<>();
        List<String> sparqlQueries = new ArrayList<>();


        for (QueryPlan queryPlan : queryPlannerPattern.getQueryPlans()) {
            logger.info(marker, "##### Resolve query plan: #####\n{}\n", queryPlan);
            logger.info(marker, "Resolve triples:");

            resolver.reset();
            resolver.setCustomMappings(customMappings);

            queryPlan.forEach(triplePattern -> {
                resolver.resolveTriple(triplePattern.toBoundTriple(analysis.getBindings()));
            });

            SPARQLQueryBuilder queryBuilder = new SPARQLQueryBuilder(stargraph, dbId, analysis.getQueryType(),
                    queryPlan,
                    analysis.getBindings(),
                    resolver.getMappings());

            logger.info(marker, "SPARQLQueryBuilder:");
            logger.info(marker, queryBuilder.toString());

            String sparqlQueryStr = queryBuilder.build();
            sparqlQueries.add(sparqlQueryStr);

            logger.info(marker, "SPARQLQueryString:");
            logger.info(marker, sparqlQueryStr);

            Map<String, List<NodeEntity>> vars = graphSearcher.select(sparqlQueryStr);
            if (vars.containsKey("VAR_1")) {
                Set<Score> eAnswers = vars.get("VAR_1").stream() // convention, answer must be bound to the first var
                        .map(e -> new Score(namespace.expand(e), 1)).collect(Collectors.toSet());

                entityAnswers.addAll(eAnswers);
            }

            // add possible mappings
            for (String placeholder : resolver.getPossibleMappings().keySet()) {
                for (DataModelBindingContext context : resolver.getPossibleMappings().get(placeholder).keySet()) {
                    Set<Score> scores = resolver.getPossibleMappings().get(placeholder).get(context);
                    possibleMappings.computeIfAbsent(placeholder, p -> new HashMap<>()).computeIfAbsent(context, c -> new LinkedHashSet<>()).addAll(scores);
                }
            }

            // add mappings
            for (String placeholder : resolver.getMappings().keySet()) {
                for (DataModelBindingContext context : resolver.getMappings().get(placeholder).keySet()) {
                    Set<Score> scores = resolver.getMappings().get(placeholder).get(context);
                    mappings.computeIfAbsent(placeholder, p -> new HashMap<>()).computeIfAbsent(context, c -> new LinkedHashSet<>()).addAll(scores);
                }
            }

            // TODO ?
            //if (triplePattern.getTypes().contains("VARIABLE TYPE CLASS")) {
            //    entities = core.getEntitySearcher().searchByTypes(new HashSet<String>(Arrays.asList(triplePattern.objectLabel.split(" "))), true, 100);
            //}
        }

        answerSet.setMappings(mappings);
        answerSet.setPossibleMappings(possibleMappings);
        answerSet.setEntityAnswers(entityAnswers.stream().collect(Collectors.toList()));
        answerSet.setSparqlQueries(sparqlQueries);

        return answerSet;
    }

    private QueryResponse entitySimilarityQuery(String userQuery, Language language) {
        List<String> docTypes = core.getDocTypes();

        EntityQueryBuilder queryBuilder = new EntityQueryBuilder();
        EntityQuery query = queryBuilder.parse(userQuery, ENTITY_SIMILARITY);

        // create mappings for core entity
        DataModelBinding coreEntityBinding = new DataModelBinding(DataModelType.INSTANCE, "INSTANCE_1", query.getCoreEntity());
        DataModelBindingContext context = DataModelBindingContext.NON_PREDICATE;

        resolver.reset();
        resolver.setCustomMappings(customMappings);
        resolver.resolveInstance(coreEntityBinding, context, 10, 1);

        if (resolver.hasMappings(coreEntityBinding.getPlaceHolder(), context)) {
            Score coreEntityScore = resolver.getMappings(coreEntityBinding.getPlaceHolder(), context).iterator().next();

            ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
            Scores entityScores = entitySearcher.similarInstanceSearch((InstanceEntity) coreEntityScore.getEntry(), searchParams, docTypes);
            if (!entityScores.isEmpty()) {
                AnswerSetResponse answerSet = new AnswerSetResponse(ENTITY_SIMILARITY, userQuery);

                answerSet.setEntityAnswers(entityScores);
                answerSet.setCoreEntity(coreEntityScore);
                answerSet.setDocTypes(docTypes);

                answerSet.setPossibleMappings(resolver.getPossibleMappings());
                answerSet.setMappings(resolver.getMappings());

                return answerSet;
            }
        }

        return new NoResponse(NLI, userQuery);
    }

    public QueryResponse definitionQuery(String userQuery, Language language) {
        final List<String> definitionDocTypes = Arrays.asList("definition", "description"); //TODO remove magic strings

        EntityQueryBuilder queryBuilder = new EntityQueryBuilder();
        EntityQuery query = queryBuilder.parse(userQuery, DEFINITION);

        // create mappings for core entity
        DataModelBinding coreEntityBinding = new DataModelBinding(DataModelType.INSTANCE, "INSTANCE_1", query.getCoreEntity());
        DataModelBindingContext context = DataModelBindingContext.NON_PREDICATE;

        resolver.reset();
        resolver.setCustomMappings(customMappings);
        resolver.resolveInstance(coreEntityBinding, context, 10, 1);

        if (resolver.hasMappings(coreEntityBinding.getPlaceHolder(), context)) {
            Score coreEntityScore = resolver.getMappings(coreEntityBinding.getPlaceHolder(), context).iterator().next();

            List<Document> documents = entitySearcher.getDocumentsForResourceEntity(dbId, ((InstanceEntity) coreEntityScore.getEntry()).getId(), definitionDocTypes);
            if (!documents.isEmpty()) {
                AnswerSetResponse answerSet = new AnswerSetResponse(DEFINITION, userQuery);

                answerSet.setDocumentAnswers(documents.stream().map(d -> new Score(d, 1)).collect(Collectors.toList()));
                answerSet.setTextAnswers(documents.stream().map(d -> d.getText()).collect(Collectors.toList()));

                answerSet.setCoreEntity(coreEntityScore);
                answerSet.setDocTypes(definitionDocTypes);

                answerSet.setPossibleMappings(resolver.getPossibleMappings());
                answerSet.setMappings(resolver.getMappings());

                return answerSet;
            }
        }

        return new NoResponse(DEFINITION, userQuery);
    }

    public QueryResponse likeThisQuery(String userQuery, Language language) {
        List<String> docTypes = core.getDocTypes();

        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId);
        ModifiableSearchString searchString = ModifiableSearchString.create().searchTermsFromStr(userQuery);
        Scores entityScores = entitySearcher.likeThisInstanceSearch(searchParams, searchString, docTypes);
        if (!entityScores.isEmpty()) {
            AnswerSetResponse answerSet = new AnswerSetResponse(LIKE_THIS, userQuery);

            answerSet.setEntityAnswers(entityScores);
            answerSet.setDocTypes(docTypes);

            return answerSet;
        }

        return new NoResponse(LIKE_THIS, userQuery);
    }



    public QueryResponse filterQuery(String userQuery, Language language) {
        final boolean RE_RANK = true;
        final int SEARCH_SPACE_LIMIT = -1;
        final int RESULT_LIMIT = 30;
        List<String> docTypes = core.getDocTypes();

        FilterQueryBuilder queryBuilder = new FilterQueryBuilder(stargraph, dbId);
        FilterQuery filterQuery = queryBuilder.parse(userQuery, FILTER);

        // get query filters
        List<PassageExtraction> queryFilters = filterQuery.getExtractionFilters();
        queryFilters.forEach(ef -> {
            logger.info(marker, "Query-Filter: {}", ef);
        });

        // extract initial search phrases
        // TODO term search takes too long!!
        List<ModifiableSearchString.Phrase> searchPhrases = new ArrayList<>();
        for (PassageExtraction queryFilter : queryFilters) {
            searchPhrases.addAll(queryFilter.getTerms().stream().map(t -> new ModifiableSearchString.Phrase(t)).collect(Collectors.toList()));
        }
        if (searchPhrases.size() <= 0) {
            for (PassageExtraction queryFilter : queryFilters) {
                searchPhrases.add(new ModifiableSearchString.Phrase(queryFilter.getRelation()));
            }
        }
        logger.debug(marker, "Search-phrases: {}", searchPhrases);

        // search for initial search phrases (limit the search space)
        Scores documentScores;
        if (searchPhrases.size() > 0) {
            logger.info(marker, "Search phrases for document-search: " + searchPhrases);
            ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).searchSpaceLimit(SEARCH_SPACE_LIMIT);
            ModifiableSearchString searchString = ModifiableSearchString.create().searchPhrases(searchPhrases);
            documentScores = new Scores(entitySearcher.documentSearch(searchParams, searchString, docTypes, true, false));
        } else
            {
            logger.info(marker, "Search term for similar-document-search: " + userQuery);
            ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).searchSpaceLimit(SEARCH_SPACE_LIMIT);
            ModifiableSearchString searchString = ModifiableSearchString.create().searchTermsFromStr(userQuery);
            documentScores = new Scores(entitySearcher.similarDocumentSearch(searchParams, searchString, docTypes, true));
        }

        // re-rank results
        List<FilterResult> filterResults = new ArrayList<>();
        if (RE_RANK) {
            FilterQueryEngine filterQueryEngine = new FilterQueryEngine();

            // re-rank
            ModifiableRankParams relationRankParams = ParamsBuilder.word2vec().threshold(Threshold.min(0.2)); //TODO magic number
            if (relationRankParams instanceof ModifiableIndraParams) {
                core.configureDistributionalParams((ModifiableIndraParams) relationRankParams);
            }
            ModifiableRankParams termRankParams = ParamsBuilder.levenshtein(); //TODO threshold?
            if (termRankParams instanceof ModifiableIndraParams) {
                core.configureDistributionalParams((ModifiableIndraParams) termRankParams);
            }
            documentScores = new Scores(documentScores.stream().map(s -> filterQueryEngine.rerankDocuments(s, queryFilters, relationRankParams, termRankParams, filterResults)).collect(Collectors.toList()));
            documentScores.sort(true);
        }

        // limit results
        documentScores = new Scores(documentScores.stream().limit(RESULT_LIMIT).collect(Collectors.toList()));

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


}
