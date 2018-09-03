package net.stargraph.core.processors;

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

import com.typesafe.config.Config;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.process.DocumentPreprocessor;
import net.stargraph.core.Stargraph;
import net.stargraph.core.annotation.binding.BindAnnotator;
import net.stargraph.core.annotation.binding.Binding;
import net.stargraph.core.annotation.binding.BindingPattern;
import net.stargraph.core.annotation.pos.POSAnnotator;
import net.stargraph.core.query.Rules;
import net.stargraph.core.query.nli.DataModelType;
import net.stargraph.data.processor.BaseProcessor;
import net.stargraph.data.processor.Holder;
import net.stargraph.data.processor.ProcessorException;
import net.stargraph.model.Document;
import net.stargraph.query.Language;

import java.io.Serializable;
import java.io.StringReader;
import java.util.*;

/**
 * Can be placed in the workflow to create terms.
 */
public final class DocumentTermsProcessor extends BaseProcessor {
    public static String name = "document-terms-processor";

    private Stargraph stargraph;
    private Map<Language, BindAnnotator<DataModelType>> textAnnotators;

    public DocumentTermsProcessor(Stargraph stargraph, Config config) {
        super(config);
        this.stargraph = Objects.requireNonNull(stargraph);
        this.textAnnotators = new HashMap<>();
    }

    private static List<String> splitIntoSentences(String text) {
        List<String> res = new ArrayList<>();

        DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(text));
        for (List<HasWord> sentence : dp) {
            res.add(SentenceUtils.listToString(sentence));
        }

        return res;
    }

    private BindAnnotator<DataModelType> getBindAnnotator(Language language) {
        if (textAnnotators.containsKey(language)) {
            return textAnnotators.get(language);
        } else {
            POSAnnotator posAnnotator = stargraph.createPOSAnnotatorFactory().create();
            Rules rules = new Rules(stargraph.getMainConfig());
            List<BindingPattern<DataModelType>> bindingPatterns = rules.getDataModelBindingPatterns(language);

            BindAnnotator ba = new BindAnnotator<>(posAnnotator, language, bindingPatterns);
            textAnnotators.put(language, ba);
            return ba;
        }
    }

    public List<String> extractTerms(Language language, String text) {
        List<String> terms = new ArrayList<>();

        for (String sentence : splitIntoSentences(text)) {
            List<Binding<DataModelType>> bindings = getBindAnnotator(language).extractBindings(sentence);
            for (Binding binding : bindings) {
                if (binding.isBound()) {
                    DataModelType modelType = (DataModelType) binding.getObject();
                    if (modelType.equals(DataModelType.INSTANCE)
                            || modelType.equals(DataModelType.CLASS)
                            || modelType.equals(DataModelType.ATCLASS)
                            || modelType.equals(DataModelType.COMPLEX_CLASS)) {
                        terms.add(binding.getBoundText());
                    }
                }
            }
        }

        return terms;
    }

    @Override
    public void doRun(Holder<Serializable> holder) throws ProcessorException {
        Serializable entry = holder.get();

        if (entry instanceof Document) {
            Document document = (Document)entry;

            Language language = stargraph.getKBCore(holder.getKBId().getId()).getLanguage();
            List<String> terms = extractTerms(language, document.getText());

            holder.set(new Document(
                    document.getId(),
                    document.getType(),
                    document.getEntity(),
                    document.getTitle(),
                    document.getSummary(),
                    document.getText(),
                    document.getEntities(),
                    terms,
                    document.getPassages(),
                    document.getPassageExtractions(),
                    document.getExtractions()));
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
