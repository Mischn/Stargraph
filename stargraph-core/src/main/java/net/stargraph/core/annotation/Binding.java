package net.stargraph.core.annotation;

/**
 * Binds a text to an object
 * @param <T> The type of the object
 */
public class Binding<T> extends Word {
    private T object;
    private String placeHolder;

    public Binding(T object, String text, String placeHolder) {
        super(new POSTag(placeHolder), text);
        this.object = object;
        this.placeHolder = placeHolder;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

    public String getPlaceHolder() {
        return placeHolder;
    }

    public void setText(String text) {
        this.text = text;
    }
}
