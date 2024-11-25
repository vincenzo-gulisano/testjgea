package grammar;

import grammar.utils.CSVTypesExtractor;
import grammar.utils.GrammarBuilder;
import grammar.utils.GrammarFileWriter;
import grammar.types.DataTypesEnum;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GrammarGenerator {

    public static void generateGrammar(String csvFilePath, String grammarFilePath) throws IOException {
        Map<String, DataTypesEnum> columnTypes = CSVTypesExtractor.getColumnTypesFromCSV(csvFilePath);
        List<DataTypesEnum> uniqueColumnTypes = columnTypes.values().stream().toList();
        Map<String, Set<String>> uniqueStringValues = CSVTypesExtractor.inferUniqueStringValues(csvFilePath, columnTypes);

        String grammar = GrammarBuilder.buildGrammar(columnTypes, uniqueStringValues, uniqueColumnTypes);
        GrammarFileWriter.writeToFile(grammar, grammarFilePath);
    }
}
