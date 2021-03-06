import com.google.common.base.Predicates;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Data;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import ranking.Ranking;
import ranking.Ranking.SearchCandidate;
import ranking.ScoresCombiner;
import search.SearchResponse;
import search.Searcher;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Predicate;

public class SearchServer {
  public static final int PORT = 5242;

  public static void start() throws Exception {
    final HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
    server.createContext("/s", new SearchRequestHandler());
    server.setExecutor(null);
    server.start();
  }

  private static class SearchRequestHandler implements HttpHandler {
    private Searcher searcher = new Searcher();

    SearchRequestHandler() throws IOException, SQLException {
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
      final List<NameValuePair> params = URLEncodedUtils.parse(httpExchange.getRequestURI(), "UTF-8");
      String requestString = null;
      String coefFile = ScoresCombiner.COEF_FILE;
      int y1 = 0, y2 = 2100;
      double ratingFrom = -1, ratingTo = 11;
      for (NameValuePair param : params) {
        if (param.getName().equals("q")) {
          requestString = param.getValue();
        }
        if (param.getName().equals("coef")) {
          coefFile = param.getValue();
        }
        if (param.getName().equals("y1")) {
          try {
            y1 = Integer.parseInt(param.getValue());
          } catch (NumberFormatException ignored) {}
        }
        if (param.getName().equals("y2")) {
          try {
            y2 = Integer.parseInt(param.getValue());
          } catch (NumberFormatException ignored) {}
        }
        if (param.getName().equals("ratingFrom")) {
          try {
            ratingFrom = Double.parseDouble(param.getValue());
          } catch (NumberFormatException ignored) {}
        }
        if (param.getName().equals("ratingTo")) {
          try {
            ratingTo = Double.parseDouble(param.getValue());
          } catch (NumberFormatException ignored) {}
        }
      }
      searcher.setCoefFile(coefFile);
      OutputStream output = httpExchange.getResponseBody();
      if (requestString == null) {
        httpExchange.sendResponseHeaders(400, 0);
        output.close();
        System.err.println("empty request");
        return;
      }

      Predicate<SearchCandidate> filter = c -> true;
      if (y1 != 0 || y2 != 2100) {
        filter = filter.and(new Searcher.YearFilter(y1, y2));
      }
      if (ratingFrom != -1 || ratingTo != 11) {
        filter = filter.and(new Searcher.RatingFilter(ratingFrom, ratingTo));
      }

      try {
        SearchResponse[] response = searcher.search(requestString, filter);
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeSpecialFloatingPointValues()
                .create();
        byte[] bytes = gson.toJson(new MyResponse(response), MyResponse.class).getBytes();
        httpExchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        httpExchange.sendResponseHeaders(200, bytes.length);
        output.write(bytes);
        output.close();
      } catch (Exception e) {
        String errorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
        System.err.println("failed to process request: " + errorMessage);
        e.printStackTrace();
        httpExchange.sendResponseHeaders(500, errorMessage.length());
        output.write(errorMessage.getBytes());
        output.close();
      }
    }
  }

  @Data
  private static class MyResponse {
    private final ResponseItem[] data;

    public MyResponse(SearchResponse[] response) {
      data = new ResponseItem[response.length];
      for (int i = 0; i < response.length; i++) {
        data[i] = new ResponseItem(response[i]);
      }
    }
  }

  @Data
  private static class ResponseItem {
    private final String id;
    private final String type = "movie";
    private final SearchResponse attributes;

    public ResponseItem(SearchResponse response) {
      this.attributes = response;
      this.id = response.getMovieId();
    }
  }
}