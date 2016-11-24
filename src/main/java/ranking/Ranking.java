package ranking;

import lombok.Data;
import ranking.WordStatsCalculator.WordStat;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Ranking {
    private static final int RESULTS_LIMIT = 30;
    private final Preprocessor preprocessor;
    private final ScoresCombiner scoresCombiner;
    private final ScoresCalculator scoresCalculator;

    public Ranking() throws IOException {
        preprocessor = new Preprocessor();
        scoresCombiner = new ScoresCombiner();
        scoresCalculator = new ScoresCalculator(preprocessor);
    }

    public List<SearchCandidate> rerank(List<SearchCandidate> candidates, String query) {
        QueryInfo queryInfo = new QueryInfo(query);
        preprocessor.preprocessQuery(queryInfo);

        candidates.parallelStream().forEach(candidate -> {
            preprocessor.preprocessCandidate(candidate);
            candidate.setScores(scoresCalculator.calculateScores(candidate, queryInfo));
            candidate.setFinalScore(scoresCombiner.combine(candidate.scores));
        });
        scoresCalculator.calculateComplexScores(candidates, queryInfo);

        return candidates.stream()
                .sorted(Comparator.comparing(c -> -c.getFinalScore()))
                .limit(RESULTS_LIMIT)
                .collect(Collectors.toList());
    }

    public void setCoefFile(String coefFile) {
        scoresCombiner.setCoefFile(coefFile);
    }

    @Data
    public static class SearchCandidate {
        private final String movieId;

        private final String title;
        private final String date;
        private final String poster;

        private final String plot;
        private final String reviews;
        private final String preprocessed_plot;

        private final double luceneScore;

        private final int reviewCount;

        private double finalScore = 0.0;
        private Map<String, Double> scores = new HashMap<>();
        private final Map<String, WordStat> plotStats = new HashMap<>();
        private final Map<String, WordStat> reviewsStats = new HashMap<>();
        private final Map<String, WordStat> processedPlotsStats = new HashMap<>();
    }

    @Data
    public static class QueryInfo {
        private final String query;
        private final Map<String, WordStat> stats = new HashMap<>();
    }
}
