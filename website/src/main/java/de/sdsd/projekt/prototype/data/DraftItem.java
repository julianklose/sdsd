package de.sdsd.projekt.prototype.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import de.sdsd.projekt.prototype.applogic.TripleFunctions;

/**
 * Represents a wikinormia draft item/class, stored in MongoDB.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class DraftItem implements Comparable<DraftItem> {
	public static final String ID = "_id", USER = "user", FORMAT = "format", CONTENT = "content", 
			IDENTIFIER = "identifier", LABEL = "label", COMMENT = "shortDescription", 
			BASE = "base", PARTOF = "partof", ATTRIBUTES = "attributes", INSTANCES = "instances",
			VALUE = "value", TYPE = "type", UNIT = "unit";
	public static final String CONTENT_IDENTIFIER = CONTENT + '.' + IDENTIFIER;

	public static Bson filter(User user) {
		return Filters.eq(USER, user.getName());
	}

	public static Bson filter(User user, ObjectId id) {
		return Filters.and(filter(user), Filters.eq(id));
	}
	
	public static Bson filterFormat(ObjectId formatID) {
		return Filters.eq(FORMAT, formatID);
	}
	
	public static Bson filter(User user, ObjectId formatID, String identifier, @Nullable ObjectId id) {
		return id == null ? Filters.and(filter(user), filterFormat(formatID), Filters.eq(CONTENT_IDENTIFIER, identifier))
				: Filters.and(Filters.not(Filters.eq(id)), filter(user), filterFormat(formatID), Filters.eq(CONTENT_IDENTIFIER, identifier));
	}
	
	public static Document create(User user, ObjectId format, JSONObject content) {
		Document doc = new Document()
				.append(ID, new ObjectId())
				.append(USER, user.getName())
				.append(FORMAT, format)
				.append(CONTENT, Document.parse(content.toString()));
		return doc;
	}
	
	public static DraftItem getDefault(User user, ObjectId id) {
		return new DraftItem(id, user);
	}

	private final ObjectId id;
	private final String user;
	private final ObjectId format;
	private Document content;

	public DraftItem(Document doc) {
		this.id = doc.getObjectId(ID);
		this.user = doc.getString(USER);
		this.format = doc.getObjectId(FORMAT);
		this.content = doc.get(CONTENT, Document.class);
	}
	
	protected DraftItem(ObjectId id, User user) {
		this.id = id;
		this.user = user.getName();
		this.format = id;
		this.content = new Document();
	}
	
	public Bson filter() {
		return Filters.eq(id);
	}

	public ObjectId getId() {
		return id;
	}
	
	public String getAuthor() {
		return user;
	}
	
	public ObjectId getFormat() {
		return format;
	}
	
	public Document getContent() {
		return content;
	}
	
	public JSONObject getJson() {
		return new JSONObject(content.toJson());
	}
	
	public Bson setContent(JSONObject content) {
		this.content = Document.parse(content.toString());
		return Updates.set(CONTENT, this.content);
	}
	
	public String getIdentifier() {
		return content.getString(IDENTIFIER);
	}
	
	public String getLabel() {
		return content.getString(LABEL);
	}
	
	@CheckForNull
	public String getComment() {
		return content.getString(COMMENT);
	}
	
	public List<Ref> getBase() {
		List<Document> list = content.getList(BASE, Document.class);
		if(list == null) return Collections.emptyList();
		List<Ref> out = new ArrayList<>(list.size());
		for(Document doc : list) {
			out.add(Ref.of(doc.getString(VALUE)));
		}
		return out;
	}
	
	public List<Ref> getPartOf() {
		List<Document> list = content.getList(PARTOF, Document.class);
		if(list == null) return Collections.emptyList();
		List<Ref> out = new ArrayList<>(list.size());
		for(Document doc : list) {
			out.add(Ref.of(doc.getString(VALUE)));
		}
		return out;
	}
	
	public List<Attribute> getAttributes() {
		List<Document> list = content.getList(ATTRIBUTES, Document.class);
		if(list == null) return Collections.emptyList();
		List<Attribute> out = new ArrayList<>(list.size());
		for(Document doc : list) {
			out.add(new Attribute(doc));
		}
		return out;
	}
	
	public List<Instance> getInstances() {
		List<Document> list = content.getList(INSTANCES, Document.class);
		if(list == null) return Collections.emptyList();
		List<Instance> out = new ArrayList<>(list.size());
		for(Document doc : list) {
			out.add(new Instance(doc));
		}
		return out;
	}

	@Override
	public int compareTo(DraftItem o) {
		return getLabel().compareToIgnoreCase(o.getLabel());
	}
	
	/**
	 * A reference to a wikinormia resource, either in draft mode or finalized.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class Ref {
		@CheckForNull
		public static Ref of(@Nullable String ref) {
			if(ref == null || ref.isBlank()) return null;
			return new Ref(ref);
		}
		
		public final String ref;
		public Ref(String ref) {
			this.ref = ref.startsWith("wkn:") ? TripleFunctions.NS_WIKI + ref.substring(4) : ref;
		}
		public boolean isDraft() {
			return Util.isObjectId(ref);
		}
		public boolean isWiki() {
			return !Util.isObjectId(ref);
		}
		public Node asNode() {
			return NodeFactory.createURI(ref);
		}
		public ObjectId asObjectId() {
			return new ObjectId(ref);
		}
		@Override
		public String toString() {
			return ref;
		}
		@Override
		public int hashCode() {
			return isDraft() ? asObjectId().hashCode() : asNode().hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof Ref))
				return false;
			Ref other = (Ref) obj;
			if(isDraft()) {
				return other.isDraft() ? asObjectId().equals(other.asObjectId()) : false;
			} else {
				return other.isDraft() ? false : asNode().equals(other.asNode());
			}
		}
	}
	
	/**
	 * A wikinormia draft mode attribute.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class Attribute {
		private final Document doc;
		public Attribute(Document doc) {
			this.doc = doc;
		}
		public String getIdentifier() {
			return doc.getString(IDENTIFIER);
		}
		public String getLabel() {
			return doc.getString(LABEL);
		}
		public Ref getType() {
			return Ref.of(doc.getString(TYPE));
		}
		@CheckForNull
		public String getUnit() {
			return doc.getString(UNIT);
		}
	}
	
	/**
	 * A wikinormia draft mode attribute value.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class AttributeValues {
		private final Document doc;
		
		public AttributeValues(Document doc) {
			this.doc = doc;
		}
		public Ref getType() {
			return Ref.of(doc.getString(TYPE));
		}
		public List<Object> getValues() {
			List<Object> list = doc.getList(VALUE, Object.class);
			return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
		}
	}
	
	/**
	 * A wikinormia draft mode element instance.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class Instance {
		private final Document doc;
		public Instance(Document doc) {
			this.doc = doc;
		}
		public String getIdentifier() {
			return doc.getString(IDENTIFIER);
		}
		public String getLabel() {
			return doc.getString(LABEL);
		}
		public Map<Ref, String> getPartOf() {
			List<Document> list = doc.getList(PARTOF, Document.class);
			if(list == null) return Collections.emptyMap();
			Map<Ref, String> out = new HashMap<>(list.size());
			for(Document doc : list) {
				out.put(Ref.of(doc.getString(VALUE)), doc.getString(IDENTIFIER));
			}
			return out;
		}
		public Map<Ref, Map<String, AttributeValues>> getAttributes() {
			Document types = doc.get(ATTRIBUTES, Document.class);
			if(types == null) return Collections.emptyMap();
			Map<Ref, Map<String, AttributeValues>> out = new HashMap<>(types.size());
			for(String uri : types.keySet()) {
				Document attrs = types.get(uri, Document.class);
				Map<String, AttributeValues> aout = new HashMap<>(attrs.size());
				for(String id : attrs.keySet()) {
					aout.put(id, new AttributeValues(attrs.get(id, Document.class)));
				}
				out.put(Ref.of(uri), aout);
			}
			return out;
		}
	}
}
