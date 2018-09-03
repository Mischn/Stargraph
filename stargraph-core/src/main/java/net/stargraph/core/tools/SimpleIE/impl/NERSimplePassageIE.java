package net.stargraph.core.tools.SimpleIE.impl;

import net.stargraph.core.Stargraph;
import net.stargraph.core.annotation.pos.Word;
import net.stargraph.core.ner.NER;
import net.stargraph.core.ner.NamedEntity;
import net.stargraph.core.tools.SimpleIE.SimpleIE;
import net.stargraph.model.PassageExtraction;
import net.stargraph.model.date.TimeParser;
import net.stargraph.model.date.TimeRange;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class NERSimplePassageIE extends SimpleIE<PassageExtraction> {
    private static final TimeParser TIME_PARSER = new TimeParser();
    private NER ner;

    public NERSimplePassageIE(Stargraph stargraph, String dbId) {
        this.ner = stargraph.getKBCore(dbId).getNER();
    }

    @Override
    protected PassageExtraction createPassage(Word relation, List<List<Word>> arguments) {
        String rel = relation.getText();
        List<String> terms = new ArrayList<>();
        List<TimeRange> temporals = new ArrayList<>();

        for (List<Word> argument : arguments) {
            String s = argument.stream().map(a -> a.getText()).collect(Collectors.joining(" "));
            List<NamedEntity> namedEntities = ner.searchWithoutLink(s);
            terms.addAll(namedEntities.parallelStream()
                    .filter(l -> !l.getCat().equals("DATE"))
                    .map(l -> l.getValue()).collect(Collectors.toList()));
            temporals.addAll(TIME_PARSER.parse(s));
        }

        return new PassageExtraction(rel, terms, temporals);
    }
}
