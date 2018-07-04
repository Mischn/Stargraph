package net.stargraph.core.tools.SimpleIE;

import net.stargraph.core.tools.SimpleIE.graph.GraphNode;
import net.stargraph.core.tools.SimpleIE.graph.RootNode;
import net.stargraph.model.Extraction;
import org.lambda3.text.simplification.discourse.utils.parseTree.ParseTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public final class SimpleIE {
    private interface GraphOperation {
        void operate(GraphNode node);
    }

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker;

    private static final StanfordGraphParser PARSER = new StanfordGraphParser();

    private void traverseToWord(GraphNode node, String word, GraphOperation graphOperation) {
        if (node.getWord().equals(word)) {
            graphOperation.operate(node);
            return;
        }
        node.getOutEdges().forEach(e -> traverseToWord(e.getTrgNode(), word, graphOperation));
    }

    private void yieldWordModifiers(List<Extraction> extractions, GraphNode node) {
        Extraction extraction = new Extraction(node.getWord());
        node.yield(Arrays.asList("!nmod"), Arrays.asList()).forEach(n -> {
            extraction.addArgument(n.yieldStr(Arrays.asList("**"), Arrays.asList("!acl:relcl")));
        });
        extractions.add(extraction);
    }

    private void traverseRelations(GraphNode node, GraphOperation graphOperation) {
        if (node.getTag().matches("V.+")) {
            graphOperation.operate(node);
        }
        node.getOutEdges(Arrays.asList("*"), Arrays.asList("auxpass", "aux")).forEach(e -> traverseRelations(e.getTrgNode(), graphOperation));
    }

    private void yieldRelationalArguments(List<Extraction> extractions, GraphNode node) {
        Extraction extraction = new Extraction(node.getWord());
        node.yield(Arrays.asList("!nsubj", "!nsubjpass", "!dobj", "!nmod"), Arrays.asList()).forEach(n -> {
            extraction.addArgument(n.yieldStr(Arrays.asList("**"), Arrays.asList("!acl:relcl")));
        });
        extractions.add(extraction);
    }

    public List<Extraction> extract(String text) {
        List<Extraction> res = new ArrayList<>();
        RootNode root = null;
        try {
            root = PARSER.parse(text);
        } catch (ParseTreeException e) {
            logger.error(marker, "Failed to parse text: '" + text + "'");
            return Collections.emptyList();
        }
        traverseRelations(root, node -> yieldRelationalArguments(res, node));
        return res;
    }

    public Extraction extractModifiers(String text, String word) {
        List<Extraction> res = new ArrayList<>();
        RootNode root = null;
        try {
            root = PARSER.parse(text);
        } catch (ParseTreeException e) {
            logger.error(marker, "Failed to parse text: '" + text + "'");
            return null;
        }
        traverseToWord(root, word, node -> yieldWordModifiers(res, node));
        if (res.size() > 0) {
            return res.get(0);
        } else {
            return null;
        }
    }
}
