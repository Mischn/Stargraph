package net.stargraph.core.tools.SimpleIE.graph;

import net.stargraph.IDGenerator;
import net.stargraph.core.annotation.POSTag;
import net.stargraph.core.annotation.Word;

import java.util.*;
import java.util.stream.Collectors;

public class GraphNode {
    public static Comparator<GraphNode> NODE_COMPARATOR = new Comparator<GraphNode>() {
        @Override
        public int compare(GraphNode n1, GraphNode n2) {
            return n1.wordIndex - n2.wordIndex;
        }
    };

    protected final String id;
    protected String word;
    protected int wordIndex;
    protected String tag;

    protected List<GraphEdge> outEdges;
    protected List<GraphEdge> inEdges;

    public GraphNode(String word, int wordIndex, String tag) {
        this.id = IDGenerator.generateUUID();
        this.word = word;
        this.wordIndex = wordIndex;
        this.tag = tag;
        this.outEdges = new ArrayList<>();
        this.inEdges = new ArrayList<>();
    }

    public GraphNode shallowCopy(boolean establishInEdges, boolean establishOutEges) {
        GraphNode res = new GraphNode(word, wordIndex, tag);
        if (establishInEdges) {
            res.establishInEdges(inEdges);
        }
        if (establishOutEges) {
            res.establishOutEdges(outEdges);
        }

        return res;
    }



    /**
     * Example patterns:
     * 'xcomp'
     * 'xcomp.dobj'
     * 'xcomp.!dobj'
     * 'xcomp.dobj.*'
     * 'xcomp.dobj.**'
     */
    private HashSet<GraphNode> _yield(List<String> patterns) {
        HashSet<GraphNode> nodes = new HashSet<>();

        for (String pattern : patterns) {
            if (pattern.contains(".")) {
                String directPattern = pattern.substring(0, pattern.indexOf("."));
                if (directPattern.startsWith("!")) {
                    directPattern = directPattern.substring(1);
                } else {
                    nodes.add(this);
                }

                String nextPattern = pattern.substring(pattern.indexOf(".") + 1);

                String finalDirectPattern = directPattern;
                for (GraphEdge outEdge : outEdges.stream().filter(e -> e.getLabel().equals(finalDirectPattern)).collect(Collectors.toList())) {
                    nodes.addAll(outEdge.getTrgNode()._yield(Arrays.asList(nextPattern)));
                }
            } else {
                String directPattern = pattern;
                if (directPattern.startsWith("!")) {
                    directPattern = directPattern.substring(1);
                } else {
                    nodes.add(this);
                }

                if (directPattern.equals("*")) {
                    for (GraphEdge outEdge : outEdges) {
                        nodes.add(outEdge.getTrgNode());
                    }
                } else if (directPattern.equals("**")) {
                    for (GraphEdge outEdge : outEdges) {
                        nodes.addAll(outEdge.getTrgNode()._yield(Arrays.asList(directPattern)));
                    }
                } else {
                    String finalDirectPattern = directPattern;
                    for (GraphEdge outEdge : outEdges.stream().filter(e -> e.getLabel().equals(finalDirectPattern)).collect(Collectors.toList())) {
                        nodes.add(outEdge.getTrgNode());
                    }
                }
            }
        }

        return nodes;
    }

    public ArrayList<GraphNode> yield(List<String> allowPatterns, List<String> permitPatterns) {
        HashSet<GraphNode> nodes = _yield(allowPatterns);
        nodes.removeAll(_yield(permitPatterns));

        ArrayList<GraphNode> res = new ArrayList<>(nodes);
        res.sort(NODE_COMPARATOR);
        return res;
    }

    public ArrayList<GraphNode> yield() {
        return yield(Arrays.asList("**"), Collections.emptyList());
    }

    public List<Word> yieldWords(List<String> allowPatterns, List<String> permitPatterns) {
        return yield(allowPatterns, permitPatterns).stream().map(n -> new Word(new POSTag(n.tag), n.word)).collect(Collectors.toList());
    }

    public String yieldStr(List<String> allowPatterns, List<String> permitPatterns) {
        return yield(allowPatterns, permitPatterns).stream().map(n -> n.word).collect(Collectors.joining(" "));
    }

    public String yieldStr() {
        return yieldStr(Arrays.asList("**"), Collections.emptyList());
    }



    /**
     * Example patterns:
     * 'xcomp'
     * '*'
     */
    private static List<GraphEdge> selectEdges(List<GraphEdge> edges, List<String> patterns) {
        List<GraphEdge> res = new ArrayList<>();
        for (GraphEdge edge : edges) {
            if (patterns.contains("*") || patterns.contains(edge.getLabel())) {
                res.add(edge);
            }
        }
        return res;
    }

    private static List<GraphEdge> selectEdges(List<GraphEdge> edges, List<String> allowPatterns, List<String> permitPatterns) {
        List<GraphEdge> res = selectEdges(edges, allowPatterns);
        res.removeAll(selectEdges(edges, permitPatterns));

        return res;
    }

    private static List<GraphEdge> selectEdges(List<GraphEdge> edges, boolean selectTrg, GraphNode node) {
        if (selectTrg) {
            return edges.stream().filter(e -> e.getTrgNode().equals(node)).collect(Collectors.toList());
        } else {
            return edges.stream().filter(e -> e.getSrcNode().equals(node)).collect(Collectors.toList());
        }
    }

    private static List<GraphEdge> selectEdges(List<GraphEdge> edges, boolean selectTrg, GraphNode node, String label) {
        return selectEdges(edges, selectTrg, node).stream().filter(e -> e.getLabel().equals(label)).collect(Collectors.toList());
    }




    public List<GraphEdge> getOutEdges(List<String> allowPatterns, List<String> permitPatterns) {
        return selectEdges(outEdges, allowPatterns, permitPatterns);
    }

    public List<GraphEdge> getInEdges(List<String> allowPatterns, List<String> permitPatterns) {
        return selectEdges(inEdges, allowPatterns, permitPatterns);
    }

    public List<GraphEdge> getOutEdges(GraphNode trgNode) {
        return selectEdges(outEdges, true, trgNode);
    }

    public List<GraphEdge> getInEdges(GraphNode srcNode) {
        return selectEdges(inEdges, false, srcNode);
    }

    public List<GraphEdge> getOutEdges(GraphNode trgNode, String label) {
        return selectEdges(outEdges, true, trgNode, label);
    }

    public List<GraphEdge> getInEdges(GraphNode srcNode, String label) {
        return selectEdges(inEdges, false, srcNode, label);
    }



    public void establishOutEdges(List<GraphEdge> outEdges) {
        outEdges.forEach(e -> new GraphEdge(this, e.getTrgNode(), e.getLabel()).establish());
    }

    public void establishInEdges(List<GraphEdge> outEdges) {
        outEdges.forEach(e -> new GraphEdge(e.getSrcNode(), this, e.getLabel()).establish());
    }

    public void establishOutEdges(List<GraphEdge> outEdges, String newLabel) {
        outEdges.forEach(e -> new GraphEdge(this, e.getTrgNode(), newLabel).establish());
    }

    public void establishInEdges(List<GraphEdge> outEdges, String newLabel) {
        outEdges.forEach(e -> new GraphEdge(e.getSrcNode(), this, newLabel).establish());
    }


    public List<GraphEdge> removeOutEdges(List<String> allowPatterns, List<String> permitPatterns) {
        List<GraphEdge> res = getOutEdges(allowPatterns, permitPatterns);
        res.forEach(e -> e.remove());
        return res;
    }

    public List<GraphEdge> removeInEdges(List<String> allowPatterns, List<String> permitPatterns) {
        List<GraphEdge> res = getInEdges(allowPatterns, permitPatterns);
        res.forEach(e -> e.remove());
        return res;
    }

    public List<GraphEdge> removeOutEdges(GraphNode trgNode) {
        List<GraphEdge> res = getOutEdges(trgNode);
        res.forEach(e -> e.remove());
        return res;
    }

    public List<GraphEdge> removeInEdges(GraphNode srcNode) {
        List<GraphEdge> res = getInEdges(srcNode);
        res.forEach(e -> e.remove());
        return res;
    }

    public List<GraphEdge> removeOutEdges(GraphNode trgNode, String label) {
        List<GraphEdge> res = getOutEdges(trgNode, label);
        res.forEach(e -> e.remove());
        return res;
    }

    public List<GraphEdge> removeInEdges(GraphNode srcNode, String label) {
        List<GraphEdge> res = getInEdges(srcNode, label);
        res.forEach(e -> e.remove());
        return res;
    }



    protected HashMap<String, GraphNode> getSubNodesMap() {
        HashMap<String, GraphNode> nodes = new HashMap<>();
        nodes.put(id, this);
        for (GraphEdge outEdge : outEdges) {
            nodes.putAll(outEdge.getTrgNode().getSubNodesMap());
        }
        return nodes;
    }

    public List<GraphNode> getSubNodes() {
        return new ArrayList<>(getSubNodesMap().values());
    }

    public GraphNode getSubNode(String id) {
        return getSubNodesMap().get(id);
    }

    public List<GraphEdge> getSubEdges() {
        List<GraphEdge> edges = new ArrayList<>();
        for (GraphNode node : getSubNodesMap().values()) {
            edges.addAll(node.outEdges);
        }
        return edges;
    }

    public List<GraphEdge> getSubEdges(GraphNode srcNode, GraphNode trgNode, String label) {
        return getSubEdges().stream().filter(e -> e.getSrcNode().equals(srcNode) && e.getTrgNode().equals(trgNode) && e.getLabel().equals(label)).collect(Collectors.toList());
    }



    public String getId() {
        return id;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getWord() {
        return word;
    }

    public void setWordIndex(int wordIndex) {
        this.wordIndex = wordIndex;
    }

    public int getWordIndex() {
        return wordIndex;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public List<GraphEdge> getOutEdges() {
        return outEdges;
    }

    public List<GraphEdge> getInEdges() {
        return inEdges;
    }

    public String pprint(String indent) {
        StringBuilder strb = new StringBuilder("");

        String nodeRepr = "[" + word + "] [" + tag + "] (" + wordIndex + ") (" + id + ")";

        strb.append(indent + nodeRepr + "\n");
        String indent2 = "";
        for (int i = 0; i < indent.length(); i++) {
            indent2 += " ";
        }
        List<GraphEdge> sortedOutEdges = new ArrayList<>(outEdges);
        sortedOutEdges.sort(GraphEdge.EDGE_COMPARATOR);
        for (GraphEdge edge : sortedOutEdges) {
            strb.append(edge.getTrgNode().pprint(indent2 + "|---" + edge.getLabel() + "---> "));
        }
        return strb.toString();
    }

    @Override
    public String toString() {
        return "GraphNode{" +
                "id='" + id + '\'' +
                ", word='" + word + '\'' +
                ", wordIndex=" + wordIndex +
                ", tag='" + tag + '\'' +
                '}';
    }
}
