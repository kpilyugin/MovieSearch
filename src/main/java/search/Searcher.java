package search;

import data.IndexBuilder;
import org.apache.commons.lang3.ArrayUtils;
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

import java.io.IOException;
import java.nio.file.Paths;

public class Searcher {

  private static final int NUM_RESULTS = 10;

  private final StandardAnalyzer analyzer = new StandardAnalyzer();
  private final Directory index = FSDirectory.open(Paths.get(IndexBuilder.INDEX_DIRECTORY));
  private final IndexReader reader = DirectoryReader.open(index);

  IndexSearcher searcher = new IndexSearcher(reader);

  public Searcher() throws IOException {
  }

  public SearchResponse[] search(String line) throws ParseException, IOException, InvalidTokenOffsetsException {
    SearchResponse[] fromSummary = doSearch(false, line);
    SearchResponse[] fromReviews = doSearch(true, line);
    return ArrayUtils.addAll(fromSummary, fromReviews);
  }

  private SearchResponse[] doSearch(boolean inReviews, String line) throws ParseException, IOException, InvalidTokenOffsetsException {
    String field = inReviews ? "reviews" : "summary";
    Query query = new QueryParser(field, analyzer).parse(line);
    TopDocs docs = searcher.search(query, NUM_RESULTS);
    ScoreDoc[] hits = docs.scoreDocs;
    SearchResponse[] responses = new SearchResponse[hits.length];

    QueryScorer scorer = new QueryScorer(query, field);
    Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);
    Highlighter highlighter = new Highlighter(scorer);
    highlighter.setTextFragmenter(fragmenter);

    for (int i = 0; i < hits.length; ++i) {
      ScoreDoc hit = hits[i];
      Document doc = searcher.doc(hit.doc);
      int wikiId = Integer.parseInt(doc.get("wikiId"));
      boolean hasReviews = doc.get("reviews").length() > 0;

      //noinspection deprecation
      TokenStream tokenStream = TokenSources.getAnyTokenStream(reader, hit.doc, field, doc, analyzer);
      String fragment = highlighter.getBestFragment(tokenStream, doc.get(field));
      responses[i] = new SearchResponse(
          wikiId, doc.get("asin"), doc.get("name"), doc.get("date"),
          hit.score, inReviews, hasReviews, fragment
      );
    }
    return responses;
  }

}
