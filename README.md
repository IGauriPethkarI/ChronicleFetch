# CS7IS3 BM25ers Search Engine

A highly optimized information retrieval system built using **Apache Lucene 10.x**, featuring custom text analysis, Pseudo-Relevance Feedback (PRF), and sophisticated narrative parsing.


---

## 🚀 Key Features

### 🔍 Advanced Retrieval & Ranking
- **BM25 Similarity**: Custom-tuned $k_1$ and $b$ parameters for optimal performance on journalistic datasets.
- **Pseudo-Relevance Feedback (PRF)**: Dynamically expands queries using the most significant terms from top-ranked documents.
- **Multi-Field Weighting**: Applies strategic boosts to `headline` (1.8x) and `summary` (1.8x) compared to main `text`.

### 🛡️ Robust Indexing
- **Duplicate Detection**: Content-based hashing to prevent redundant indexing of documents.
- **Date Normalization**: Standardizes multi-formatted dates (e.g., "Feb 28 1994", "19940215") into a consistent searchable format.
- **Custom Analyzer**:
  - **Stopword Removal**: News-specific stopword list filtering common journalistic filler.
  - **Stemming**: PorterStemFilter for term normalization.
  - **Token Filtering**: Minimum (2) and maximum (15) token length constraints.

### 📝 Smart Query Construction
- **Narrative Extraction**: Intelligent parsing of TREC topics to extract positive signals while ignoring "irrelevant" or "negative" constraints.
- **Weighted Multi-Part Queries**: Combines Title (2.0x boost), Description (1.0x boost), and Narrative (0.5x boost) into a comprehensive search objective.

---

## 📂 Project Structure

```text
├── dataset/             # Document dataset (FBIS, FR94, FT, LATIMES)
├── index/               # Generated Lucene index
├── runs/                # Search output (TREC format)
├── src/main/java/       # Core implementation
│   ├── analyzer/        # Custom tokenization logic
│   ├── parsers/         # XML-style dataset parsers
│   ├── App.java         # CLI Entry point
│   ├── Indexer.java     # Document processing engine
│   └── Searcher.java    # Query logic & PRF expansion
├── tools/               # Evaluation utilities
├── qrels                # Ground truth for evaluation
├── pom.xml              # Maven configuration (Java 21)
└── README.md            # Project documentation
```

---

## 🛠️ Usage Instructions

### 1. Requirements
- **Java 21**
- **Maven 3.x**

### 2. Build the Project
```bash
mvn clean package
```

### 3. Execution Commands

#### **Option A: Run Full Pipeline (Index + Search)**
Recommended for a fresh start.
```bash
java -jar target/cs7is3-search-1.0.0.jar both --docs dataset --index index --topics topics --output runs/results.run
```

#### **Option B: Step-by-Step**
**Index Documents:**
```bash
java -jar target/cs7is3-search-1.0.0.jar index --docs dataset --index index
```

**Search Topics:**
```bash
java -jar target/cs7is3-search-1.0.0.jar search --index index --topics topics --output runs/results.run --numDocs 1000
```

### 4. Evaluation
Evaluate performance using the provided Python tool:
```bash
python tools/evaluate.py qrels runs/results.run
```

---

## 📊 Evaluation Results
The system is built to maximize:
- **MAP** (Mean Average Precision)
- **P@5** and **P@20**
- **nDCG@20**

Performance is tracked via GitHub Actions on every push to ensure regression testing and quality control.
