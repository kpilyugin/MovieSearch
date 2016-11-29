package ranking;

import lombok.Data;
import ranking.Ranking.SearchCandidate;
import search.DbConnection;

import java.sql.Array;
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
                movieInfo.getReleaseDate(),
                movieInfo.getPoster_url(),
                searchInfo.getPlot(),
                searchInfo.getReviews(),
                searchInfo.getPreprocessed_plot(),
                luceneScore,
                movieInfo.getReviewsCount(),
                movieInfo.getYear(),
                movieInfo.getImdbRating(),
                movieInfo.getType(),
                movieInfo.getGenres()
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
        int reviews = 0;
        if (reviewCntResults.next()) {
            reviews = reviewCntResults.getInt("cnt");
        }
        final MovieInfo movieInfo = new MovieInfo(movieResults, reviews);
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
        /**
         * Not just actors, but directors and other guys and girls...
         */
        private final String[] credits;
        /**
         * That is usa release releaseDate, if I've got it right,
         * so if u want 'real' year of the movie look at year field
         */
        private final String releaseDate;
        private final int reviewsCount;
        private final String[] genres;
        private final double imdbRating;
        /**
         * number of votes for the movie
         */
        private final int voteCnt;
        /**
         * Year is sometimes filled even though {@code releaseDate} field is empty
         * (that is how imdb api works), so release releaseDate may be unknown, but
         * movie year is known.
         */
        private final int year;
        /**
         * Movie type, may be one of:
         *    - tv_episode
         *    - tv_special
         *    - documentary
         *    - short
         *    - tv_series
         *    - video
         *    - feature (that is regular full length movie)
         */
        private final String type;

        public MovieInfo(ResultSet movieResults, int reviewsCount) throws SQLException {
            this.imdb_id = movieResults.getString("imdb_id");
            this.title = movieResults.getString("title");
            this.plot = movieResults.getString("plot");
            this.poster_url = movieResults.getString("poster_url");

            Array creditsArray = movieResults.getArray("credits");
            if (creditsArray == null) {
                this.credits = new String[]{};
            } else {
                this.credits = (String[]) creditsArray.getArray();
            }

            this.releaseDate = movieResults.getString("date");
            this.reviewsCount = reviewsCount;

            Array genresArray = movieResults.getArray("genres");
            if (genresArray == null) {
                this.genres = new String[]{};
            } else {
                this.genres = (String[]) genresArray.getArray();
            }

            this.imdbRating = movieResults.getDouble("rating");
            this.voteCnt = movieResults.getInt("votes");
            this.year = movieResults.getInt("year");
            this.type = movieResults.getString("type");
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
