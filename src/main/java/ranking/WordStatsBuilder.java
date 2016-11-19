package ranking;

import com.google.gson.Gson;
import lombok.Data;
import ranking.WordStatsCalculator.WordStat;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashMap;

public class WordStatsBuilder implements AutoCloseable {
    private static final String statsPath = "/home/ir/src/stackexchange/stats_";
    public static final String plotStatsFile = statsPath + "plot.json";
    public static final String reviewsStatsFile = statsPath + "reviews.json";
    public static final String pplotStatsFile = statsPath + "pplot.json";

    private static final String DATABASE = "jdbc:postgresql://localhost:5432/imdbcrawl";
    private static final String selectQuery = "SELECT * FROM search";

    private final HashMap<String, WordStat> plotsStat;
    //private final HashMap<String, WordStat> reviewsStat;
    private final HashMap<String, WordStat> processedPlotsStat;
    private final WordStatsCalculator statsCalculator = new WordStatsCalculator();
    private final Connection selectConnection;

    public WordStatsBuilder() throws IOException, SQLException {
        plotsStat = new HashMap<>();
        //reviewsStat = new HashMap<>();
        processedPlotsStat = new HashMap<>();

        selectConnection = connect();
        selectConnection.setAutoCommit(false);
    }

    public void run() throws SQLException, IOException {
        try (Statement statement = selectConnection.createStatement()) {
            statement.setFetchSize(100);
            ResultSet resultSet = statement.executeQuery(selectQuery);

            int counter = 0;
            while (resultSet.next()) {
                statsCalculator.process(plotsStat, resultSet.getString("plot"));
                //statsCalculator.process(reviewsStat, resultSet.getString("reviews"));
                statsCalculator.process(processedPlotsStat, resultSet.getString("preprocessed_plot"));

                if (counter++ % 5000 == 0) System.out.println("processed: " + counter);
            }

            try (PrintWriter writer = new PrintWriter(plotStatsFile)) {
                writer.write(new Gson().toJson(plotsStat));
            }
            //try (PrintWriter writer = new PrintWriter(reviewsStatsFile)) {
            //    writer.write(new Gson().toJson(reviewsStat));
            //}
            try (PrintWriter writer = new PrintWriter(pplotStatsFile)) {
                writer.write(new Gson().toJson(processedPlotsStat));
            }
        }
    }

    public void close() throws SQLException {
        selectConnection.close();
    }

    public static void build() throws SQLException, IOException {
        try (WordStatsBuilder builder = new WordStatsBuilder()) {
            builder.run();
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DATABASE, "ir", "");
    }
}
