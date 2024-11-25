package representation.mappers.treerepresentationparsers;

import io.github.ericmedvet.jgea.core.representation.tree.Tree;

public class ValueParser {

    public static float parseFNum(Tree<String> fNumNode) {
        StringBuilder fNumStr = new StringBuilder();
        for (Tree<String> digitNode : fNumNode.leaves()) {
            String content = digitNode.content();
            if (content.matches("[0-9.+-E]")) {
                fNumStr.append(content);
            }
        }
        return Float.parseFloat(fNumStr.toString());
    }

    public static boolean parseBooleanValue(Tree<String> boolNode) {
        String value = boolNode.visitLeaves().get(0);
        return Boolean.parseBoolean(value);
    }

    public static String parseStringValue(Tree<String> strNode) {
        return strNode.visitLeaves().get(0);
    }
}
