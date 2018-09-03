package net.stargraph.core.annotation.binding;

import net.stargraph.core.annotation.pos.Word;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Binds words to an object
 * @param <T> The type of the object
 */
public class Binding<T> {
    private List<Word> words;
    private T object; // optional
    private String placeHolder;  // optional

    public Binding(List<Word> words, T object, String placeHolder) {
        this.words = words;
        this.object = object;
        this.placeHolder = placeHolder;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }

    public List<Word> getWords() {
        return words;
    }

    public String getBoundText() {
        return words.stream().map(w -> w.getText()).collect(Collectors.joining(" "));
    }

    public T getObject() {
        return object;
    }

    public String getPlaceHolder() {
        return placeHolder;
    }

    public boolean isBound() {
        return object != null;
    }

    @Override
    public String toString() {
        String res = (isBound())? words.toString() + "=>(" + placeHolder + ")" : words.toString();
        return res;
    }
}
