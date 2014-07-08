import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;

public class Runner extends HttpServlet {

    private static final String PROPERTIES_FILENAME = "youtube.properties";
    private static final long NUMBER_OF_VIDEOS_RETURNED = 25;
    private static final int queryLength = 5;

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.getWriter().print("Hello from Java!\n");
        // new Search(resp);
        SearchYouTube();
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new Runner()), "/*");
        server.start();
        server.join();
    }

    private static void SearchYouTube() {
        Properties properties = new Properties();
        try {
            InputStream in = Search.class.getResourceAsStream("/"
                    + PROPERTIES_FILENAME);
            properties.load(in);
        } catch (IOException e) {
            System.err.println("Error: error reading " + PROPERTIES_FILENAME
                    + ": " + e.getMessage());
        }

        try {
            FindRandom(properties);
        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: "
                    + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : "
                    + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void FindRandom(Properties properties)
            throws GoogleJsonResponseException, IOException, Throwable {
        YouTube youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT,
                Auth.JSON_FACTORY, new HttpRequestInitializer() {
                    public void initialize(HttpRequest request)
                            throws IOException {
                    }
                }).setApplicationName("search-random").build();

        String queryTerm = getRandomQuery();

        YouTube.Search.List search = youtube.search().list("id, snippet");

        String apiKey = properties.getProperty("youtube.apikey");
        search.setKey(apiKey);
        search.setQ(queryTerm);

        search.setType("video");

        search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
        search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
        SearchListResponse searchResponse = search.execute();
        List<SearchResult> searchResultList = searchResponse.getItems();
        if (searchResultList != null) {
            prettyPrint(searchResultList.iterator(), queryTerm);
        }
    }

    private static void prettyPrint(
            Iterator<SearchResult> iteratorSearchResults, String query)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=============================================================");
        sb.append("   First " + NUMBER_OF_VIDEOS_RETURNED
                + " videos for search on \"" + query + "\".");
        sb.append("=============================================================\n");

        if (!iteratorSearchResults.hasNext()) {
            sb.append(" There aren't any results for your query.");
        }

        while (iteratorSearchResults.hasNext()) {

            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();

            if (rId.getKind().equals("youtube#video")) {
                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails()
                        .getDefault();

                sb.append(" Video Id: " + rId.getVideoId());
                sb.append(" Title: " + singleVideo.getSnippet().getTitle());
                sb.append(" Thumbnail: " + thumbnail.getUrl());
                sb.append("\n-------------------------------------------------------------\n");
            }
        }
    }

    private static String getRandomQuery() {
        String query = "v=";
        Random rand = new Random();
        for (int i = 0; i < queryLength; i++) {
            query += (char) (rand.nextInt(93) + 33);
        }
        return query;
    }
}