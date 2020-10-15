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
	
	/** The Constant UNIT. */
	public static final String ID = "_id", USER = "user", FORMAT = "format", CONTENT = "content", 
			IDENTIFIER = "identifier", LABEL = "label", COMMENT = "shortDescription", 
			BASE = "base", PARTOF = "partof", ATTRIBUTES = "attributes", INSTANCES = "instances",
			VALUE = "value", TYPE = "type", UNIT = "unit";
	
	/** The Constant CONTENT_IDENTIFIER. */
	public static final String CONTENT_IDENTIFIER = CONTENT + '.' + IDENTIFIER;

	/**
	 * Filter.
	 *
	 * @param user the user
	 * @return the bson
	 */
	public static Bson filter(User user) {
		return Filters.eq(USER, user.getName());
	}

	/**
	 * Filter.
	 *
	 * @param user the user
	 * @param id the id
	 * @return the bson
	 */
	public static Bson filter(User user, ObjectId id) {
		return Filters.and(filter(user), Filters.eq(id));
	}
	
	/**
	 * Filter format.
	 *
	 * @param formatID the format ID
	 * @return the bson
	 */
	public static Bson filterFormat(ObjectId formatID) {
		return Filters.eq(FORMAT, formatID);
	}
	
	/**
	 * Filter.
	 *
	 * @param user the user
	 * @param formatID the format ID
	 * @param identifier the identifier
	 * @param id the id
	 * @return the bson
	 */
	public static Bson filter(User user, ObjectId formatID, String identifier, @Nullable ObjectId id) {
		return id == null ? Filters.and(filter(user), filterFormat(formatID), Filters.eq(CONTENT_IDENTIFIER, identifier))
				: Filters.and(Filters.not(Filters.eq(id)), filter(user), filterFormat(formatID), Filters.eq(CONTENT_IDENTIFIER, identifier));
	}
	
	/**
	 * Creates the.
	 *
	 * @param user the user
	 * @param format the format
	 * @param content the content
	 * @return the document
	 */
	public static Document create(User user, ObjectId format, JSONObject content) {
		Document doc = new Document()
				.append(ID, new ObjectId())
				.append(USER, user.getName())
				.append(FORMAT, format)
				.append(CONTENT, Document.parse(content.toString()));
		return doc;
	}
	
	/**
	 * Gets the default.
	 *
	 * @param user the user
	 * @param id the id
	 * @return the default
	 */
	public static DraftItem getDefault(User user, ObjectId id) {
		return new DraftItem(id, user);
	}

	/** The id. */
	private final ObjectId id;
	
	/** The user. */
	private final String user;
	
	/** The format. */
	private final ObjectId format;
	
	/** The content. */
	private Document content;

	/**
	 * Instantiates a new draft item.
	 *
	 * @param doc the doc
	 */
	public DraftItem(Document doc) {
		this.id = doc.getObjectId(ID);
		this.user = doc.getString(USER);
		this.format = doc.getObjectId(FORMAT);
		this.content = doc.get(CONTENT, Document.class);
	}
	
	/**
	 * Instantiates a new draft item.
	 *
	 * @param id the id
	 * @param user the user
	 */
	protected DraftItem(ObjectId id, User user) {
		this.id = id;
		this.user = user.getName();
		this.format = id;
		this.content = new Document();
	}
	
	/**
	 * Filter.
	 *
	 * @return the bson
	 */
	public Bson filter() {
		return Filters.eq(id);
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public ObjectId getId() {
		return id;
	}
	
	/**
	 * Gets the author.
	 *
	 * @return the author
	 */
	public String getAuthor() {
		return user;
	}
	
	/**
	 * Gets the format.
	 *
	 * @return the format
	 */
	public ObjectId getFormat() {
		return format;
	}
	
	/**
	 * Gets the content.
	 *
	 * @return the content
	 */
	public Document getContent() {
		return content;
	}
	
	/**
	 * Gets the json.
	 *
	 * @return the json
	 */
	public JSONObject getJson() {
		return new JSONObject(content.toJson());
	}
	
	/**
	 * Sets the content.
	 *
	 * @param content the content
	 * @return the bson
	 */
	public Bson setContent(JSONObject content) {
		this.content = Document.parse(content.toString());
		return Updates.set(CONTENT, this.content);
	}
	
	/**
	 * Gets the identifier.
	 *
	 * @return the identifier
	 */
	public String getIdentifier() {
		return content.getString(IDENTIFIER);
	}
	
	/**
	 * Gets the label.
	 *
	 * @return the label
	 */
	public String getLabel() {
		return content.getString(LABEL);
	}
	
	/**
	 * Gets the comment.
	 *
	 * @return the comment
	 */
	@CheckForNull
	public String getComment() {
		return content.getString(COMMENT);
	}
	
	/**
	 * Gets the base.
	 *
	 * @return the base
	 */
	public List<Ref> getBase() {
		List<Document> list = content.getList(BASE, Document.class);
		if(list == null) return Collections.emptyList();
		List<Ref> out = new ArrayList<>(list.size());
		for(Document doc : list) {
			out.add(Ref.of(doc.getString(VALUE)));
		}
		return out;
	}
	
	/**
	 * Gets the part of.
	 *
	 * @return the part of
	 */
	public List<Ref> getPartOf() {
		List<Document> list = content.getList(PARTOF, Document.class);
		if(list == null) return Collections.emptyList();
		List<Ref> out = new ArrayList<>(list.size());
		for(Document doc : list) {
			out.add(Ref.of(doc.getString(VALUE)));
		}
		return out;
	}
	
	/**
	 * Gets the attributes.
	 *
	 * @return the attributes
	 */
	public List<Attribute> getAttributes() {
		List<Document> list = content.getList(ATTRIBUTES, Document.class);
		if(list == null) return Collections.emptyList();
		List<Attribute> out = new ArrayList<>(list.size());
		for(Document doc : list) {
			out.add(new Attribute(doc));
		}
		return out;
	}
	
	/**
	 * Gets the instances.
	 *
	 * @return the instances
	 */
	public List<Instance> getInstances() {
		List<Document> list = content.getList(INSTANCES, Document.class);
		if(list == null) return Collections.emptyList();
		List<Instance> out = new ArrayList<>(list.size());
		for(Document doc : list) {
			out.add(new Instance(doc));
		}
		return out;
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
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
		
		/**
		 * Of.
		 *
		 * @param ref the ref
		 * @return the ref
		 */
		@CheckForNull
		public static Ref of(@Nullable String ref) {
			if(ref == null || ref.isBlank()) return null;
			return new Ref(ref);
		}
		
		/** The ref. */
		public final String ref;
		
		/**
		 * Instantiates a new ref.
		 *
		 * @param ref the ref
		 */
		public Ref(String ref) {
			this.ref = ref.startsWith("wkn:") ? TripleFunctions.NS_WIKI + ref.substring(4) : ref;
		}
		
		/**
		 * Checks if is draft.
		 *
		 * @return true, if is draft
		 */
		public boolean isDraft() {
			return Util.isObjectId(ref);
		}
		
		/**
		 * Checks if is wiki.
		 *
		 * @return true, if is wiki
		 */
		public boolean isWiki() {
			return !Util.isObjectId(ref);
		}
		
		/**
		 * As node.
		 *
		 * @return the node
		 */
		public Node asNode() {
			return NodeFactory.createURI(ref);
		}
		
		/**
		 * As object id.
		 *
		 * @return the object id
		 */
		public ObjectId asObjectId() {
			return new ObjectId(ref);
		}
		
		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return ref;
		}
		
		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return isDraft() ? asObjectId().hashCode() : asNode().hashCode();
		}
		
		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
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
		
		/** The doc. */
		private final Document doc;
		
		/**
		 * Instantiates a new attribute.
		 *
		 * @param doc the doc
		 */
		public Attribute(Document doc) {
			this.doc = doc;
		}
		
		/**
		 * Gets the identifier.
		 *
		 * @return the identifier
		 */
		public String getIdentifier() {
			return doc.getString(IDENTIFIER);
		}
		
		/**
		 * Gets the label.
		 *
		 * @return the label
		 */
		public String getLabel() {
			return doc.getString(LABEL);
		}
		
		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		public Ref getType() {
			return Ref.of(doc.getString(TYPE));
		}
		
		/**
		 * Gets the unit.
		 *
		 * @return the unit
		 */
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
		
		/** The doc. */
		private final Document doc;
		
		/**
		 * Instantiates a new attribute values.
		 *
		 * @param doc the doc
		 */
		public AttributeValues(Document doc) {
			this.doc = doc;
		}
		
		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		public Ref getType() {
			return Ref.of(doc.getString(TYPE));
		}
		
		/**
		 * Gets the values.
		 *
		 * @return the values
		 */
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
		
		/** The doc. */
		private final Document doc;
		
		/**
		 * Instantiates a new instance.
		 *
		 * @param doc the doc
		 */
		public Instance(Document doc) {
			this.doc = doc;
		}
		
		/**
		 * Gets the identifier.
		 *
		 * @return the identifier
		 */
		public String getIdentifier() {
			return doc.getString(IDENTIFIER);
		}
		
		/**
		 * Gets the label.
		 *
		 * @return the label
		 */
		public String getLabel() {
			return doc.getString(LABEL);
		}
		
		/**
		 * Gets the part of.
		 *
		 * @return the part of
		 */
		public Map<Ref, String> getPartOf() {
			List<Document> list = doc.getList(PARTOF, Document.class);
			if(list == null) return Collections.emptyMap();
			Map<Ref, String> out = new HashMap<>(list.size());
			for(Document doc : list) {
				out.put(Ref.of(doc.getString(VALUE)), doc.getString(IDENTIFIER));
			}
			return out;
		}
		
		/**
		 * Gets the attributes.
		 *
		 * @return the attributes
		 */
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
