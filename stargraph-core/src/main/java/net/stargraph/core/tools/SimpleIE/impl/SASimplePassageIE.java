package net.stargraph.core.tools.SimpleIE.impl;

import net.stargraph.core.Stargraph;
import net.stargraph.core.annotation.*;
import net.stargraph.core.query.Rules;
import net.stargraph.core.query.nli.DataModelType;
import net.stargraph.core.query.nli.DataModelTypePattern;
import net.stargraph.core.tools.SimpleIE.SimpleIE;
import net.stargraph.model.PassageExtraction;
import net.stargraph.model.date.TimeParser;
import net.stargraph.model.date.TimeRange;
import net.stargraph.query.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class SASimplePassageIE extends SimpleIE<PassageExtraction> {
    private static final TimeParser TIME_PARSER = new TimeParser();

    private BindAnnotator<DataModelType> textAnnotator;

    public SASimplePassageIE(Stargraph stargraph, String dbId) {
        POSAnnotator posAnnotator = stargraph.createPOSAnnotatorFactory().create();
        Language language = stargraph.getKBCore(dbId).getLanguage();
        Rules rules = new Rules(stargraph.getMainConfig());
        List<BindingPattern<DataModelType>> bindingPatterns = new ArrayList<>();
        for (DataModelTypePattern p : rules.getDataModelTypeRules(language)) {
            bindingPatterns.add(new BindingPattern<>(p.getPattern(), p.getDataModelType(), language));
        }
        this.textAnnotator = new BindAnnotator<>(posAnnotator, language, bindingPatterns);
    }

    public List<String> extractTerms(List<Word> posAnnotated) {
        List<Word> words = textAnnotator.extractBindings(posAnnotated);

        List<String> terms = new ArrayList<>();
        for (Word word : words) {
            if (word instanceof Binding) {
                Binding binding = (Binding) word;
                DataModelType modelType = (DataModelType) binding.getObject();
                if (modelType.equals(DataModelType.INSTANCE)
                        || modelType.equals(DataModelType.CLASS)
                        || modelType.equals(DataModelType.ATCLASS)
                        || modelType.equals(DataModelType.COMPLEX_CLASS)) {
                    terms.add(binding.getText());
                }
            }
        }

        return terms;
    }

    @Override
    protected PassageExtraction createPassage(Word relation, List<List<Word>> arguments) {
        String rel = relation.getText();
        List<String> terms = new ArrayList<>();
        List<TimeRange> temporals = new ArrayList<>();

        for (List<Word> argument : arguments) {
            String s = argument.stream().map(a -> a.getText()).collect(Collectors.joining(" "));

            terms.addAll(extractTerms(argument));
            temporals.addAll(TIME_PARSER.parse(s));
        }

        return new PassageExtraction(rel, terms, temporals);
    }
}
