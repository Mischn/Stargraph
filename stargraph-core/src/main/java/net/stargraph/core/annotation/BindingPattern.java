package net.stargraph.core.annotation;

import net.stargraph.query.Language;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Specifies a regex-pattern for identifying a specific object
 * @param <T> The type of the object
 */
public class BindingPattern<T> {
    private String pattern; // regex-pattern
    private T object;
    private boolean posPattern;

    public BindingPattern(String pattern, T object, Language language) {
        this.pattern = pattern;
        this.object = object;
        this.posPattern = determinePOSPattern(pattern, language);
    }

    private static boolean determinePOSPattern(String pattern, Language language) {
        List<String> x = PartOfSpeechSet.getPOSSet(language).stream()
                .map(t -> t.getTag())
                .filter(s -> s.matches("\\w+"))
                .map(s -> Pattern.quote(s))
                .collect(Collectors.toList());

        String regex = "^.*?(?<!\\\\w)(" + x.stream().collect(Collectors.joining("|")) + ")(?!\\\\w).*$";
        return pattern.matches(regex);
    }

    public String getPattern() {
        return pattern;
    }

    public T getObject() {
        return object;
    }

    public boolean isPosPattern() {
        return posPattern;
    }
}
