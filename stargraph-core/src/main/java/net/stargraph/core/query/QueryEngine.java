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
import net.stargraph.model.LabeledEntity;
import net.stargraph.model.ResourceEntity;
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

    protected String dbId;
    protected KBCore core;
    protected Analyzers analyzers;
    protected GraphSearcher graphSearcher;
    protected EntitySearcher entitySearcher;
    protected InteractionModeSelector modeSelector;
    protected Namespace namespace;
    protected Language language;

    public QueryEngine(String dbId, Stargraph stargraph) {
        this.dbId = Objects.requireNonNull(dbId);
        this.core = Objects.requireNonNull(stargraph.getKBCore(dbId));
        this.analyzers = new Analyzers(stargraph);
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
                case NLI:
                    response = nliQuery(query, language);
                    break;
                case SPARQL:
                    response = sparqlQuery(query);
                    break;
                case ENTITY_SIMILARITY:
                    response = entitySimilarityQuery(query, language);
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

            Set<LabeledEntity> expanded = vars.get("VAR_1").stream()
                    .map(e -> namespace.expand(e)).collect(Collectors.toSet());

            answerSet.setEntityAnswer(new ArrayList<>(expanded)); // convention, answer must be bound to the first var
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
        Score instanceScore = resolveScoredInstance(query.getCoreEntity());
        ResourceEntity instance = (ResourceEntity) instanceScore.getEntry();

        Set<ResourceEntity> entities = new HashSet<>();
        Scores scores = entitySearcher.similarResourceSearch(dbId, instance, docTypes);
        scores.forEach(s -> {
            entities.add((ResourceEntity)s.getEntry());
        });

        if (!entities.isEmpty()) {
            AnswerSetResponse answerSet = new AnswerSetResponse(ENTITY_SIMILARITY, userQuery);

            // create mapping for core entity
            Map<DataModelBinding, List<Score>> mappings = new HashMap<>();
            mappings.put(new DataModelBinding(DataModelType.INSTANCE, query.getCoreEntity(), "INSTANCE_1"), Arrays.asList(instanceScore));
            answerSet.setMappings(mappings);

            List<Document> docs = entitySearcher.getDocumentsForResourceEntity(dbId, instance.getId(), docTypes);
            answerSet.setDocuments(docs);

            answerSet.setEntityAnswer(new ArrayList<>(entities));
            return answerSet;
        }

        return new NoResponse(NLI, userQuery);
    }

    public QueryResponse definitionQuery(String userQuery, Language language) {
        final List<String> definitionDocTypes = Arrays.asList("definition", "description"); //TODO remove magic strings

        EntityQueryBuilder queryBuilder = new EntityQueryBuilder();
        EntityQuery query = queryBuilder.parse(userQuery, DEFINITION);
        Score instanceScore = resolveScoredInstance(query.getCoreEntity());
        ResourceEntity instance = (ResourceEntity) instanceScore.getEntry();

        List<Document> documents = entitySearcher.getDocumentsForResourceEntity(dbId, instance.getId(), definitionDocTypes);
        if (!documents.isEmpty()) {
            AnswerSetResponse answerSet = new AnswerSetResponse(DEFINITION, userQuery);

            // create mapping for core entity
            Map<DataModelBinding, List<Score>> mappings = new HashMap<>();
            mappings.put(new DataModelBinding(DataModelType.INSTANCE, query.getCoreEntity(), "INSTANCE_1"), Arrays.asList(instanceScore));
            answerSet.setMappings(mappings);

            List<String> textAnswer = documents.stream().map(d -> d.getText()).collect(Collectors.toList());
            answerSet.setTextAnswer(textAnswer);

            answerSet.setDocuments(documents);
            return answerSet;
        }

        return new NoResponse(DEFINITION, userQuery);
    }

    public QueryResponse clueQuery(String userQuery, Language language) {

//      These filters will be used very soon
//      ClueAnalyzer clueAnalyzer = new ClueAnalyzer();
//      String pronominalAnswerType = clueAnalyzer.getPronominalAnswerType(userQuery);
//      String lexicalAnswerType = clueAnalyzer.getLexicalAnswerType(userQuery);
//      String abstractLexicalAnswerType = clueAnalyzer.getAbstractType(lexicalAnswerType);

//      Get documents containing the keywords
//      Map<Document, Double> documents = core.getDocumentSearcher().searchDocuments(userQuery, 3);

        Set<LabeledEntity> entities = new HashSet<>();
        if(!entities.isEmpty()) {
            AnswerSetResponse answerSet = new AnswerSetResponse(DEFINITION, userQuery);
            answerSet.setEntityAnswer(new ArrayList<>(entities));
            return answerSet;
        }

        return new NoResponse(NLI, userQuery);
    }

    private void resolve(Triple triple, SPARQLQueryBuilder builder) {
        logger.debug(marker, "Resolve triple {}", triple);

        if (triple.p.getModelType() != DataModelType.TYPE) {
            // if predicate is not a type assume: I (C|P) V pattern
            logger.debug(marker, "Assume 'I (C|P) V' pattern");

            boolean subjPivot = true;
            ResourceEntity pivot = resolvePivot(triple.s, builder);
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

    private void resolveClass(DataModelBinding binding, SPARQLQueryBuilder builder) {
        if (binding.getModelType() == DataModelType.CLASS) {
            final int LIMIT = 3;

            logger.debug(marker, "Resolve class, searching for term '{}'", binding.getTerm());
            Scores scores = searchClass(binding);
            logger.debug(marker, "Results: {}", scores);

            logger.debug(marker, "Map {} to binding {}", scores.stream().limit(LIMIT).collect(Collectors.toList()), binding);
            builder.addMapping(binding, scores.stream().limit(LIMIT).collect(Collectors.toList()));
        }
    }

    protected Scores searchClass(DataModelBinding binding) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).term(binding.getTerm());
        ModifiableRankParams rankParams = ParamsBuilder.word2vec();
        return entitySearcher.classSearch(searchParams, rankParams);
    }

    private void resolvePredicate(ResourceEntity pivot, boolean incomingEdges, boolean outgoingEdges, DataModelBinding binding, SPARQLQueryBuilder builder) {
        if ((binding.getModelType() == DataModelType.CLASS
                || binding.getModelType() == DataModelType.PROPERTY) && !builder.isResolved(binding)) {
            final int LIMIT = 6;

            logger.debug(marker, "Resolve predicate for pivot {}, searching for term '{}'", pivot, binding.getTerm());
            Scores scores = searchPredicate(pivot, incomingEdges, outgoingEdges, binding);
            logger.debug(marker, "Results: {}", scores);

            logger.debug(marker, "Map {} to binding {}", scores.stream().limit(LIMIT).collect(Collectors.toList()), binding);
            builder.addMapping(binding, scores.stream().limit(LIMIT).collect(Collectors.toList()));
        }
    }

    protected Scores searchPredicate(ResourceEntity pivot, boolean incomingEdges, boolean outgoingEdges, DataModelBinding binding) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).term(binding.getTerm());
        ModifiableRankParams rankParams = ParamsBuilder.word2vec();
        return entitySearcher.pivotedSearch(pivot, searchParams, rankParams, incomingEdges, outgoingEdges, 1, false);
    }

    private ResourceEntity resolvePivot(DataModelBinding binding, SPARQLQueryBuilder builder) {
        List<Score> mappings = builder.getMappings(binding);
        if (!mappings.isEmpty()) {
            logger.debug(marker, "Pivot was already resolved");
            logger.debug(marker, "Return " + mappings.get(0));
            return (ResourceEntity)mappings.get(0).getEntry();
        }

        if (binding.getModelType() == DataModelType.INSTANCE) {

            logger.debug(marker, "Resolve pivot, searching for term '{}'", binding.getTerm());
            Scores scores = searchPivot(binding);
            logger.debug(marker, "Results: {}", scores);

            logger.debug(marker, "Map {} to binding {}", scores.get(0), binding);
            ResourceEntity instance = (ResourceEntity) scores.get(0).getEntry();
            builder.addMapping(binding, Collections.singletonList(scores.get(0)));
            return instance;
        }
        return null;
    }

    protected Scores searchPivot(DataModelBinding binding) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).term(binding.getTerm());
        ModifiableRankParams rankParams = ParamsBuilder.levenshtein(); // threshold defaults to auto
        return entitySearcher.resourceSearch(searchParams, rankParams);
    }

    private ResourceEntity resolveInstance(String instanceTerm) {
        return (ResourceEntity) resolveScoredInstance(instanceTerm).getEntry();
    }
    private Score resolveScoredInstance(String instanceTerm) {

        logger.debug(marker, "Resolve instance/resource, searching for term '{}'", instanceTerm);
        Scores scores = searchInstance(instanceTerm);
        logger.debug(marker, "Results: {}", scores);

        logger.debug(marker, "Return: {}", scores.get(0));
        return scores.get(0);
    }

    protected Scores searchInstance(String instanceTerm) {
        ModifiableSearchParams searchParams = ModifiableSearchParams.create(dbId).term(instanceTerm);
        ModifiableRankParams rankParams = ParamsBuilder.levenshtein(); // threshold defaults to auto
        return entitySearcher.resourceSearch(searchParams, rankParams);
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
