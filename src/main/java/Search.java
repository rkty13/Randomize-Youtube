import java.io.IOException;
import java.util.ArrayList;
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

public class Search {

	/**
	 * 
	 */
	private final String PROPERTIES_FILENAME = "youtube.properties";
	private final long NUMBER_OF_VIDEOS_RETURNED = 25;
	private final int queryLength = 5;

	private YouTube youtube;
	private Properties properties;

	private HttpServletResponse resp;

	public Search(HttpServletResponse resp) {
		/*
		 * properties = new Properties(); try { InputStream in = Search.class
		 * .getResourceAsStream(PROPERTIES_FILENAME); properties.load(in); }
		 * catch (IOException e) { System.err.println("Error: error reading " +
		 * PROPERTIES_FILENAME + ": " + e.getMessage()); }
		 */
		this.resp = resp;

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

	public void FindRandom() throws GoogleJsonResponseException, IOException,
			Throwable {
		youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY,
				new HttpRequestInitializer() {
					public void initialize(HttpRequest request)
							throws IOException {
					}
				}).setApplicationName("search-random").build();

		String queryTerm = getRandomQuery();

		YouTube.Search.List search = youtube.search().list("id, snippet");

		String apiKey = "AIzaSyBXVoLvqBOr_DZtfu9hPcbqh1IqsfIM71Y";
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

	private void prettyPrint(Iterator<SearchResult> iteratorSearchResults,
			String query) throws IOException {

		resp.getWriter().println("\n=======================");
		resp.getWriter().println("   RANDOM YOUTUBE VIDEO:");
		resp.getWriter().println("=======================\n");

		if (!iteratorSearchResults.hasNext()) {
			resp.getWriter().println(
					" There aren't any results for your query.");
		}

		int count = 0;
		String info = null;
		ArrayList<String> videosInfo = new ArrayList<String>();

		while (iteratorSearchResults.hasNext()) {

			SearchResult singleVideo = iteratorSearchResults.next();
			ResourceId rId = singleVideo.getId();

			if (rId.getKind().equals("youtube#video")) {
				Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails()
						.getDefault();

				info = "";

				info += " Video Id: " + rId.getVideoId() + "\n";
				info += " Title: " + singleVideo.getSnippet().getTitle() + "\n";
				info += " Thumbnail: " + thumbnail.getUrl() + "\n";

				videosInfo.add(info);

				count++;
			}
		}

		Random chooseVideo = new Random();
		resp.getWriter().println(videosInfo.get(chooseVideo.nextInt(count)));
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