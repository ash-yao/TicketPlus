package db.mongo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;

import db.DBConnection;
import entity.Item;
import external.TicketMasterAPI;

public class MongoConnection implements DBConnection {
	private MongoClient client;
	private MongoDatabase db;
	
	public MongoConnection() {
		client = new MongoClient();
		db = client.getDatabase(MongoUtil.DB_NAME);
	}
	@Override
	public void close() {
		// TODO Auto-generated method stub
		if (client != null) {
			client.close();
		}
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		// TODO Auto-generated method stub
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$push", new Document("favorite", new Document("$each", itemIds))));
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		// TODO Auto-generated method stub
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$pullAll", new Document("favorite", itemIds)));
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		// TODO Auto-generated method stub
		Set<String> res = new HashSet<String>();
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		if (iterable.first() != null && iterable.first().containsKey("favorite")) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) iterable.first().get("favorite");
			res.addAll(list);
		}
		return res;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		// TODO Auto-generated method stub
		Set<String> ids = getFavoriteItemIds(userId);
		Set<Item> res = new HashSet<>();
		Item.ItemBuilder ib = new Item.ItemBuilder();
		for (String id : ids) {
			FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", id));
			if (iterable.first() != null) {
				Document doc = iterable.first();
				ib.setItemId(doc.getString("item_id"))
				.setName(doc.getString("name"))
				.setDate(doc.getString("date"))
				.setAddress(doc.getString("address"))
				.setImageUrl(doc.getString("image_url"))
				.setUrl(doc.getString("url"))
				.setDistance(doc.getDouble("distance"))
				.setCategories(getCategories(id));
				res.add(ib.build());
			}
		}
		return res;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		// TODO Auto-generated method stub
		Set<String> res = new HashSet<>();
		FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));
		if (iterable.first() != null && iterable.first().containsKey("categories")) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) iterable.first().get("categories");
			res.addAll(list);
		}
		return res;
	}

	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		// TODO Auto-generated method stub
		List<Item> result = new TicketMasterAPI().search(lat, lon, term);
		for (Item i : result) {
			saveItem(i);
		}
		return result;
	}

	@Override
	public void saveItem(Item item) {
		// TODO Auto-generated method stub
		FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", item.getItemId()));
		if (iterable.first() == null) {
		db.getCollection("items").insertOne(new Document()
	    		.append("item_id", item.getItemId())
	    		.append("name", item.getName())
	    		.append("url", item.getUrl())
	    		.append("date", item.getDate())
	    		.append("image_url", item.getImageUrl())
	    		.append("distance", item.getDistance())
	    		.append("address", item.getAddress())
	    		.append("categories", item.getCategories())
	    		);
		}
	}

	@Override
	public String getFullname(String userId) {
		// TODO Auto-generated method stub
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		Document document = iterable.first();
		String firstName = document.getString("first_name");
		String lastName = document.getString("last_name");
		return firstName + " " + lastName;
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		// TODO Auto-generated method stub
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		return (iterable.first() != null) && (iterable.first().getString("password").equals(password));
	}
	
	// Test purpose
	public static void main(String[] args) {
		MongoConnection mongo = new MongoConnection();
		mongo.searchItems(37.38, -122.08, "music");
	}

}
