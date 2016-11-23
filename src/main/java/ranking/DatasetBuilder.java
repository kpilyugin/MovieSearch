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
import ranking.Ranking.QueryInfo;
import ranking.Ranking.SearchCandidate;
import search.Searcher;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DatasetBuilder {
    private static final String queriesPath = "/home/ir/src/stackexchange/queries.json";
    private static final String resultsPath = "/home/ir/src/stackexchange/results_v2.json";

    private final Searcher searcher = new Searcher();
    private final Preprocessor preprocessor = new Preprocessor();
    private final ScoresCalculator scoresCalculator = new ScoresCalculator(preprocessor);

    public DatasetBuilder() throws IOException {
    }

    public static void build() {
        try {
            final DatasetBuilder datasetBuilder = new DatasetBuilder();
            final StackExchangeQuery[] queries =
                    new Gson().fromJson(new FileReader(queriesPath), StackExchangeQuery[].class);
            final List<SearchResult> allResults = new ArrayList<>();
            final AtomicInteger counter = new AtomicInteger();
            Arrays.stream(queries).parallel().forEach(query -> {
                try {
                    final List<HitResult> results = datasetBuilder.search(query.title);
                    final SearchResult result = new SearchResult(query, results);
                    synchronized (allResults) {
                        allResults.add(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (counter.incrementAndGet() % 5 == 0) {
                    System.out.println("processed: " + counter.get());
                }
            });
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(resultsPath)) {
                writer.write(gson.toJson(allResults.toArray(new SearchResult[0])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<HitResult> search(String query) throws ParseException, IOException, InvalidTokenOffsetsException, SQLException {
        List<SearchCandidate> candidates = searcher.getCandidates(query).getCandidates();

        QueryInfo queryInfo = new QueryInfo(query);
        preprocessor.preprocessQuery(queryInfo);

        candidates.parallelStream().forEach(candidate -> {
            preprocessor.preprocessCandidate(candidate);
            candidate.setScores(scoresCalculator.calculateScores(candidate, queryInfo));
        });

        return candidates.stream()
                .map(c -> new HitResult(c.getMovieId(), c.getScores()))
                .collect(Collectors.toList());
    }

    @Data
    private static class HitResult {
        private final String id;
        private final Map<String, Double> scores;
    }

    @Data
    private static class StackExchangeQuery {
        private final String post_id;
        private final List<String> imdb;
        private final String title;
        private final String body;
    }

    @Data
    private static class SearchResult {
        private final StackExchangeQuery query;
        private final List<HitResult> results;
    }
}
