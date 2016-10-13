package ru.spbau.mit;

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) throws Exception {
    if (args.length > 0) {
      String arg = args[0];
      switch (arg) {
        case "-build":
          System.out.println("Building index");
          ReviewsIndex.build();
          break;
        case "-server":
          System.out.println("Starting server");
          SearchServer.start();
        case "-nlp":
          System.out.println("Named entity extractor");
          EntityExtractor.start();
      }
    } else {
      System.out.println("Search by console input");
      startSearch();
    }
  }

  private static void startSearch() throws ParseException, IOException {
    Searcher searcher = new Searcher();
    try (Scanner scanner = new Scanner(System.in);
         PrintWriter writer = new PrintWriter(System.out)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        searcher.search(line, writer);
      }
    }
  }
}
