package net.stargraph.core.tools.SimpleIE.impl;

import net.stargraph.core.annotation.pos.Word;
import net.stargraph.core.tools.SimpleIE.SimpleIE;
import net.stargraph.model.Extraction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class SimpleExtractionIE extends SimpleIE<Extraction> {

    @Override
    protected Extraction createPassage(Word relation, List<List<Word>> arguments) {
        String rel = relation.getText();
        List<String> args = new ArrayList<>();
        for (List<Word> argument : arguments) {
            args.add(argument.stream().map(w -> w.getText()).collect(Collectors.joining(" ")));
        }

        return new Extraction(rel, args);
    }
}
