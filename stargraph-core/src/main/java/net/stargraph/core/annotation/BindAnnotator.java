package net.stargraph.core.annotation;

import net.stargraph.UnsupportedLanguageException;
import net.stargraph.query.Language;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BindAnnotator<T> extends Annotator {
    private Annotator posAnnotator;
    private Language language;
    private List<BindingPattern<T>> bindingPatterns;

    public BindAnnotator(Annotator posAnnotator, Language language, List<BindingPattern<T>> bindingPatterns) {
        this.posAnnotator = posAnnotator;
        this.language = language;
        this.bindingPatterns = bindingPatterns;
    }

    public List<Word> extractBindings(List<Word> posAnnotated) {
        List<Word> currWords = new ArrayList<>(posAnnotated);

        boolean hasMatch = true;
        while (hasMatch) {
            hasMatch = false;

            String lexicalStr = getLexicalStr(currWords);
            String posTagStr = getPosTagStr(currWords);
            logger.debug(marker, "{} / {}", lexicalStr, posTagStr);

            for (BindingPattern<T> rule : bindingPatterns) {
                final T trgObject = rule.getObject();
                final Pattern pattern = Pattern.compile(rule.getPattern());

                Matcher lexicalMatcher = pattern.matcher(lexicalStr);
                Matcher posTagMatcher = pattern.matcher(posTagStr);

                if (!rule.isPosPattern() && lexicalMatcher.matches()) {
                    logger.debug(marker, "Matched lexical rule: {} -> {}", rule.getPattern(), trgObject);
                    String placeHolder = createPlaceholder(lexicalStr, trgObject);

                    Binding binding = new Binding(trgObject, "tmp", placeHolder);
                    currWords = replace(currWords, lexicalStr, lexicalMatcher, binding);

                    hasMatch = true;
                } else if (rule.isPosPattern() && posTagMatcher.matches()) {
                    logger.debug(marker, "Matched POS rule: {} -> {}", rule.getPattern(), trgObject);
                    String placeHolder = createPlaceholder(posTagStr, trgObject);

                    Binding binding = new Binding(trgObject, "tmp", placeHolder);
                    currWords = replace(currWords, posTagStr, posTagMatcher, binding);
                    hasMatch = true;
                }

                if (hasMatch) {
                    break;
                }
            }
        }

        return currWords;
    }

    @Override
    protected List<Word> doRun(Language language, String sentence) {
        if (!language.equals(this.language)) {
            throw new UnsupportedLanguageException(language);
        }
        return extractBindings(posAnnotator.run(language, sentence));
    }







    private static <T> String createPlaceholder(String target, T object) {
        int unusedIdx = 1;
        String placeHolder = String.format("%s_%d", object, unusedIdx);
        while (target.contains(placeHolder)) {
            placeHolder = String.format("%s_%d", object, unusedIdx++);
        }
        return placeHolder;
    }

    private static String getLexicalStr(List<Word> words) {
        return words.stream().map(w -> (w instanceof Binding)? ((Binding) w).getPlaceHolder() : w.getText()).collect(Collectors.joining(" "));
    }

    private static String getPosTagStr(List<Word> words) {
        return words.stream().map(w -> (w instanceof Binding)? ((Binding) w).getPlaceHolder() : w.getPosTagString()).collect(Collectors.joining(" "));
    }


    private static int wordIndex(String str, int idx) {
        return StringUtils.countMatches(str.substring(0, idx), " ");
    }

    private List<Word> replace(List<Word> words, String wordsStr, Matcher matcher, Binding binding) {
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

        List<Word> extracted = words.subList(startExtractWordIdx, endExtractWordIdx+1);
        List<Word> replaced = words.subList(startReplaceWordIdx, endReplaceWordIdx+1);

        logger.debug(marker, "Replaced '{}' with '{}'", replaced, binding.getPlaceHolder());

        // set text
        binding.setText(getLexicalStr(extracted));

        List<Word> result = new ArrayList<>();
        result.addAll(words.subList(0, startReplaceWordIdx));
        result.addAll(Arrays.asList(binding));
        result.addAll(words.subList(endReplaceWordIdx+1, words.size()));

        return result;
    }
}
