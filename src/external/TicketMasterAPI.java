package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;

public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_TERM = ""; // no restriction
	private static final String API_KEY = "SPgVLSN1NNGlTHna65xbNxxgcbPFUiAG";
	
	/**
	 * Helper methods
	 */
	private String getAddress(JSONObject event) throws JSONException {
		if (!event.isNull("_embedded")) {
			JSONObject embedded = event.getJSONObject("_embedded");
			if (!embedded.isNull("venues")) {
				JSONArray venues = embedded.getJSONArray("venues");
				if (venues.length() > 0) {
					JSONObject json = venues.getJSONObject(0);
					StringBuilder sb = new StringBuilder();
					if (!json.isNull("address")) {
						JSONObject address = json.getJSONObject("address");
						if (!address.isNull("line1")) {
							sb.append(address.getString("line1"));
						}
						if (!address.isNull("line2")) {
							sb.append(address.getString("line2"));
						}
						if (!address.isNull("line3")) {
							sb.append(address.getString("line3"));
						}
						sb.append(",");
					}
					if (!json.isNull("city")) {
						JSONObject city = json.getJSONObject("city");
						if (!city.isNull("name")) {
							sb.append(city.getString("name"));
						}
					}
					return sb.toString();
				}
			}
		}
		return null;

	}

	private String getImageUrl(JSONObject event) throws JSONException {
		if (!event.isNull("images")) {
			JSONArray arr = event.getJSONArray("images");
			if (arr.length() > 0) {
				return arr.getJSONObject(0).getString("url");
			}
		}
		return null;
	}

	private Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> results = new HashSet<>();
		if (!event.isNull("classifications")) {
			JSONArray arr = event.getJSONArray("classifications");
			for (int i = 0; i < arr.length(); i++) {
				JSONObject json = arr.getJSONObject(i);
				if (!json.isNull("segment")) {
					JSONObject seg = json.getJSONObject("segment");
					if (!seg.isNull("name")) {
						results.add(seg.getString("name"));
					}
				}
			}
			return results;
		}
		return null;
	}
	
	private String getDate(JSONObject event) throws JSONException {
		if (!event.isNull("dates")) {
			JSONObject date = event.getJSONObject("dates");
			if (!date.isNull("start")) {
				JSONObject go = date.getJSONObject("start");
				if (!go.isNull("localDate")) {
					return go.getString("localDate");
				}
			}
		}
		return null;
	}

	// Convert JSONArray to a list of item objects.
	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> results = new ArrayList<>();
		for (int i = 0; i < events.length(); i++) {
			JSONObject event = events.getJSONObject(i);
			Item item = new Item.ItemBuilder()
					.setName(event.isNull("name") ? null : event.getString("name"))
					.setItemId(event.isNull("id") ? null : event.getString("id"))
					.setUrl(event.isNull("url") ? null : event.getString("url"))
					.setDistance(event.isNull("distance") ? 0.0 : event.getDouble("distance"))
					.setAddress(getAddress(event))
					.setImageUrl(getImageUrl(event))
					.setCategories(getCategories(event))
					.setDate(getDate(event))
					.build();
			results.add(item);
		}
		return results;
	}

	public List<Item> search(double lat, double lon, String term) {
		if (term == null) {
			term = DEFAULT_TERM;
		} else {
			try {
				term = java.net.URLEncoder.encode(term);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		String geohash = GeoHash.encodeGeohash(lat, lon, 8);
		String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s"
				,API_KEY, geohash, term, 50);
		try {
			HttpURLConnection connection = 
					(HttpURLConnection)new URL(URL+"?"+query).openConnection();
			connection.setRequestMethod("GET");
			
			int responseCode = connection.getResponseCode();
			System.out.println("ResponseCode: " + responseCode);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line);
			}
			in.close();
			
			JSONObject reObj = new JSONObject(sb.toString());
			if (reObj.isNull("_embedded")) {
				return new ArrayList<>();
			} else {
				return getItemList(reObj.getJSONObject("_embedded").getJSONArray("events"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	private void queryAPI(double lat, double lon) {
		List<Item> events = search(lat, lon, null);
		try {
		    for (int i = 0; i < events.size(); i++) {
		        JSONObject event = events.get(i).toJSONObject();
		        System.out.println(event);
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// Houston, TX
		tmApi.queryAPI(29.682684, -95.295410);
	}
}
