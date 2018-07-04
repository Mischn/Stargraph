package net.stargraph.core.tools.SimpleIE;

import net.stargraph.core.tools.SimpleIE.graph.RootNode;
import org.lambda3.text.simplification.discourse.utils.parseTree.ParseTreeException;

public interface GraphParser {
    RootNode parse(String text) throws ParseTreeException;
}
