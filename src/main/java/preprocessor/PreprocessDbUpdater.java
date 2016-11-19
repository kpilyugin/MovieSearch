package preprocessor;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PreprocessDbUpdater implements AutoCloseable {
    private static final String DATABASE = "jdbc:postgresql://localhost:5432/imdbcrawl";
    //private static final String selectQuery =
    //        "SELECT * FROM search WHERE COALESCE(plot, '') != '' AND COALESCE(preprocessed_plot, '') = ''";
    private static final String selectQuery =
            "SELECT * FROM search WHERE COALESCE(reviews, '') != '' AND COALESCE(preprocessed_reviews, '') = ''";
    private final Preprocessor preprocessor;
    private final Connection selectConnection;
    private ResultSet movie;
    private final LinkedBlockingQueue<SearchEntry> queue;
    private volatile boolean finished;

    public PreprocessDbUpdater() throws IOException, SQLException {
        preprocessor = new Preprocessor();

        selectConnection = connect();
        selectConnection.setAutoCommit(false);
        finished = false;

        queue = new LinkedBlockingQueue<>(50);
    }

    public void run() throws SQLException {
        while (!finished) {
            try (Statement statement = selectConnection.createStatement()) {
                statement.setFetchSize(50);
                movie = statement.executeQuery(selectQuery);

                final List<Thread> threads = new ArrayList<>();
                for (int i = 0; i < 4; ++i) {
                    final Thread thread = new Thread(new UpdateThread());
                    thread.start();
                    threads.add(thread);
                }

                System.out.println("started!");

                while (!finished) {
                    if (!movie.next()) {
                        finished = true;
                        break;
                    }
                    SearchEntry entry = new SearchEntry(
                            movie.getString("movie_id"),
                            movie.getString("plot"),
                            movie.getString("reviews"));
                    queue.put(entry);
                }

                for (Thread thread : threads) {
                    thread.join();
                }
            } catch (InterruptedException e) {
                System.err.println("interrupted: " + e.getMessage());
                e.printStackTrace(System.err);
            }

            if (!finished) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public void close() throws SQLException {
        selectConnection.close();
    }

    public static void start() throws SQLException, IOException {
        try (PreprocessDbUpdater updater = new PreprocessDbUpdater()) {
            updater.run();
        }
    }

    private SearchEntry getNext() throws InterruptedException {
        while (!finished) {
            SearchEntry entry = queue.poll(1, TimeUnit.SECONDS);
            if (entry != null)
                return entry;
        }
        return null;
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DATABASE, "ir", "");
    }

    private class UpdateThread implements Runnable {
        //private static final String updateStatement =
        //        "UPDATE search SET preprocessed_plot = ?, preprocessed_reviews = ? WHERE movie_id = ?";
        //private static final String updateStatement =
        //        "UPDATE search SET preprocessed_plot = ? WHERE movie_id = ?";
        private static final String updateStatement =
                "UPDATE search SET preprocessed_reviews = ? WHERE movie_id = ?";

        @Override
        public void run() {
            while (!finished) {
                try (Connection connection = connect()) {
                    connection.setAutoCommit(true);
                    try (PreparedStatement statement = connection.prepareStatement(updateStatement)) {
                        while (!finished) {
                            final SearchEntry entry = getNext();
                            if (entry == null)
                                break;
                            process(entry);
                            //statement.setString(1, entry.getPreprocessed_plot());
                            statement.setString(1, entry.getPreprocessed_reviews());
                            statement.setString(2, entry.getMovie_id());
                            statement.execute();
                        }
                    }
                } catch (SQLException | InterruptedException e) {
                    System.err.println("error: " + e.getMessage());
                    e.printStackTrace(System.err);
                }

                if (!finished) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }

        private void process(SearchEntry entry) {
            //entry.setPreprocessed_plot(preprocessor.process(entry.getPlot()));
            String reviews = entry.getReviews();
            if (reviews.length() > 5000)
                reviews = reviews.substring(0, 5000);
            entry.setPreprocessed_reviews(preprocessor.process(reviews));
        }
    }
}
