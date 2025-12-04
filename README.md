# CS7IS3 Search Engine

## Requirements
- Java 21
- Maven 3.x

## Steps to Run

1. **Clean existing index** (if any):
   ```bash
   rm -rf index/
   ```

2. **Build the project**:
   ```bash
   mvn clean package
   ```

3. **Index the documents**:
   ```bash
   java -jar target/cs7is3-search-1.0.0.jar index --docs "Assignment Two" --index index
   ```

4. **Search topics**:
   ```bash
   java -jar target/cs7is3-search-1.0.0.jar search --index index --topics topics --output runs/student.run --numDocs 1000
   ```

5. **Evaluate results**:
   ```bash
   python3 tools/evaluate.py qrels.assignment2.part1 runs/student.run
   ```

## Alternative: Run Both (Index + Search)
```bash
java -jar target/cs7is3-search-1.0.0.jar both --docs "Assignment Two" --index index --topics topics --output runs/student.run --numDocs 1000
```

## 📁 File Structure

```
├── .github/workflows/
│   └── evaluation.yml                  # GitHub Actions evaluation workflow
├── .idea/                              # IntelliJ IDEA configuration
├── Assignment Two/                     # Document dataset
│   ├── dtds/                           # Document type definitions
│   ├── fbis/                           # Foreign Broadcast Information Service
│   ├── fr94/                           # Federal Register 1994
│   ├── ft/                             # Financial Times
│   ├── latimes/                        # Los Angeles Times
│   └── ReadMe.txt                      # Dataset documentation
├── index/                              # Generated Lucene index
├── runs/                               # Search results output
│   └── student.run                     # TREC format results
├── src/main/java/org/cs7is3/
│   ├── analyzer/
│   │   ├── CustomAnalyzer.java         # Custom text analyzer
│   │   └── TopIndexTerms.java          # Index term analysis utility
│   ├── constants/
│   │   └── Constants.java              # Application constants
│   ├── parsers/
│   │   ├── FBISParser.java             # FBIS document parser
│   │   ├── FRParsers.java              # Federal Register parser
│   │   ├── FTParser.java               # Financial Times parser
│   │   └── LATParser.java              # LA Times parser
│   ├── App.java                        # Main application
│   ├── Indexer.java                    # Document indexer
│   └── Searcher.java                   # Topic searcher
├── target/                             # Maven build output
├── tools/
│   └── evaluate.py                     # Performance evaluation script
├── topics                              # 50 search topics
├── pom.xml                             # Maven build configuration
└── README.md                           # This documentation
```

## Output Format
Results are written in TREC format:
```
401 Q0 DOCNO1 1 0.123456 cs7is3
401 Q0 DOCNO2 2 0.098765 cs7is3
```