package search;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DbConnection implements AutoCloseable {
    private static final String DATABASE = "jdbc:postgresql://localhost:5432/imdbcrawl";
    private static final String movieQuery = "SELECT * FROM movie WHERE imdb_id = ?";
    private static final String searchQuery = "SELECT * FROM search WHERE movie_id = ?";
    private static final String reviewsCountQuery = "SELECT cnt FROM review_counts WHERE movie_id = ?";
    //private static final String allSearch = "SELECT * FROM search";

    public final Connection connection;
    public final PreparedStatement movieRequestStatement;
    public final PreparedStatement searchRequestStatement;
    public final PreparedStatement reviewsCountStatement;
    //public final PreparedStatement searchAllStatement;

    public DbConnection() throws SQLException {
        connection = DriverManager.getConnection(DATABASE, "ir", "");
        movieRequestStatement = connection.prepareStatement(movieQuery);
        searchRequestStatement = connection.prepareStatement(searchQuery);
        reviewsCountStatement = connection.prepareStatement(reviewsCountQuery);
        //searchAllStatement = connection.prepareStatement(allSearch);
    }

    @Override
    public void close() throws SQLException {
        movieRequestStatement.close();
        searchRequestStatement.close();
        reviewsCountStatement.close();
        connection.close();
    }
}
