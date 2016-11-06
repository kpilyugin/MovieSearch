package search;

import data.IndexBuilder;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class Searcher {

  private static final int NUM_RESULTS = 5;

  private final StandardAnalyzer analyzer = new StandardAnalyzer();
  private final Directory index = FSDirectory.open(Paths.get(IndexBuilder.INDEX_DIRECTORY));
  private final IndexReader reader = DirectoryReader.open(index);

  IndexSearcher searcher = new IndexSearcher(reader);

  public Searcher() throws IOException {
  }

  public SearchResponse[] search(String line) throws ParseException, IOException {
    Query query = new QueryParser("summary", analyzer).parse(line);
    TopDocs docs = searcher.search(query, NUM_RESULTS);
    ScoreDoc[] hits = docs.scoreDocs;

    SearchResponse[] responses = new SearchResponse[hits.length];
    for (int i = 0; i < hits.length; ++i) {
      ScoreDoc hit = hits[i];
      Document doc = searcher.doc(hit.doc);
      responses[i] = new SearchResponse(Integer.parseInt(doc.get("wikiId")), doc.get("name"), hit.score);
    }
    return responses;
  }
}
