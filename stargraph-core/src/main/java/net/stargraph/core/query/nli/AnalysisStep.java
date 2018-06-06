package net.stargraph.core.query.nli;

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

import net.stargraph.core.query.annotator.POSTag;
import net.stargraph.core.query.annotator.Word;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class AnalysisStep {
    private static final Pattern PUNCT_PATTERN = Pattern.compile(".*(([(){},.;!?<>%])).*");

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("nli");

    private List<Word> annotated;
    private List<DataModelBinding> dataModelBindings;

    private AnalysisStep(List<DataModelBinding> dataModelBindings, List<Word> annotated) {
        this.dataModelBindings = Objects.requireNonNull(dataModelBindings);
        this.annotated = Objects.requireNonNull(annotated);

        if (annotated.isEmpty()) {
            throw new IllegalArgumentException();
        }

        logger.debug(marker, "Current Step: '{}' / '{}'", getLexicalStr(), getPosTagStr());
    }

    public AnalysisStep(List<Word> annotated) {
        this(Collections.emptyList(), annotated);
    }

    private static String getLexicalStr(List<Word> words) {
        return words.stream().map(w -> w.getText()).collect(Collectors.joining(" "));
    }

    private static String getPosTagStr(List<Word> words) {
        return words.stream().map(w -> w.getPosTag().getTag()).collect(Collectors.joining(" "));
    }

    private String getLexicalStr() {
        return getLexicalStr(annotated);
    }

    private String getPosTagStr() {
        return getPosTagStr(annotated);
    }

    public String getAnalyzedQuestionStr() {
        return getLexicalStr();
    }

    public List<DataModelBinding> getBindings() {
        return annotated.stream().map(w -> w.getText())
                .map(this::getBinding)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public AnalysisStep resolve(DataModelTypePattern rule) {
        final DataModelType modelType = Objects.requireNonNull(rule).getDataModelType();
        final Pattern rulePattern = Pattern.compile(rule.getPattern());
        //logger.debug(marker, "Check to replace pattern: '{}' ---> '{}'", rulePattern, modelType);

        final List<DataModelBinding> bindings = new LinkedList<>(dataModelBindings);

        String lexicalStr = getLexicalStr();
        String posTagStr = getPosTagStr();

        Matcher lexicalMatcher = rulePattern.matcher(lexicalStr);
        Matcher posTagMatcher = rulePattern.matcher(posTagStr);

        if (rule.isLexical() && lexicalMatcher.matches()) {
            String placeHolder = createPlaceholder(lexicalStr, modelType);

            Replacement<Word> replacement = replace(annotated, lexicalStr, lexicalMatcher, Arrays.asList(new Word(new POSTag(placeHolder), placeHolder)));
            //logger.debug(marker, "Lexical replacement of pattern:\t\t'{}'\t\t'{}' ---> '{}'\t\tResult: '{}'", rulePattern, getLexicalStr(replacement.getReplaced()), getLexicalStr(replacement.getReplacement()), getLexicalStr(replacement.getResult()));

            return new AnalysisStep(bindings, replacement.getResult());
        } else if (!rule.isLexical() && posTagMatcher.matches()) {
            String placeHolder = createPlaceholder(posTagStr, modelType);

            Replacement<Word> replacement = replace(annotated, posTagStr, posTagMatcher, Arrays.asList(new Word(new POSTag(placeHolder), placeHolder)));
            //logger.debug(marker, "POSTag replacement of pattern:\t\t'{}'\t\t'{}' ---> '{}'\t\tResult: '{}'", rulePattern, getPosTagStr(replacement.getReplaced()), getPosTagStr(replacement.getReplacement()), getPosTagStr(replacement.getResult()));

            // bind
            bindings.add(new DataModelBinding(modelType, getLexicalStr(replacement.getExtracted()), placeHolder));
            //logger.debug(marker, "Bound '{}' to '{}'", placeHolder, getLexicalStr(replacement.getExtracted()));

            return new AnalysisStep(bindings, replacement.getResult());
        }

        return null;
    }

    private String createPlaceholder(String target, DataModelType modelType) {
        int unusedIdx = 1;
        String placeHolder = String.format("%s_%d", modelType.name(), unusedIdx);
        while (target.contains(placeHolder)) {
            placeHolder = String.format("%s_%d", modelType.name(), unusedIdx++);
        }
        return placeHolder;
    }

    public AnalysisStep clean(List<Pattern> stopPatterns) {
        List<Word> newAnnotated = new ArrayList<>(annotated);

        List<Pattern> patterns = new ArrayList<>(stopPatterns);
        patterns.add(PUNCT_PATTERN);

        for (Pattern pattern : patterns) {
            boolean b = true;
            while(b) {
                String lexicalStr = getLexicalStr(newAnnotated);
                Matcher lexicalMatcher = pattern.matcher(lexicalStr);

                if (lexicalMatcher.matches()) {
                    Replacement<Word> replacement = replace(newAnnotated, lexicalStr, lexicalMatcher, Arrays.asList());
                    //logger.debug(marker, "Clean replacement of pattern:\t\t{}\t\t'{}' ---> '{}'\t\tResult: '{}'", pattern, getLexicalStr(replacement.getReplaced()), getLexicalStr(replacement.getReplacement()), getLexicalStr(replacement.getResult()));

                    newAnnotated = replacement.getResult();
                    logger.debug(marker, "Cleaned Step: '{}' / '{}'", getLexicalStr(newAnnotated), getPosTagStr(newAnnotated));
                } else {
                    b = false;
                }
            }
        }

        return new AnalysisStep(dataModelBindings, newAnnotated);
    }

    private Optional<DataModelBinding> getBinding(String term) {
        return dataModelBindings.stream().filter(binding -> binding.getPlaceHolder().equals(term)).findFirst();
    }

    @Override
    public String toString() {
        return "AnalysisStep{" +
                "annotated=" + annotated +
                '}';
    }


    private static int wordIndex(String str, int idx) {
        return StringUtils.countMatches(str.substring(0, idx), " ");
    }

    private static Replacement<Word> replace(List<Word> words, String wordsStr, Matcher matcher, List<Word> replacement) {
        if (!matcher.matches()) {
            throw new IllegalStateException("No match!");
        }
        if (matcher.groupCount() < 1) {
            throw new IllegalStateException("Match has no capturing group!");
        }

        int startReplaceWordIdx = wordIndex(wordsStr, matcher.start(1));
        int endReplaceWordIdx = wordIndex(wordsStr, matcher.end(1));
        int startExtractWordIdx = (matcher.groupCount() >= 2)? wordIndex(wordsStr, matcher.start(2)) : startReplaceWordIdx;
        int endExtractWordIdx = (matcher.groupCount() >= 2)? wordIndex(wordsStr, matcher.end(2)) : endReplaceWordIdx;

        List<Word> result = new ArrayList<>();
        result.addAll(words.subList(0, startReplaceWordIdx));
        result.addAll(replacement);
        result.addAll(words.subList(endReplaceWordIdx+1, words.size()));

        return new Replacement<>(
                new ArrayList<>(words),
                words.subList(startExtractWordIdx, endExtractWordIdx+1),
                words.subList(startReplaceWordIdx, endReplaceWordIdx+1),
                new ArrayList<>(replacement),
                result
        );
    }

    private static class Replacement<T> {
        private final List<T> original;
        private final List<T> extracted;
        private final List<T> replaced;
        private final List<T> replacement;
        private final List<T> result;

        public Replacement(List<T> original, List<T> extracted, List<T> replaced, List<T> replacement, List<T> result) {
            this.original = original;
            this.extracted = extracted;
            this.replaced = replaced;
            this.replacement = replacement;
            this.result = result;
        }

        public List<T> getOriginal() {
            return original;
        }

        public List<T> getExtracted() {
            return extracted;
        }

        public List<T> getReplaced() {
            return replaced;
        }

        public List<T> getReplacement() {
            return replacement;
        }

        public List<T> getResult() {
            return result;
        }

        @Override
        public String toString() {
            return "Replacement{" +
                    "original=" + original +
                    ", extracted=" + extracted +
                    ", replaced=" + replaced +
                    ", replacement=" + replacement +
                    ", result=" + result +
                    '}';
        }
    }
}
