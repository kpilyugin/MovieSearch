package ru.spbau.mit;

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
import java.io.PrintWriter;
import java.nio.file.Paths;

public class Searcher {

  private final StandardAnalyzer analyzer = new StandardAnalyzer();
  private final Directory index = FSDirectory.open(Paths.get(ReviewsIndex.INDEX_DIRECTORY));

  public Searcher() throws IOException {
  }

  public void search(String line, PrintWriter writer) throws ParseException, IOException {
    Query query = new QueryParser("review", analyzer).parse(line);

    int hitsPerPage = 10;
    IndexReader reader = DirectoryReader.open(index);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopDocs docs = searcher.search(query, hitsPerPage);
    ScoreDoc[] hits = docs.scoreDocs;

    writer.println("Found " + hits.length + " hits.");
    for (int i = 0; i < hits.length; ++i) {
      ScoreDoc hit = hits[i];
      int docId = hit.doc;
      Document d = searcher.doc(docId);
      String id = d.get("id");
      writer.println((i + 1) + ": score = " + hit.score + ". " + id + " " + d.get("review"));
    }
  }
}
