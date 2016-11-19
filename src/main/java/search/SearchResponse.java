package search;

import lombok.Data;
import ranking.Ranking;

import java.util.Map;

@Data
public class SearchResponse {
  private final String movieId;
  private final String title;
  private final String plot;
  private final String date;
  private final String poster;
  private final double score;
  private final String origin;
  private final String fragment;
  private final Map<String, Double> debugScores;

  public SearchResponse(Ranking.SearchCandidate sc, Searcher.LuceneResult lr) {
    movieId = sc.getMovieId();
    title = sc.getTitle();
    plot = sc.getPlot();
    date = sc.getDate();
    poster = sc.getPoster();
    score = sc.getFinalScore();
    origin = lr.getOrigin();
    fragment = lr.getFragment();
    debugScores = sc.getScores();
  }
}
