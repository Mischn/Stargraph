package net.stargraph.core.tools.SimpleIE;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphFactory;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import net.stargraph.core.tools.SimpleIE.graph.GraphEdge;
import net.stargraph.core.tools.SimpleIE.graph.GraphNode;
import net.stargraph.core.tools.SimpleIE.graph.RootNode;
import org.lambda3.text.simplification.discourse.utils.parseTree.ParseTreeException;
import org.lambda3.text.simplification.discourse.utils.parseTree.ParseTreeParser;

import java.util.ArrayList;
import java.util.List;

public class StanfordGraphParser implements GraphParser {

    @Override
    public RootNode parse(String text) throws ParseTreeException {
        Tree tree = ParseTreeParser.parse(text);
        SemanticGraph sg = SemanticGraphFactory.makeFromTree(tree, SemanticGraphFactory.Mode.BASIC, GrammaticalStructure.Extras.MAXIMAL, null, false, true);

        List<GraphNode> nodes = new ArrayList<>();
        RootNode root = new RootNode();
        nodes.add(root);

        int i = 1;
        for (IndexedWord indexedWord : sg.vertexListSorted()) {
            nodes.add(new GraphNode(indexedWord.word(), i, indexedWord.tag()));
            ++i;
        }

        GraphEdge rootEdge = new GraphEdge(root, nodes.get(sg.getFirstRoot().index()), "root");
        rootEdge.establish();

        for (SemanticGraphEdge e : sg.edgeListSorted()) {
            GraphEdge edge = new GraphEdge(nodes.get(e.getSource().index()), nodes.get(e.getTarget().index()), e.getRelation().getShortName());
            edge.establish();
        }

        return root;
    }
}
