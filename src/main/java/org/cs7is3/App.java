package org.cs7is3;

// TODO: Implement your main application class
// This class should handle command-line arguments and coordinate between Indexer and Searcher
// 
// Required command-line interface:
//   java -jar your-jar.jar index --docs "Assignment Two" --index index
//   java -jar your-jar.jar search --index index --topics topics --output runs/student.run --numDocs 1000
//
// The GitHub Actions workflow expects:
// 1. Maven build to succeed (mvn clean package)
// 2. Index command to create an index from the dataset
// 3. Search command to produce a TREC-format run file with exactly 1000 results per topic
// 4. Output format: "topic_id Q0 docno rank score run_tag"

public class App {
    public static String DATA_PATH = "data";
    public static String INDEX_PATH = "index";

    public static void main(String[] args) {
        try {
            System.out.println("Starting indexing...");
            TrialIndexer.createIndex(DATA_PATH, INDEX_PATH);
            System.out.println("Index created successfully at: " + INDEX_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }}
}
