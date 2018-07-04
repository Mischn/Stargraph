package net.stargraph.core.tools.SimpleIE.graph;

import java.util.Comparator;

public class GraphEdge {
    public static Comparator<GraphEdge> EDGE_COMPARATOR = new Comparator<GraphEdge>() {
        @Override
        public int compare(GraphEdge e1, GraphEdge e2) {
            return e1.getTrgNode().wordIndex - e2.getTrgNode().wordIndex;
        }
    };

    private GraphNode srcNode;
    private GraphNode trgNode;
    private String label;

    public GraphEdge(GraphNode srcNode, GraphNode trgNode, String label) {
        this.srcNode = srcNode;
        this.trgNode = trgNode;
        this.label = label;
    }

    public void establish() {
        if (!srcNode.outEdges.contains(this)) {
            srcNode.outEdges.add(this);
        }
        if (!trgNode.inEdges.contains(this)) {
            trgNode.inEdges.add(this);
        }
    }

    public void remove() {
        if (srcNode.outEdges.contains(this)) {
            srcNode.outEdges.remove(this);
        }
        if (trgNode.inEdges.contains(this)) {
            trgNode.inEdges.remove(this);
        }
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public GraphNode getSrcNode() {
        return srcNode;
    }

    public GraphNode getTrgNode() {
        return trgNode;
    }

    @Override
    public String toString() {
        return "GraphEdge{" +
                "srcNode=" + srcNode.id +
                ", trgNode=" + trgNode.id +
                ", label='" + label + '\'' +
                '}';
    }
}
