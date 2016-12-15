package search;

import index.IndexBuilder;
import lombok.Data;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import ranking.CandidateInfoProvider;
import ranking.Ranking;
import ranking.Ranking.SearchCandidate;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Searcher {

  private static final int CANDIDATES_LIMIT = 150; //

  private final Directory index = FSDirectory.open(Paths.get(IndexBuilder.INDEX_DIRECTORY));
  private final IndexReader reader = DirectoryReader.open(index);
  private final IndexSearcher searcher = new IndexSearcher(reader);
  private final StandardAnalyzer analyzer = new StandardAnalyzer();
  private final Ranking ranking = new Ranking();

  public Searcher() throws IOException {
  }

  public SearchResponse[] search(String query, Predicate<SearchCandidate> filter) throws ParseException, IOException, InvalidTokenOffsetsException, SQLException {
    final CandidatesResults results = getCandidates(query);
    final List<SearchCandidate> filtered =
            results.candidates.stream().filter(filter).collect(Collectors.toList());

    return ranking.rerank(filtered, query).stream()
            .map(c -> new SearchResponse(c, results.luceneResults.get(c.getMovieId())))
            .toArray(SearchResponse[]::new);
  }

  public void setCoefFile(String coefFile) {
    ranking.setCoefFile(coefFile);
  }

  public CandidatesResults getCandidates(String query) throws ParseException, IOException, InvalidTokenOffsetsException, SQLException {
    final List<LuceneResult> luceneResults = new ArrayList<>();
    luceneResults.addAll(doSearch("plot", query));
    luceneResults.addAll(doSearch("reviews", query));
    luceneResults.addAll(doSearch("preprocessed_plot", query));

    final Map<String, LuceneResult> uniqueResults = new HashMap<>();
    for (LuceneResult result : luceneResults) {
      if (!uniqueResults.containsKey(result.getMovieId())) {
        uniqueResults.put(result.getMovieId(), result);
      } else {
        uniqueResults.put(result.getMovieId(),
                uniqueResults.get(result.getMovieId()).combine(result));
      }
    }

    final List<SearchCandidate> candidates = new ArrayList<>();
    try (CandidateInfoProvider provider = new CandidateInfoProvider()) {
      for (LuceneResult result : uniqueResults.values()) {
        candidates.add(provider.getInfo(result.getMovieId(), result.getScore()));
      }
    }

    return new CandidatesResults(uniqueResults, candidates);
  }

  private List<LuceneResult> doSearch(String field, String queryLine) throws ParseException, IOException, InvalidTokenOffsetsException {
    List<LuceneResult> results = new ArrayList<>();
    //Query query = new QueryParser(field, analyzer).parse(queryLine);
    Query query = new QueryParser(field, analyzer).parse(QueryParser.escape(queryLine));
    TopDocs docs = searcher.search(query, CANDIDATES_LIMIT);

    QueryScorer scorer = new QueryScorer(query, field);
    Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);
    Highlighter highlighter = new Highlighter(scorer);
    highlighter.setTextFragmenter(fragmenter);

    for (ScoreDoc hit : docs.scoreDocs) {
      Document doc = searcher.doc(hit.doc);

      //noinspection deprecation
      TokenStream tokenStream = TokenSources.getAnyTokenStream(reader, hit.doc, field, doc, analyzer);
      String fragment = highlighter.getBestFragment(tokenStream, doc.get(field));
      results.add(new LuceneResult(doc.get("movie-id"), hit.score, field, fragment));
    }
    return results;
  }

  @Data
  public static class LuceneResult {
    private final String movieId;
    private final double score;
    private final String origin;
    private final String fragment;

    public LuceneResult combine(LuceneResult that) {
      return new LuceneResult(
              movieId,
              Math.max(this.score, that.score),
              this.origin + "," + that.origin,
              this.fragment + "\n" + that.fragment
      );
    }
  }

  @Data
  public static class CandidatesResults {
    private final Map<String, LuceneResult> luceneResults;
    private final List<SearchCandidate> candidates;
  }

  public static class YearFilter implements Predicate<SearchCandidate> {
    private final int y1;
    private final int y2;

    public YearFilter(int y1, int y2) {
      this.y1 = y1;
      this.y2 = y2;
    }

    @Override
    public boolean test(SearchCandidate searchCandidate) {
      return y1 <= searchCandidate.getYear() && searchCandidate.getYear() <= y2;
    }
  }

  public static class RatingFilter implements Predicate<SearchCandidate> {
    private final double ratingFrom;
    private final double ratingTo;

    public RatingFilter(double ratingFrom, double ratingTo) {
      this.ratingFrom = ratingFrom;
      this.ratingTo = ratingTo;
    }

    @Override
    public boolean test(SearchCandidate searchCandidate) {
      return ratingFrom <= searchCandidate.getImdbRating() &&
              searchCandidate.getImdbRating() <= ratingTo;
    }
  }

}
