package ranking;

import ranking.Ranking.QueryInfo;
import ranking.Ranking.SearchCandidate;
import ranking.WordStatsCalculator.WordStat;

import java.util.HashMap;
import java.util.Map;

public class ScoresCalculator {
    private final Preprocessor preprocessor;

    public ScoresCalculator(Preprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    public Map<String, Double> calculateScores(SearchCandidate candidate, QueryInfo queryInfo) {
        Map<String, Double> scores = new HashMap<>();
        scores.put("lucene", candidate.getLuceneScore());
        scores.put("cos_p", cosScore(queryInfo.getStats(), candidate.getPlotStats()));
        scores.put("cos_pp", cosScore(queryInfo.getStats(), candidate.getProcessedPlotsStats()));
        scores.put("tf-idf_p", TFIDF(queryInfo.getStats(), candidate.getPlotStats(), preprocessor.plotsStat));
        scores.put("tf-idf_pp", TFIDF(queryInfo.getStats(), candidate.getProcessedPlotsStats(), preprocessor.processedPlotsStat));
        return scores;
    }

    private double cosScore(Map<String, WordStat> query,
                            Map<String, WordStat> document) {
        double score = 0.0, sumQuery = 0.0, sumDocument = 0.0;
        for (String word : query.keySet()) {
            if (!document.containsKey(word)) continue;
            double queryCount = query.get(word).getCf();
            double documentCount = document.get(word).getCf();
            score += queryCount * documentCount;
            sumQuery += queryCount * queryCount;
            sumDocument += documentCount * documentCount;
        }
        return score / Math.sqrt(sumQuery) / Math.sqrt(sumDocument);
    }

    private double TFIDF(Map<String, WordStat> query,
                         Map<String, WordStat> document,
                         Map<String, WordStat> collection) {
        double N = 115150; // FIX THIS
        double score = 0.0, sumQuery = 0.0, sumDocument = 0.0;
        for (String word : query.keySet()) {
            if (!document.containsKey(word)) continue;
            if (!collection.containsKey(word)) continue;
            double queryCount = query.get(word).getCf();
            double documentCount = document.get(word).getCf();
            double df = collection.get(word).getDf();
            //double idf = Math.log(N / df);
            double idf = Math.max(0.0, Math.log((N - df) / df));
            queryCount *= idf;
            documentCount *= idf;
            score += queryCount * documentCount;
            sumQuery += queryCount * queryCount;
            sumDocument += documentCount * documentCount;
        }
        return score / Math.sqrt(sumQuery) / Math.sqrt(sumDocument);
    }
}
