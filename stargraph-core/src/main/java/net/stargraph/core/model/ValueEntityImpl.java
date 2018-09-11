package net.stargraph.core.model;

import net.stargraph.model.ValueEntity;

import java.util.Objects;

public class ValueEntityImpl extends ValueEntity {
    public static String DEFAULT_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#string";

    private String value;
    private String dataType;
    private String language; // optional

    public ValueEntityImpl(String id, String value, String dataType, String language) {
        super(id);
        this.value = Objects.requireNonNull(value);
        this.dataType = (dataType != null)? dataType : DEFAULT_DATA_TYPE;
        this.language = language;
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getRankableValue() {
        return getValue();
    }
}
