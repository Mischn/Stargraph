package net.stargraph.core.annotation.binding;

import net.stargraph.core.annotation.pos.POSAnnotator;
import net.stargraph.core.annotation.pos.Word;
import net.stargraph.query.Language;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BindAnnotator<T> {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected Marker marker = MarkerFactory.getMarker("bind-annotator");

    private POSAnnotator posAnnotator;
    private Language language;
    private List<BindingPattern<T>> bindingPatterns;

    public BindAnnotator(POSAnnotator posAnnotator, Language language, List<BindingPattern<T>> bindingPatterns) {
        this.posAnnotator = posAnnotator;
        this.language = language;
        this.bindingPatterns = bindingPatterns;
    }

    public void setPosAnnotator(POSAnnotator posAnnotator) {
        this.posAnnotator = posAnnotator;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public void setBindingPatterns(List<BindingPattern<T>> bindingPatterns) {
        this.bindingPatterns = bindingPatterns;
    }

    public List<Binding<T>> extractBindings(String sentence) {
        List<Word> posAnnotated = posAnnotator.run(language, sentence);
        return extractBindings(posAnnotated);
    }

    public List<Binding<T>> extractBindings(List<Word> posAnnotated) {
        List<Binding<T>> initialBindings = posAnnotated.stream().map(w -> new Binding<T>(Arrays.asList(w), null, null)).collect(Collectors.toList());
        return extractBindings2(initialBindings);
    }

    public List<Binding<T>> extractBindings2(List<Binding<T>> bindings) {
        List<Binding<T>> current = new ArrayList<>(bindings);

        boolean hasMatch = true;
        while (hasMatch) {
            hasMatch = false;

            String lexicalStr = getLexicalStr(current);
            String posTagStr = getPosTagStr(current);
            logger.debug(marker, "{} / {}", lexicalStr, posTagStr);

            for (BindingPattern<T> rule : bindingPatterns) {
                final T trgObject = rule.getObject();
                final Pattern pattern = Pattern.compile(rule.getPattern());

                Matcher lexicalMatcher = pattern.matcher(lexicalStr);
                Matcher posTagMatcher = pattern.matcher(posTagStr);

                if (!rule.isPosPattern() && lexicalMatcher.matches()) {
                    logger.debug(marker, "Matched lexical rule: {} -> {}", rule.getPattern(), trgObject);
                    String placeHolder = createPlaceholder(lexicalStr, trgObject);

                    Binding binding = new Binding(null, trgObject, placeHolder);
                    current = replace(current, lexicalStr, lexicalMatcher, binding);

                    hasMatch = true;
                } else if (rule.isPosPattern() && posTagMatcher.matches()) {
                    logger.debug(marker, "Matched POS rule: {} -> {}", rule.getPattern(), trgObject);
                    String placeHolder = createPlaceholder(posTagStr, trgObject);

                    Binding binding = new Binding(null, trgObject, placeHolder);
                    current = replace(current, posTagStr, posTagMatcher, binding);
                    hasMatch = true;
                }

                if (hasMatch) {
                    break;
                }
            }
        }

        return current;
    }





    private static <T> String createPlaceholder(String target, T object) {
        int unusedIdx = 1;
        String placeHolder = String.format("%s_%d", object, unusedIdx);
        while (target.contains(placeHolder)) {
            placeHolder = String.format("%s_%d", object, unusedIdx++);
        }
        return placeHolder;
    }

    private static <T> String getLexicalStr(List<Binding<T>> bindings) {
        return bindings.stream().map(b -> (b.isBound())? b.getPlaceHolder() : ((Word)b.getWords().get(0)).getText()).collect(Collectors.joining(" "));
    }

    private static <T> String getPosTagStr(List<Binding<T>> bindings) {
        return bindings.stream().map(b -> (b.isBound())? b.getPlaceHolder() : ((Word)b.getWords().get(0)).getPosTagString()).collect(Collectors.joining(" "));
    }


    private static int wordIndex(String str, int idx) {
        return StringUtils.countMatches(str.substring(0, idx), " ");
    }

    private List<Binding> replace(List<Binding<T>> bindings, String wordsStr, Matcher matcher, Binding<T> binding) {
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

        List<Binding<T>> extracted = bindings.subList(startExtractWordIdx, endExtractWordIdx+1);
        List<Binding<T>> replaced = bindings.subList(startReplaceWordIdx, endReplaceWordIdx+1);

        List<Word> extractedWords = new ArrayList<>();
        for (Binding b : extracted) {
            extractedWords.addAll(b.getWords());
        }

        logger.debug(marker, "Replaced '{}' with '{}'", replaced, binding.getPlaceHolder());

        // set Words
        binding.setWords(extractedWords);

        List<Binding> result = new ArrayList<>();
        result.addAll(bindings.subList(0, startReplaceWordIdx));
        result.addAll(Arrays.asList(binding));
        result.addAll(bindings.subList(endReplaceWordIdx+1, bindings.size()));

        return result;
    }
}
