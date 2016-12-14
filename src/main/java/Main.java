import index.IndexBuilder;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import preprocessor.PreprocessDbUpdater;
import ranking.DatasetBuilder;
import ranking.Regression;
import ranking.WordStatsBuilder;
import search.SearchResponse;
import search.Searcher;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) throws Exception {
    if (args.length > 0) {
      String arg = args[0];
      switch (arg) {
        case "-build":
          System.out.println("Building index");
          IndexBuilder.build();
          break;
        case "-server":
          System.out.println("Starting server");
          SearchServer.start();
          break;
        case "-preprocess":
          System.out.println("Preprocessing");
          PreprocessDbUpdater.start();
          break;
        case "-dataset":
          System.out.println("Building dataset");
          DatasetBuilder.build();
          break;
        case "-wordstats":
          System.out.println("Building wordstats");
          WordStatsBuilder.build();
          break;
        case "-regression":
          System.out.println("Calculating regression coefs");
          Regression.calculateCoefs();
      }
    } else {
      System.out.println("Search by console input");
      startSearch();
    }
  }

  private static void startSearch() throws ParseException, IOException, InvalidTokenOffsetsException, SQLException {
    Searcher searcher = new Searcher();
    try (Scanner scanner = new Scanner(System.in)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        SearchResponse[] responses = searcher.search(line, c -> true);
        for (int i = 0; i < responses.length; i++) {
          System.out.println((i + 1) + ": " + responses[i]);
        }
      }
    }
  }
}
