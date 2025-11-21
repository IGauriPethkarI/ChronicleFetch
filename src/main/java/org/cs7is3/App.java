package org.cs7is3;

import java.nio.file.Path;
import java.nio.file.Paths;

public class App {

    public static final String DEFAULT_DATA_PATH = "Assignment Two";
    public static final String DEFAULT_INDEX_PATH = "index";
    public static final String DEFAULT_TOPICS_PATH = "topics";
    public static final String DEFAULT_OUTPUT_PATH = "runs/results.run";
    public static final int DEFAULT_NUM_DOCS = 1000;

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.exit(1);
            }

            String command = args[0].toLowerCase();

            switch (command) {
                case "index":
                    handleIndexCommand(args);
                    break;
                case "search":
                    handleSearchCommand(args);
                    break;
                case "both":
                    handleIndexCommand(args);
                    handleSearchCommand(args);
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void handleIndexCommand(String[] args) throws Exception {
        String dataPath = DEFAULT_DATA_PATH;
        String indexPath = DEFAULT_INDEX_PATH;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--docs") && i + 1 < args.length) {
                dataPath = args[i + 1];
                i++;
            } else if (args[i].equals("--index") && i + 1 < args.length) {
                indexPath = args[i + 1];
                i++;
            }
        }

        long startTime = System.currentTimeMillis();

        Indexer indexer = new Indexer();
        indexer.buildIndex(Paths.get(dataPath), Paths.get(indexPath));

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime) / 1000;
    }

    private static void handleSearchCommand(String[] args) throws Exception {
        String indexPath = DEFAULT_INDEX_PATH;
        String topicsPath = DEFAULT_TOPICS_PATH;
        String outputPath = DEFAULT_OUTPUT_PATH;
        int numDocs = DEFAULT_NUM_DOCS;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--index") && i + 1 < args.length) {
                indexPath = args[i + 1];
                i++;
            } else if (args[i].equals("--topics") && i + 1 < args.length) {
                topicsPath = args[i + 1];
                i++;
            } else if (args[i].equals("--output") && i + 1 < args.length) {
                outputPath = args[i + 1];
                i++;
            } else if (args[i].equals("--numDocs") && i + 1 < args.length) {
                numDocs = Integer.parseInt(args[i + 1]);
                i++;
            }
        }

        Path outputFilePath = Paths.get(outputPath);
        if (outputFilePath.getParent() != null) {
            outputFilePath.getParent().toFile().mkdirs();
        }

        Searcher searcher = new Searcher();
        searcher.searchTopics(
                Paths.get(indexPath),
                Paths.get(topicsPath),
                Paths.get(outputPath),
                numDocs
        );

    }
}