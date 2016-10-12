package ru.spbau.mit;


import org.apache.lucene.queryparser.classic.ParseException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class SearchServer {
  public static void start() throws Exception {
    Server server = new Server(8080);
    server.setHandler(new SearchHandler());
    server.start();
    server.join();
  }

  private static class SearchHandler extends AbstractHandler {
    private final Searcher searcher = new Searcher();

    private SearchHandler() throws IOException {
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpRequest,
                       HttpServletResponse response) throws IOException, ServletException {
      try (BufferedReader reader = httpRequest.getReader();
           PrintWriter writer = response.getWriter()) {
        String query = "";
        String line;
        while ((line = reader.readLine()) != null) {
          query += line;
        }
        searcher.search(query, writer);
      } catch (ParseException e) {
        System.err.println("Failed to parse query: " + e);
      }

      response.setContentType("text/html;charset=utf-8");
      response.setStatus(HttpServletResponse.SC_OK);
      request.setHandled(true);
    }
  }
}
