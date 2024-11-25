package grammar.utils;

import grammar.types.DataTypesEnum;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CSVTypesExtractor {

    public static Map<String, DataTypesEnum> getColumnTypesFromCSV(String csvFilePath) throws IOException {
        Map<String, DataTypesEnum> columnTypes = new HashMap<>();
        try (Reader reader = new FileReader(csvFilePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : csvParser) {
                for (String column : record.toMap().keySet()) {
                    if (column.equalsIgnoreCase("timestamp")) {
                        continue;
                    }
                    String value = record.get(column);
                    DataTypesEnum currentType = inferType(value);
                    columnTypes.merge(column, currentType, (existingType, newType) -> {
                        if (existingType == DataTypesEnum.INT && newType == DataTypesEnum.FLOAT) {
                            return DataTypesEnum.FLOAT;
                        }
                        return existingType == newType ? existingType : DataTypesEnum.STRING;
                    });
                }
            }
        }
        return columnTypes;
    }

    public static Map<String, Set<String>> inferUniqueStringValues(String csvFilePath, Map<String, DataTypesEnum> columnTypes) throws IOException {
        Map<String, Set<String>> uniqueStringValues = new HashMap<>();
        try (Reader reader = new FileReader(csvFilePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : csvParser) {
                for (String column : record.toMap().keySet()) {
                    String value = record.get(column);
                    if (columnTypes.get(column) == DataTypesEnum.STRING) {
                        uniqueStringValues.computeIfAbsent(column, k -> new HashSet<>()).add(value);
                    }
                }
            }
        }
        return uniqueStringValues;
    }

    private static DataTypesEnum inferType(String value) {
        if (value.matches("-?\\d+")) return DataTypesEnum.INT;
        else if (value.matches("-?\\d*\\.\\d+([eE][-+]?\\d+)?")) return DataTypesEnum.FLOAT;
        else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return DataTypesEnum.BOOLEAN;
        else return DataTypesEnum.STRING;
    }
}
