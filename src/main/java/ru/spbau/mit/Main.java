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
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) throws IOException, ParseException {
    if (args.length > 0 && "-build".equals(args[0])) {
      System.out.println("Building index");
      ReviewsIndex.build();
    } else {
      System.out.println("Starting search");
      startSearch();
    }
  }

  private static void startSearch() throws ParseException, IOException {
    StandardAnalyzer analyzer = new StandardAnalyzer();
    Directory index = FSDirectory.open(Paths.get(ReviewsIndex.INDEX_DIRECTORY));
    try (Scanner scanner = new Scanner(System.in)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        Query query = new QueryParser("review", analyzer).parse(line);

        int hitsPerPage = 10;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(query, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        System.out.println("Found " + hits.length + " hits.");
        for (int i = 0; i < hits.length; ++i) {
          ScoreDoc hit = hits[i];
          int docId = hit.doc;
          Document d = searcher.doc(docId);
          String id = d.get("id");
          System.out.println((i + 1) + ": score = " + hit.score + ". " + id + " " + d.get("review"));
        }
      }
    }
  }
}
