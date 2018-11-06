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
import net.stargraph.core.Namespace;
import net.stargraph.core.SparqlCreator;
import net.stargraph.core.Stargraph;
import net.stargraph.core.impl.jena.JenaBaseSearcher;
import net.stargraph.core.impl.jena.JenaGraphSearcher;
import net.stargraph.core.model.InstanceEntityImpl;
import net.stargraph.data.processor.BaseProcessor;
import net.stargraph.data.processor.Holder;
import net.stargraph.data.processor.ProcessorException;
import net.stargraph.model.InstanceEntity;
import net.stargraph.query.Language;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.engine.binding.Binding;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Can be placed in the workflow to create passages.
 */
public final class ValueGeneratorFixedProcessorGND extends BaseProcessor {
    public static String name = "value-generator-fixed-gnd";

    private Stargraph stargraph;
    private Map<Language, List<String>> properties;
    private SparqlCreator sparqlCreator;

    public ValueGeneratorFixedProcessorGND(Stargraph stargraph, Config config) {
        super(config);
        this.stargraph = stargraph;
        this.sparqlCreator = new SparqlCreator();

        if (!getConfig().getIsNull("properties")) {
            this.properties = loadProperties(getConfig());
        }
    }

    private String createSPARQLQuery(String entityUri, Language language, List<String> properties) {
        List<String> statements = new ArrayList<>();
        String subjStr = SparqlCreator.formatURI(entityUri);
        String predStr = sparqlCreator.createPred(properties, 1);
        String objStr = "?o";
        statements.add(sparqlCreator.createStmt(subjStr, predStr, objStr));
        statements.add(sparqlCreator.createLangFilterStmt("?t", Arrays.asList(language), true));

        return "SELECT ?t { "
                + sparqlCreator.stmtJoin(statements, false)
                + " }";
    }

    private String edit(String literalText) {
        Pattern pattern = Pattern.compile("^\\s*?([^,]*?\\w[^,]*?)\\s*?,\\s*(.*?)$");
        Matcher matcher = pattern.matcher(literalText);
        if (matcher.matches()) {
            return matcher.group(2) + " " + matcher.group(1);
        } else {
            return literalText;
        }
    }

    @Override
    public void doRun(Holder<Serializable> holder) throws ProcessorException {
        Serializable entry = holder.get();
        JenaGraphSearcher graphSearcher = (JenaGraphSearcher) stargraph.getKBCore(holder.getKBId().getId()).getGraphSearcher();
        Namespace namespace = stargraph.getKBCore(holder.getKBId().getId()).getNamespace();

        /**
         * One might consider avoiding redundant calculations for same entities here,
         * but when executed after a sink-duplicate processor, this should be okay.
         */
        if (entry instanceof InstanceEntity) {
            InstanceEntity entity = (InstanceEntity)entry;
            Language language = stargraph.getKBCore(holder.getKBId().getId()).getLanguage();
            if (!properties.containsKey(language)) {
                throw new StarGraphException("No properties specified for language: " + language);
            }

            List<String> props = properties.get(language);
            if (props.size() <= 0) {
                return;
            }

            String entityUri = namespace.expandURI(entity.getId());
            String sparqlQuery = createSPARQLQuery(entityUri, language, props);

            List<String> otherValues = (entity.getOtherValues() != null)? new ArrayList<>(entity.getOtherValues()) : new ArrayList<>();
            graphSearcher.sparqlQuery(sparqlQuery, new JenaBaseSearcher.SparqlIteration() {
                @Override
                public void process(Binding binding) {
                    Map<String, Node> varMap = JenaGraphSearcher.getVarMap(binding);
                    if (!varMap.get("t").isLiteral()) {
                        return;
                    }

                    otherValues.add(edit(varMap.get("t").getLiteralLexicalForm()));
                }
            });

            System.out.println("OTHER VALUES FOR '" + entityUri + "': " + otherValues);
            if (otherValues.size() > 0) {
                System.out.println("############################");
                System.out.println("############################");
                System.out.println("############################");
            }

            holder.set(new InstanceEntityImpl(entity.getId(), entity.getValue(), entity.isClass(), otherValues));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    private Map<Language, List<String>> loadProperties(Config config) {
        Map<Language, List<String>> propertiesByLang = new LinkedHashMap<>();
        ConfigObject configObject = config.getObject("properties");

        configObject.keySet().forEach(strLang -> {
            Language language = Language.valueOf(strLang.toUpperCase());
            List<String> properties = configObject.toConfig().getStringList(strLang);

            propertiesByLang.put(language, properties);
        });

        return propertiesByLang;
    }
}
