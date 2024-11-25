package representation.mappers;

import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import representation.PatternRepresentation;
import representation.mappers.treerepresentationparsers.ClauseParser;
import representation.mappers.treerepresentationparsers.EventParser;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;

public class TreeToRepresentationMapper implements Function<Tree<String>, PatternRepresentation> {

    @Override
    public PatternRepresentation apply(Tree<String> tree) {
        List<PatternRepresentation.Event> events = new ArrayList<>();
        PatternRepresentation.WithinClause withinClause = null;
        PatternRepresentation.KeyByClause keyByClause = null;

        for (Tree<String> child : tree) {
            if ("<events>".equals(child.content())) {
                events = EventParser.parseEvents(child);
            } else if ("<withinClause>".equals(child.content())) {
                withinClause = ClauseParser.parseWithinClause(child);
            } else if ("<key_by>".equals(child.content())) {
                keyByClause = ClauseParser.parseKeyByClause(child);
            }
        }

        PatternRepresentation patternRepresentation = new PatternRepresentation(events, withinClause, keyByClause);
        validatePatternRepresentation(tree, patternRepresentation);
        return patternRepresentation;
    }

    private void validatePatternRepresentation(Tree<String> tree, PatternRepresentation patternRepresentation) {
        System.out.println("Validating Pattern Representation...");
        if (patternRepresentation.events().size() != countEventNodes(tree)) {
            System.err.println("Mismatch in number of events between Tree and PatternRepresentation.");
        }
    }

    private int countEventNodes(Tree<String> tree) {
        int count = 0;
        if ("<event>".equals(tree.content())) {
            count++;
        }
        for (Tree<String> child : tree) {
            count += countEventNodes(child);
        }
        return count;
    }
}
