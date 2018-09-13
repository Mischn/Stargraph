package net.stargraph.core.query.nli;

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
import com.typesafe.config.ConfigValue;
import net.stargraph.UnsupportedLanguageException;
import net.stargraph.core.annotation.binding.BindingPattern;
import net.stargraph.core.query.QueryType;
import net.stargraph.query.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Rules {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("query");

    private Map<Language, List<BindingPattern<DataModelType>>> dataModelBindingPatterns;
    private Map<Language, List<QueryPlannerPattern>> queryPlannerPatterns;
    private Map<Language, List<QueryTypePatterns>> queryTypePatterns;

    public Rules(Config config) {
        logger.info(marker, "Loading Rules.");
        this.dataModelBindingPatterns = loadDataModelBindingPatterns(Objects.requireNonNull(config));
        this.queryPlannerPatterns = loadQueryPlannerPatterns(Objects.requireNonNull(config));
        this.queryTypePatterns = loadQueryTypePatterns(config);
    }

    public List<BindingPattern<DataModelType>> getDataModelBindingPatterns(Language language) {
        if (dataModelBindingPatterns.containsKey(language)) {
            return dataModelBindingPatterns.get(language);
        }
        throw new UnsupportedLanguageException(language);
    }

    public List<QueryPlannerPattern> getQueryPlannerPatterns(Language language) {
        if (queryPlannerPatterns.containsKey(language)) {
            return queryPlannerPatterns.get(language);
        }
        throw new UnsupportedLanguageException(language);
    }

    public List<QueryTypePatterns> getQueryTypeRules(Language language) {
        if (queryTypePatterns.containsKey(language)) {
            return queryTypePatterns.get(language);
        }
        throw new UnsupportedLanguageException(language);
    }

    private Map<Language, List<BindingPattern<DataModelType>>> loadDataModelBindingPatterns(Config config) {
        Map<Language, List<BindingPattern<DataModelType>>> rulesByLang = new HashMap<>();
        ConfigObject configObject = config.getObject("rules.syntatic-pattern");

        configObject.keySet().forEach(strLang -> {
            Language language = Language.valueOf(strLang.toUpperCase());

            rulesByLang.compute(language, (l, r) -> {

                List<BindingPattern<DataModelType>> rules = new ArrayList<>();
                List<? extends Config> patternCfg = configObject.toConfig().getConfigList(strLang);

                for (Config cfg : patternCfg) {
                    Map.Entry<String, ConfigValue> entry = cfg.entrySet().stream().findFirst().orElse(null);
                    String modelStr = entry.getKey();
                    @SuppressWarnings("unchecked")
                    List<String> patternList = (List<String>) entry.getValue().unwrapped();
                    rules.addAll(patternList.stream()
                            .map(p -> new BindingPattern<DataModelType>(p, DataModelType.valueOf(modelStr), l))
                            .collect(Collectors.toList()));
                }

                logger.info(marker, "Loaded {} Data Model Type patterns for '{}'", rules.size(), l);

                return rules;
            });
        });


        return rulesByLang;
    }

    @SuppressWarnings("unchecked")
    private Map<Language, List<QueryPlannerPattern>> loadQueryPlannerPatterns(Config config) {
        Map<Language, List<QueryPlannerPattern>> rulesByLang = new LinkedHashMap<>();
        ConfigObject configObject = config.getObject("rules.planner-pattern");

        configObject.keySet().forEach(strLang -> {
            Language language = Language.valueOf(strLang.toUpperCase());
            List<QueryPlannerPattern> queryPlannerPatterns = new ArrayList<>();

            configObject.toConfig().getObjectList(strLang).forEach(e -> {
                String planId = e.keySet().toArray(new String[1])[0];
                List<QueryPlan> queryPlans = new ArrayList<>();

                e.toConfig().getList(planId).forEach(e2 -> {
                    List<String> triples = (ArrayList) e2.unwrapped();
                    List<TriplePattern> triplePatterns = triples.stream().map(TriplePattern::new).collect(Collectors.toList());

                    QueryPlan queryPlan = new QueryPlan();
                    queryPlan.addAll(triplePatterns);
                    queryPlans.add(queryPlan);
                });

                QueryPlannerPattern queryPlannerPattern = new QueryPlannerPattern(planId, queryPlans);
                queryPlannerPatterns.add(queryPlannerPattern);
            });

            rulesByLang.put(language, queryPlannerPatterns);
        });

        return rulesByLang;
    }

    private Map<Language, List<QueryTypePatterns>> loadQueryTypePatterns(Config config) {
        Map<Language, List<QueryTypePatterns>> rulesByLang = new HashMap<>();
        ConfigObject configObject = config.getObject("rules.query-pattern");

        configObject.keySet().forEach(strLang -> {
            Language language = Language.valueOf(strLang.toUpperCase());

            ConfigObject innerCfg = configObject.toConfig().getObject(strLang);

            List<QueryTypePatterns> patterns = new ArrayList<>();

            rulesByLang.compute(language, (l, q) -> {
                innerCfg.keySet().forEach(key -> {
                    QueryType queryType = QueryType.valueOf(key);
                    List<String> patternStr = innerCfg.toConfig().getStringList(key);
                    patterns.add(new QueryTypePatterns(queryType,
                            patternStr.stream().map(Pattern::compile).collect(Collectors.toList())));
                });

                logger.info(marker, "Loaded {} Query Type patterns for '{}'", patterns.size(), language);
                return patterns;
            });

        });

        return rulesByLang;
    }
}
