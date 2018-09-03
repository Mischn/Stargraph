package net.stargraph.core.tools.SimpleIE;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.process.DocumentPreprocessor;
import net.stargraph.core.annotation.pos.POSTag;
import net.stargraph.core.annotation.pos.Word;
import net.stargraph.core.tools.SimpleIE.graph.GraphNode;
import net.stargraph.core.tools.SimpleIE.graph.RootNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
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

    private static List<String> splitIntoSentences(String text) {
        List<String> res = new ArrayList<>();

        DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(text));
        for (List<HasWord> sentence : dp) {
            res.add(SentenceUtils.listToString(sentence));
        }

        return res;
    }

    public List<T> extractFromSentence(String sentence) {
        List<T> res = new ArrayList<>();
        RootNode root = null;
        try {
            root = PARSER.parse(sentence);
        } catch (Exception e) {
            logger.error(marker, "Failed to parse sentence: '" + sentence + "'");
            return new ArrayList<>();
        }
        traverseRelations(root, node -> yieldRelationalArguments(res, node));
        return res;
    }

    public List<T> extractFromText(String text) {
        List<T> res = new ArrayList<>();
        for (String sentence : splitIntoSentences(text)) {
            res.addAll(extractFromSentence(sentence));
        }
        return res;
    }

    public T extractModifiersFromSentence(String sentence, String word) {
        List<T> res = new ArrayList<>();
        RootNode root;
        try {
            root = PARSER.parse(sentence);
        } catch (Exception e) {
            logger.error(marker, "Failed to parse sentence: '" + sentence + "'");
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
