import data.IndexBuilder;
import org.apache.lucene.queryparser.classic.ParseException;
import search.SearchResponse;
import search.Searcher;

import java.io.IOException;
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
      }
    } else {
      System.out.println("Search by console input");
      startSearch();
    }
  }

  private static void startSearch() throws ParseException, IOException {
    Searcher searcher = new Searcher();
    try (Scanner scanner = new Scanner(System.in)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        SearchResponse[] responses = searcher.search(line);
        for (int i = 0; i < responses.length; i++) {
          System.out.println((i + 1) + ": " + responses[i].getName() + ", score = " + responses[i].getScore());
        }
      }
    }
  }
}
