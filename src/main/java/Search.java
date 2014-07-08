import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.servlet.http.HttpServletResponse;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;

class Search {

	private static final String PROPERTIES_FILENAME = "youtube.properties";
	private static final long NUMBER_OF_VIDEOS_RETURNED = 25;
	private static final int queryLength = 5;

	private YouTube youtube;
	private Properties properties;
	
	private HttpServletResponse resp;

	public Search(HttpServletResponse resp) {
		properties = new Properties();
		this.resp = resp;
		try {
			InputStream in = Search.class.getResourceAsStream("/"
					+ PROPERTIES_FILENAME);
			properties.load(in);
		} catch (IOException e) {
			System.err.println("Error: error reading " + PROPERTIES_FILENAME
					+ ": " + e.getMessage());
		}

		try {
			FindRandom();
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

	private void FindRandom() throws GoogleJsonResponseException, IOException, Throwable {
		youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
			public void initialize(HttpRequest request) throws IOException {
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
	
	private void prettyPrint(Iterator<SearchResult> iteratorSearchResults, String query) throws IOException {

		resp.getWriter().println("\n=============================================================");
		resp.getWriter().println(
                "   First " + NUMBER_OF_VIDEOS_RETURNED + " videos for search on \"" + query + "\".");
		resp.getWriter().println("=============================================================\n");

        if (!iteratorSearchResults.hasNext()) {
        	resp.getWriter().println(" There aren't any results for your query.");
        }

        while (iteratorSearchResults.hasNext()) {

            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();

            if (rId.getKind().equals("youtube#video")) {
                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();

                resp.getWriter().println(" Video Id: " + rId.getVideoId());
                resp.getWriter().println(" Title: " + singleVideo.getSnippet().getTitle());
                resp.getWriter().println(" Thumbnail: " + thumbnail.getUrl());
                resp.getWriter().println("\n-------------------------------------------------------------\n");
            }
        }
    }
	
	private String getRandomQuery() {
		String query = "v=";
    	Random rand = new Random();
    	for (int i = 0; i < queryLength; i++) {
    		query += (char) (rand.nextInt(93) + 33);
    	}
    	return query;
	}
}