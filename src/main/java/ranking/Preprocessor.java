package ranking;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ranking.Ranking.QueryInfo;
import ranking.Ranking.SearchCandidate;
import ranking.WordStatsCalculator.WordStat;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import static ranking.WordStatsBuilder.plotStatsFile;
import static ranking.WordStatsBuilder.pplotStatsFile;

public class Preprocessor {
    private final WordStatsCalculator statsCalculator;
    public final Map<String, WordStat> plotsStat;
    //public final Map<String, WordStat> reviewsStat;
    public final Map<String, WordStat> processedPlotsStat;

    public Preprocessor() throws IOException {
        statsCalculator = new WordStatsCalculator();
        plotsStat = loadStats(plotStatsFile);
        //reviewsStat = loadStats(reviewsStatsFile);
        processedPlotsStat = loadStats(pplotStatsFile);
    }

    private Map<String, WordStat> loadStats(String path) throws IOException {
        try (FileReader reader = new FileReader(path)) {
            Type type = new TypeToken<Map<String, WordStat>>() {}.getType();
            return new Gson().fromJson(reader, type);
        }
    }

    public void preprocessCandidate(SearchCandidate candidate) {
        statsCalculator.process(candidate.getPlotStats(), candidate.getPlot());
        //statsCalculator.process(candidate.getReviewsStats(), candidate.getReviews());
        statsCalculator.process(candidate.getProcessedPlotsStats(), candidate.getPreprocessed_plot());
    }

    public void preprocessQuery(QueryInfo queryInfo) {
        statsCalculator.process(queryInfo.getStats(), queryInfo.getQuery());
    }
}
