import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Data;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import search.SearchResponse;
import search.Searcher;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

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

    SearchRequestHandler() throws IOException {
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
      final List<NameValuePair> params = URLEncodedUtils.parse(httpExchange.getRequestURI(), "UTF-8");
      String requestString = null;
      for (NameValuePair param : params) {
        if (param.getName().equals("q")) {
          requestString = param.getValue();
        }
      }
      OutputStream output = httpExchange.getResponseBody();
      if (requestString == null) {
        httpExchange.sendResponseHeaders(400, 0);
        output.close();
        return;
      }

      SearchResponse[] response;
      try {
        response = searcher.search(requestString);
      } catch (Exception e) {
        httpExchange.sendResponseHeaders(500, e.getMessage().length());
        output.write(e.getMessage().getBytes());
        output.close();
        return;
      }

      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      byte[] bytes = gson.toJson(new MyResponse(response), MyResponse.class).getBytes();
      httpExchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
      httpExchange.sendResponseHeaders(200, bytes.length);
      output.write(bytes);
      output.close();
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