package ranking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import index.IndexBuilder;
import lombok.Data;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatasetBuilder {
    private static final String queriesPath = "/home/ir/src/stackexchange/queries.json";
    private static final String resultsPath = "/home/ir/src/stackexchange/results.json";
    private static final int NUM_RESULTS = 300;

    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private final Directory index = FSDirectory.open(Paths.get(IndexBuilder.INDEX_DIRECTORY));
    private final IndexReader reader = DirectoryReader.open(index);
    private final IndexSearcher searcher = new IndexSearcher(reader);

    public DatasetBuilder() throws IOException {
    }

    public static void build() {
        try {
            final DatasetBuilder datasetBuilder = new DatasetBuilder();
            final StackExchangeQuery[] queries =
                    new Gson().fromJson(new FileReader(queriesPath), StackExchangeQuery[].class);
            List<SearchResult> allResults = new ArrayList<>();
            for (StackExchangeQuery query : queries) {
                try {
                    List<HitResult> results = datasetBuilder.search(query.title);
                    HitResult[] hitResults = results.toArray(new HitResult[0]);
                    SearchResult result = new SearchResult(query, hitResults);
                    allResults.add(result);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(resultsPath)) {
                writer.write(gson.toJson(allResults.toArray(new SearchResult[0])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<HitResult> search(String line) throws ParseException, IOException {
        List<HitResult> results = new ArrayList<>();
        doSearch("plot", line, results);
        doSearch("reviews", line, results);
        doSearch("preprocessed_plot", line, results);
        return results;
    }

    private void doSearch(String field, String line, List<HitResult> out) throws ParseException, IOException {
        Query query = new QueryParser(field, analyzer).parse(QueryParser.escape(line));
        TopDocs docs = searcher.search(query, NUM_RESULTS);
        for (ScoreDoc hit : docs.scoreDocs) {
            Document doc = searcher.doc(hit.doc);
            out.add(new HitResult(doc.get("movie-id"), hit.score));
        }
    }

    @Data
    private static class HitResult {
        private final String id;
        private final double score;
    }

    @Data
    private static class StackExchangeQuery {
        private final String post_id;
        private final String[] imdb;
        private final String title;
        private final String body;
    }

    @Data
    private static class SearchResult {
        private final StackExchangeQuery query;
        private final HitResult[] results;
    }
}
