package search;

import lombok.Data;
import ranking.Ranking;

import java.util.Map;

@Data
public class SearchResponse {
  private final String movieId;
  private final String title;
  private final String plot;
  private final String releaseDate;
  private final String poster;
  private final int year;
  private final String[] genres;
  private final double imdbRating;
  private final int voteCnt;
  private final double score;
  private final String origin;
  private final String fragment;
  private final Map<String, Double> debugScores;

  public SearchResponse(Ranking.SearchCandidate sc, Searcher.LuceneResult lr) {
    movieId = sc.getMovieId();
    title = sc.getTitle();
    plot = sc.getPlot();
    releaseDate = sc.getReleaseDate();
    poster = sc.getPoster();
    score = sc.getFinalScore();
    origin = lr.getOrigin();
    fragment = lr.getFragment();
    debugScores = sc.getScores();
    year = sc.getYear();
    genres = sc.getGenres();
    imdbRating = sc.getImdbRating();
    voteCnt = sc.getVoteCnt();
  }
}
