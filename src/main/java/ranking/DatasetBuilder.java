package ranking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import ranking.Ranking.QueryInfo;
import ranking.Ranking.SearchCandidate;
import search.Searcher;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ranking.DatasetEntry.*;

public class DatasetBuilder {
    private static final String queriesPath = "/home/ir/src/stackexchange/queries.json";
    public static final String resultsPath = "/home/ir/src/stackexchange/results_v2.json";

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
            final List<DatasetEntry> allResults = new ArrayList<>();
            final AtomicInteger counter = new AtomicInteger();
            Arrays.stream(queries).parallel().forEach(query -> {
                try {
                    final List<HitResult> results = datasetBuilder.search(query.getTitle());
                    final DatasetEntry result = new DatasetEntry(query, results);
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
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .serializeSpecialFloatingPointValues()
                    .create();
            try (FileWriter writer = new FileWriter(resultsPath)) {
                writer.write(gson.toJson(allResults.toArray(new DatasetEntry[0])));
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
        scoresCalculator.calculateComplexScores(candidates, queryInfo);

        return candidates.stream()
                .map(c -> new HitResult(c.getMovieId(), c.getScores()))
                .collect(Collectors.toList());
    }

}
