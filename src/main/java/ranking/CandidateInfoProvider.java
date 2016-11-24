package ranking;

import lombok.Data;
import ranking.Ranking.SearchCandidate;
import search.DbConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

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
        final MovieInfo movieInfo = getMovie(movieId);
        final SearchInfo searchInfo = getSearch(movieId);

        return new SearchCandidate(
                movieId,
                movieInfo.getTitle(),
                movieInfo.getDate(),
                movieInfo.getPoster_url(),
                searchInfo.getPlot(),
                searchInfo.getReviews(),
                searchInfo.getPreprocessed_plot(),
                luceneScore,
                movieInfo.getReviewsCount()
        );
    }

    private MovieInfo getMovie(String movieId) throws SQLException {
        if (movieCache.containsKey(movieId)) {
            return movieCache.get(movieId);
        }
        conn.movieRequestStatement.setString(1, movieId);
        final ResultSet movieResults = conn.movieRequestStatement.executeQuery();
        if (!movieResults.next()) {
            System.err.println("movieInfo not found!");
        }
        conn.reviewsCountStatement.setString(1, movieId);
        final ResultSet reviewCntResults = conn.reviewsCountStatement.executeQuery();
        if (!reviewCntResults.next()) {
            System.err.println("review counts not found!");
        }
        final MovieInfo movieInfo = new MovieInfo(movieResults, reviewCntResults);
        movieCache.put(movieId, movieInfo);
        return movieInfo;
    }

    private SearchInfo getSearch(String movieId) throws SQLException {
        if (searchCache.containsKey(movieId)) {
            return searchCache.get(movieId);
        }
        conn.searchRequestStatement.setString(1, movieId);
        final ResultSet searchResults = conn.searchRequestStatement.executeQuery();
        if (!searchResults.next()) {
            System.err.println("searchInfo not found!");
        }
        final SearchInfo searchInfo = new SearchInfo(searchResults);
        searchCache.put(movieId, searchInfo);
        return searchInfo;
    }

    private static final HashMap<String, MovieInfo> movieCache = new HashMap<>();
    private static final HashMap<String, SearchInfo> searchCache = new HashMap<>();

    @Data
    private static class MovieInfo {
        private final String imdb_id;
        private final String title;
        private final String plot;
        private final String poster_url;
        private final String actors;
        private final String date;
        private final int reviewsCount;

        public MovieInfo(ResultSet movieResults, ResultSet reviewsResult) throws SQLException {
            this.imdb_id = movieResults.getString("imdb_id");
            this.title = movieResults.getString("title");
            this.plot = movieResults.getString("plot");
            this.poster_url = movieResults.getString("poster_url");
            this.actors = movieResults.getString("actors");
            this.date = movieResults.getString("date");
            this.reviewsCount = reviewsResult.getInt("cnt");
        }
    }

    @Data
    private static class SearchInfo {
        private final String movie_id;
        private final String plot;
        private final String reviews;
        private final String preprocessed_plot;
        private final String preprocessed_reviews;

        public SearchInfo(ResultSet resultSet) throws SQLException {
            this.movie_id = resultSet.getString("movie_id");
            this.plot = resultSet.getString("plot");
            this.reviews = resultSet.getString("reviews");
            this.preprocessed_plot = resultSet.getString("preprocessed_plot");
            this.preprocessed_reviews = resultSet.getString("preprocessed_reviews");
        }
    }
}
