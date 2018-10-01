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
import com.typesafe.config.ConfigObject;
import net.stargraph.StarGraphException;
import net.stargraph.core.Stargraph;
import net.stargraph.core.model.InstanceEntityImpl;
import net.stargraph.core.search.EntitySearcher;
import net.stargraph.core.search.ModifiableRangeSearchParams;
import net.stargraph.core.search.SearchQueryGenerator;
import net.stargraph.data.processor.BaseProcessor;
import net.stargraph.data.processor.Holder;
import net.stargraph.data.processor.ProcessorException;
import net.stargraph.model.InstanceEntity;
import net.stargraph.model.ValueEntity;
import net.stargraph.query.Language;
import net.stargraph.rank.*;

import java.io.Serializable;
import java.util.*;

/**
 * Can be placed in the workflow to create passages.
 */
public final class ValueGeneratorProcessor extends BaseProcessor {
    public static String name = "value-generator";

    private Stargraph stargraph;
    private EntitySearcher entitySearcher;
    private Map<Language, String> searchTerms;
    private double threshold;

    public ValueGeneratorProcessor(Stargraph stargraph, Config config) {
        super(config);
        this.stargraph = stargraph;
        this.entitySearcher = stargraph.getEntitySearcher();

        if (!getConfig().getIsNull("search-terms")) {
            this.searchTerms = loadSearchTerms(getConfig());
        }
        if (!getConfig().getIsNull("threshold")) {
            this.threshold = getConfig().getDouble("threshold");
        }
    }

    @Override
    public void doRun(Holder<Serializable> holder) throws ProcessorException {
        Serializable entry = holder.get();

        /**
         * One might consider avoiding redundant calculations for same entities here,
         * but when executed after a sink-duplicate processor, this should be okay.
         */
        if (entry instanceof InstanceEntity) {
            InstanceEntity entity = (InstanceEntity)entry;
            Language language = stargraph.getKBCore(holder.getKBId().getId()).getLanguage();
            if (!searchTerms.containsKey(language)) {
                throw new StarGraphException("No search-term specified for language: " + language);
            }
            String searchStr = searchTerms.get(language);

            ModifiableSearchParams searchParams = ModifiableSearchParams.create(holder.getKBId().getId());
            String rankString = searchStr;
            ModifiableRankParams rankParams = ParamsBuilder.word2vec().threshold(Threshold.min(threshold));
            ModifiableRangeSearchParams rangeSearchParams = ModifiableRangeSearchParams.create()
                    .incomingEdges(false)
                    .outgoingEdges(true)
                    .propertyTypes(SearchQueryGenerator.PropertyTypes.NON_TYPE_ONLY);

            Scores scores = entitySearcher.pivotedSearch(entity, searchParams, rangeSearchParams, 1, rankString, rankParams, true);

            List<String> otherValues = (entity.getOtherValues() != null)? new ArrayList<>(entity.getOtherValues()) : new ArrayList<>();
            scores.stream()
                    .filter(s -> s.getEntry() instanceof ValueEntity)
                    .map(s -> ((ValueEntity)s.getEntry()).getValue())
                    .forEach(s -> otherValues.add(s));

            holder.set(new InstanceEntityImpl(entity.getId(), entity.getValue(), entity.isClass(), otherValues));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    private Map<Language, String> loadSearchTerms(Config config) {
        Map<Language, String> termsByLang = new LinkedHashMap<>();
        ConfigObject configObject = config.getObject("search-terms");

        configObject.keySet().forEach(strLang -> {
            Language language = Language.valueOf(strLang.toUpperCase());
            String term = configObject.toConfig().getString(strLang);

            termsByLang.put(language, term);
        });

        return termsByLang;
    }
}
