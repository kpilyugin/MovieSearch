package ranking;

import ranking.Ranking.SearchCandidate;
import search.DbConnection;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CandidateInfoProvider implements AutoCloseable {
    private final DbConnection conn;

    public CandidateInfoProvider() throws SQLException {
        conn = new DbConnection();
    }

    @Override
    public void close() throws SQLException {
        conn.close();
    }

    public SearchCandidate getInfo(String movieId, double luceneScore) throws SQLException {
        conn.movieRequestStatement.setString(1, movieId);
        final ResultSet movieInfo = conn.movieRequestStatement.executeQuery();
        if (!movieInfo.next()) {
            System.err.println("movieInfo not found!");
        }

        conn.searchRequestStatement.setString(1, movieId);
        final ResultSet searchInfo = conn.searchRequestStatement.executeQuery();
        if (!searchInfo.next()) {
            System.err.println("searchInfo not found!");
        }

        return new SearchCandidate(
                movieId,
                movieInfo.getString("title"),
                movieInfo.getString("date"),
                movieInfo.getString("poster_url"),
                searchInfo.getString("plot"),
                searchInfo.getString("reviews"),
                searchInfo.getString("preprocessed_plot"),
                luceneScore
        );
    }
}