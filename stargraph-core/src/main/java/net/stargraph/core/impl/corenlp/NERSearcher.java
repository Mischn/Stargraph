package net.stargraph.core.impl.corenlp;

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

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import net.stargraph.core.ner.LinkedEntityScore;
import net.stargraph.core.ner.NamedEntity;
import net.stargraph.core.ner.NER;
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.model.LabeledEntity;
import net.stargraph.query.Language;
import net.stargraph.rank.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;
import java.util.stream.Collectors;

public final class NERSearcher implements NER {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Marker marker = MarkerFactory.getMarker("ner");
    private CoreNLPNERClassifier ner;
    private EntitySearcher entitySearcher;
    private List<String> entitySearcherDbIds;
    private boolean reverseNameOrder;

    public NERSearcher(Language language, EntitySearcher entitySearcher, List<String> entitySearcherDbIds) {
        this.ner = new CoreNLPNERClassifier(Objects.requireNonNull(language));
        this.entitySearcher = Objects.requireNonNull(entitySearcher);
        this.entitySearcherDbIds = Objects.requireNonNull(entitySearcherDbIds);
        this.reverseNameOrder = false; //TODO: read from configuration, specific for each KB.
    }

    @Override
    public List<NamedEntity> searchAndLink(String text) {
        return search(text, true);
    }

    @Override
    public List<NamedEntity> searchWithoutLink(String text) {
        return search(text, false);
    }

    public List<NamedEntity> search(String text, boolean link) {
        logger.debug(marker, "Search for named entities (link={}) in text: '{}'", link, text);
        long start = System.nanoTime();
        List<NamedEntity> namedEntities = new ArrayList<>();
        try {
            String input = text + " ."; //TODO remove hack?
            final List<List<CoreLabel>> sentences = ner.classify(input); //TODO: Improve decoupling, still tied to CoreNLP
            logger.trace(marker, "NER output: {}", sentences);
            namedEntities = postProcessFoundNamedEntities(sentences);
            if (link) {
                logger.debug(marker, "link entities");
                linkNamedEntities(namedEntities);
            }
            return namedEntities;
        }
        finally {
            double elapsedInMillis = (System.nanoTime() - start) / 1000_000;
            logger.debug(marker, "Took {}ms, entities: {}, text: '{}'", elapsedInMillis, namedEntities, text);
        }
    }

    private List<NamedEntity> postProcessFoundNamedEntities(List<List<CoreLabel>> sentences) {
        final List<List<NamedEntity>> sentenceList = mergeConsecutiveNamedEntities(sentences);

        if (this.reverseNameOrder) {
            sentenceList.forEach(sentence -> {
                sentence.forEach(NamedEntity::reverseValue);
            });
        }

        if (sentenceList.isEmpty() || (sentenceList.size() == 1 && sentenceList.get(0).isEmpty())) {
            logger.trace(marker, "No Entities detected.");
            return Collections.emptyList();
        }

        // Flat map
        final List<NamedEntity> allNamedEntities = new ArrayList<>();
        for (List<NamedEntity> p : sentenceList) {
            allNamedEntities.addAll(p);
        }

        return allNamedEntities;
    }

    /**
     * Receives a list of CoreLabel (from CoreNLP package) and merges two consecutive named entities with
     * the same label into a single one.
     *
     * Example: "Barack/PERSON Obama/PERSON" becomes "Barack Obama/PERSON"
     *
     * @param sentences List of lists of CoreLabels
     * @return List of ScoredNamedEntities where consecutive named entities are combined
     */
    private static List<List<NamedEntity>> mergeConsecutiveNamedEntities(List<List<CoreLabel>> sentences) {
        final List<List<NamedEntity>> sentenceList = new ArrayList<>();

        for (List<CoreLabel> sentence : sentences) {

            List<NamedEntity> namedEntities = new ArrayList<>();
            String previousCat = null;
            NamedEntity currentNamedEntity = null;

            /*
                A named entity is composed of multiple words, most of the time.
                Two consecutive words belong to one named entity if they have the same label.
                This method does not differentiate two different named entities when they are not
                divided by a different label.
                CoreNLP labels words that are not a named entity with "O", so we remove these from the output.
             */
            for (CoreLabel coreLabel : sentence) {

                String currentWord = coreLabel.originalText();

                String currentCat = coreLabel.get(CoreAnnotations.AnswerAnnotation.class);

                if (currentNamedEntity == null) {
                    currentNamedEntity = new NamedEntity(currentWord, currentCat, coreLabel.beginPosition(), coreLabel.endPosition());
                } else if (currentCat.equals(previousCat)) {
                    currentNamedEntity.merge(currentNamedEntity.getValue() + " " + currentWord, coreLabel.endPosition());
                } else {
                    namedEntities.add(currentNamedEntity);
                    currentNamedEntity = new NamedEntity(currentWord, currentCat, coreLabel.beginPosition(), coreLabel.endPosition());
                }

                previousCat = currentCat;
            }

            /* Add last NE when not already added.
               This happens, when the last token in a sentence belongs to a named entity.
             */
            if (!namedEntities.contains(currentNamedEntity)) {
                namedEntities.add(currentNamedEntity);
            }

            // ignore NamedEntities with label "O", they are not NamedEntities
            sentenceList.add(namedEntities
                    .stream()
                    .filter(s -> !s.getCat().equals("O"))
                    .collect(Collectors.toList()));
        }

        return sentenceList.stream().filter(s -> s.size() > 0).collect(Collectors.toList());
    }

    private void linkNamedEntities(List<NamedEntity> namedEntities) {
        List<NamedEntity> linkedNamedEntities = new ArrayList<>(); // these are really linked

        for (NamedEntity namedEntity : namedEntities) {
            /*
            Find reference in previous named entities.
            -> When found: Use that ID, etc.
            -> Not found: Search in database.
             */
            Optional<NamedEntity> reference = findReference(linkedNamedEntities, namedEntity.getValue());
            if (reference.isPresent()) {
                for (LinkedEntityScore score : reference.get().getEntities()) {
                    namedEntity.addLink((LabeledEntity)score.getEntry(), score.getDbId(), score.getValue());
                }
            } else {
                tryLink(namedEntity);
            }

            if (namedEntity.isLinked()) {
                linkedNamedEntities.add(namedEntity);
            }
        }

        logger.trace(marker, "Linked {} out of {} named-entities.", linkedNamedEntities.size(), namedEntities.size());
    }

    private void tryLink(NamedEntity namedEntity) {
        final int LIMIT = 3;

        if (!namedEntity.getCat().equalsIgnoreCase("DATE")) {
            //TODO: Limit reduce network latency but can hurt precision in some cases

            for (String entitySearcherDbId : entitySearcherDbIds) {
                logger.debug(marker, "Trying to link '{}' to knowledge-base '{}'", namedEntity, entitySearcherDbId);

                ModifiableSearchParams searchParams = ModifiableSearchParams.create(entitySearcherDbId).searchSpaceLimit(LIMIT); //TODO use resultLimit to increase performance at cost of runtime?
                ModifiableSearchString searchString = ModifiableSearchString.create().searchPhrase(new ModifiableSearchString.Phrase(namedEntity.getValue()));

                final Scores scores = entitySearcher.instanceSearch(searchParams, searchString, ParamsBuilder.levenshtein());
                for (Score score : scores) {
                    logger.debug(marker, "Found {}'", score.getRankableView().getId());
                    namedEntity.addLink((LabeledEntity)score.getEntry(), entitySearcherDbId, score.getValue());
                }
            }
        }
    }

    private static Optional<NamedEntity> findReference(List<NamedEntity> namedEntities, String namedEntity) {
        NamedEntity found = null;
        for (NamedEntity sne : namedEntities) {
            if (sne.getValue().contains(namedEntity)) {
                // use the first occurrence of a substring
                found = sne;
                break;
            }
        }
        return Optional.ofNullable(found);
    }

}
