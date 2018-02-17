package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;

public class GeoRecommendation {
	public List<Item> recommend (String userId, double lat, double lon) {
		List<Item> result = new ArrayList<>();
		DBConnection conn = DBConnectionFactory.getDBConnection("mongo");
		Set<String> itemIds = conn.getFavoriteItemIds(userId);
		
		Map<String, Integer> categories = new HashMap<>();
		for (String id : itemIds) {
			Set<String> cur = conn.getCategories(id);
			for (String cat : cur) {
				Integer temp = categories.get(cat);
				if (temp == null) {
					categories.put(cat, 1); 
				} else {
					categories.put(cat, temp + 1);
				}
			}
		}
		List<Entry<String, Integer>> categoryList = new ArrayList<Entry<String, Integer>>(categories.entrySet());
		Collections.sort(categoryList, new Comparator<Entry<String, Integer>>() {
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return Integer.compare(o2.getValue(), o1.getValue());
			}
		});
		
		Set<String> used = new HashSet<>();
		for (Entry<String, Integer> en : categoryList) {
			List<Item> cur = conn.searchItems(lat, lon, en.getKey());
			Collections.sort(cur, new Comparator<Item>() {
				public int compare(Item a, Item b) {
					if (a.getDistance() == b.getDistance()) {
						return 0;
					}
					return a.getDistance() < b.getDistance() ? -1 : 1;
				}
			});
			for (Item i : cur) {
				if (used.add(i.getItemId()) && !itemIds.contains(i.getItemId())) {
					result.add(i);
				}
			}
		}
		
		return result;
	}
}
