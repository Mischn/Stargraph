package net.stargraph.core.tools.SimpleIE;

import net.stargraph.core.query.annotator.POSTag;
import net.stargraph.core.query.annotator.Word;
import net.stargraph.core.tools.SimpleIE.graph.GraphNode;
import net.stargraph.core.tools.SimpleIE.graph.RootNode;
import org.lambda3.text.simplification.discourse.utils.parseTree.ParseTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public abstract class SimpleIE<T> {
    private interface GraphOperation {
        void operate(GraphNode node);

    }

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Marker marker = MarkerFactory.getMarker("IE");
    private static final StanfordGraphParser PARSER = new StanfordGraphParser();


    public SimpleIE() {}


    protected abstract T createPassage(Word relation, List<List<Word>> arguments);

    private void traverseToWord(GraphNode node, String word, GraphOperation graphOperation) {
        if (node.getWord().equals(word)) {
            graphOperation.operate(node);
            return;
        }
        node.getOutEdges().forEach(e -> traverseToWord(e.getTrgNode(), word, graphOperation));
    }

    private void yieldWordModifiers(List<T> extractions, GraphNode node) {
        Word relation = new Word(new POSTag(node.getTag()), node.getWord());
        List<List<Word>> argumentWords = new ArrayList<>();

        node.yield(Arrays.asList("!nmod"), Arrays.asList()).forEach(n -> {
            argumentWords.add(n.yieldWords(Arrays.asList("**"), Arrays.asList("!acl:relcl.**")));
        });
        extractions.add(createPassage(relation, argumentWords));
    }

    private void traverseRelations(GraphNode node, GraphOperation graphOperation) {
        if (node.getTag().matches("V.+")) {
            graphOperation.operate(node);
        }
        node.getOutEdges(Arrays.asList("*"), Arrays.asList("auxpass", "aux")).forEach(e -> traverseRelations(e.getTrgNode(), graphOperation));
    }

    private void yieldRelationalArguments(List<T> extractions, GraphNode node) {
        Word relation = new Word(new POSTag(node.getTag()), node.getWord());
        List<List<Word>> argumentWords = new ArrayList<>();

        node.yield(Arrays.asList("!nsubj", "!nsubjpass", "!dobj", "!nmod"), Arrays.asList()).forEach(n -> {
            argumentWords.add(n.yieldWords(Arrays.asList("**"), Arrays.asList("!acl:relcl.**")));
        });
        extractions.add(createPassage(relation, argumentWords));
    }

    public List<T> extract(String text) {
        List<T> res = new ArrayList<>();
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

    public T extractModifiers(String text, String word) {
        List<T> res = new ArrayList<>();
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
