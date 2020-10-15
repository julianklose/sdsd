package de.sdsd.projekt.prototype.data;

import static de.sdsd.projekt.prototype.data.Util.lit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import de.sdsd.projekt.prototype.applogic.TripleFunctions;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilRDFVisitor;

/**
 * Represents a wikinormia instance.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class WikiInstance extends WikiEntry {
	
	/** The comment. */
	private Optional<String> comment = Optional.empty();
	
	/** The part of. */
	private final Map<String, WikiEntry> partOf = new HashMap<>();
	
	/** The parts. */
	private final Map<String, WikiEntry> parts = new HashMap<>();
	
	/** The attributes. */
	private final Map<String, WikiAttributeValue> attributes = new HashMap<>();
	
	/**
	 * Instantiates a new wiki instance.
	 *
	 * @param model the model
	 * @param uri the uri
	 * @throws NoSuchElementException the no such element exception
	 */
	public WikiInstance(Model model, String uri) throws NoSuchElementException {
		super(model, uri);
		Resource res = model.getResource(uri);
		Statement stmt = res.getProperty(RDFS.comment);
		this.comment = stmt != null ? Optional.of(stmt.getString()) : Optional.empty();
		
		StmtIterator sit;
		ResIterator rit;
		
		sit = res.listProperties(DCTerms.isPartOf);
		try {
			while(sit.hasNext()) {
				Resource r = sit.next().getResource();
				this.partOf.put(r.getURI(), new WikiEntry(r));
			} 
		} finally {
			sit.close();
		}
		
		rit = model.listResourcesWithProperty(DCTerms.isPartOf, res);
		try {
			while(rit.hasNext()) {
				Resource r = rit.next();
				this.parts.put(r.getURI(), new WikiEntry(r));
			}
		} finally {
			rit.close();
		}
		
		rit = model.listResourcesWithProperty(RDFS.domain, type.get().res(model));
		try {
			while(rit.hasNext()) {
				Resource r = rit.next();
				this.attributes.put(r.getURI(), new WikiAttributeValue(new WikiAttribute(type.get(), r), model));
			}
		} finally {
			rit.close();
		}
	}
	
	/**
	 * Instantiates a new wiki instance.
	 *
	 * @param type the type
	 * @param identifier the identifier
	 * @param label the label
	 */
	public WikiInstance(WikiEntry type, String identifier, String label) {
		super(TripleFunctions.createWikiInstanceUri(type.uri, identifier), type, identifier, label);
	}
	
	/**
	 * Instantiates a new wiki instance.
	 *
	 * @param uri the uri
	 * @param type the type
	 * @param identifier the identifier
	 * @param label the label
	 */
	public WikiInstance(String uri, WikiEntry type, String identifier, String label) {
		super(uri, type, identifier, label);
	}
	
	/**
	 * Sets the label.
	 *
	 * @param label the label
	 * @return the wiki instance
	 */
	@Override
	public WikiInstance setLabel(String label) {
		this.label = label;
		return this;
	}
	
	/**
	 * Gets the comment.
	 *
	 * @return the comment
	 */
	public Optional<String> getComment() {
		return comment;
	}

	/**
	 * Sets the comment.
	 *
	 * @param comment the comment
	 * @return the wiki instance
	 */
	public WikiInstance setComment(@Nullable String comment) {
		this.comment = Optional.ofNullable(comment);
		return this;
	}

	/**
	 * Gets the part of.
	 *
	 * @return the part of
	 */
	public Collection<WikiEntry> getPartOf() {
		return Collections.unmodifiableCollection(partOf.values());
	}
	
	/**
	 * Adds the part of.
	 *
	 * @param parent the parent
	 * @return the wiki instance
	 */
	public WikiInstance addPartOf(WikiEntry parent) {
		partOf.put(parent.uri, parent);
		if(parent instanceof WikiInstance)
			((WikiInstance)parent).parts.put(this.uri, this);
		return this;
	}
	
	/**
	 * Removes the part of.
	 *
	 * @param uri the uri
	 * @return the wiki instance
	 */
	public WikiInstance removePartOf(String uri) {
		WikiEntry parent = partOf.remove(uri);
		if(parent instanceof WikiInstance)
			((WikiInstance)parent).parts.remove(this.uri);
		return this;
	}

	/**
	 * Gets the parts.
	 *
	 * @return the parts
	 */
	public Collection<WikiEntry> getParts() {
		return Collections.unmodifiableCollection(parts.values());
	}

	/**
	 * Gets the attributes.
	 *
	 * @return the attributes
	 */
	public Collection<WikiAttributeValue> getAttributes() {
		return Collections.unmodifiableCollection(attributes.values());
	}
	
	/**
	 * Adds the attribute.
	 *
	 * @param val the val
	 * @return the wiki instance
	 */
	public WikiInstance addAttribute(WikiAttributeValue val) {
		attributes.put(val.attr.getUri(), val);
		return this;
	}
	
	/**
	 * Adds the attribute.
	 *
	 * @param attr the attr
	 * @param obj the obj
	 * @return the wiki instance
	 */
	public WikiInstance addAttribute(WikiAttribute attr, RDFNode... obj) {
		return addAttribute(new WikiAttributeValue(attr, obj));
	}
	
	/**
	 * Adds the attribute.
	 *
	 * @param attr the attr
	 * @param obj the obj
	 * @return the wiki instance
	 */
	public WikiInstance addAttribute(WikiAttribute attr, Collection<RDFNode> obj) {
		return addAttribute(new WikiAttributeValue(attr, obj));
	}
	
	/**
	 * Removes the attribute.
	 *
	 * @param uri the uri
	 * @return the wiki instance
	 */
	public WikiInstance removeAttribute(String uri) {
		attributes.remove(uri);
		return this;
	}
	
	/**
	 * Write to.
	 *
	 * @param model the model
	 * @return the resource
	 */
	@Override
	public Resource writeTo(Model model) {
		Resource res = super.writeTo(model);
		if(comment.isPresent()) res.addLiteral(RDFS.comment, lit(comment.get()));
		for(WikiEntry we : partOf.values()) res.addProperty(DCTerms.isPartOf, we.res(model));
		for(WikiAttributeValue wav : attributes.values()) wav.writeTo(model);
		return res;
	}
	
	/**
	 * A wikinormia instance attribute value.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class Value {
		
		/** The obj. */
		public final RDFNode obj;
		
		/** The label. */
		public final String label;
		
		/** The uri. */
		@CheckForNull
		public final String uri;
		
		/**
		 * Instantiates a new value.
		 *
		 * @param obj the obj
		 * @param label the label
		 * @param uri the uri
		 */
		Value(RDFNode obj, String label, @Nullable String uri) {
			this.obj = obj;
			this.label = label;
			this.uri = uri;
		}
		
		/**
		 * Checks if is literal.
		 *
		 * @return true, if is literal
		 */
		public boolean isLiteral() {
			return obj instanceof Literal;
		}
		
		/**
		 * As literal.
		 *
		 * @return the literal
		 */
		public Literal asLiteral() {
			return (Literal)obj;
		}
		
		/**
		 * Checks if is resource.
		 *
		 * @return true, if is resource
		 */
		public boolean isResource() {
			return obj instanceof Resource;
		}
		
		/**
		 * As resource.
		 *
		 * @return the resource
		 */
		public Resource asResource() {
			return (Resource)obj;
		}
	}
	
	/** The Constant VALUE_VISITOR. */
	private static final UtilRDFVisitor<Value> VALUE_VISITOR = new UtilRDFVisitor<Value>() {
		@Override
		public Value visitURI(Resource r, String uri) {
			Statement labelstmt = r.getProperty(RDFS.label);
			return new Value(r, labelstmt != null ? labelstmt.getString() : r.toString(), uri);
		}
		
		@Override
		public Value visitLiteral(Literal l) {
			return new Value(l, l.getValue().toString(), null);
		}
		
		@Override
		public Value visitBlank(Resource r, AnonId id) {
			return new Value(r, id.toString(), null);
		}
	};
	
	/**
	 * Represents the values of a wikinormia instance attribute.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class WikiAttributeValue implements Comparable<WikiAttributeValue> {
		
		/** The attr. */
		public final WikiAttribute attr;
		
		/** The values. */
		protected final List<RDFNode> values = new ArrayList<>();

		/**
		 * Instantiates a new wiki attribute value.
		 *
		 * @param attr the attr
		 * @param model the model
		 * @throws NoSuchElementException the no such element exception
		 */
		public WikiAttributeValue(WikiAttribute attr, Model model) throws NoSuchElementException {
			this.attr = attr;
			StmtIterator stmtit = WikiInstance.this.res(model).listProperties(attr.prop(model));
			try {
				while(stmtit.hasNext()) {
					values.add(stmtit.next().getObject());
				}
			} finally {
				stmtit.close();
			}
		}
		
		/**
		 * Instantiates a new wiki attribute value.
		 *
		 * @param attr the attr
		 * @param obj the obj
		 */
		public WikiAttributeValue(WikiAttribute attr, RDFNode... obj) {
			this.attr = attr;
			for(RDFNode o : obj) {
				values.add(o);
			}
		}
		
		/**
		 * Instantiates a new wiki attribute value.
		 *
		 * @param attr the attr
		 * @param obj the obj
		 */
		public WikiAttributeValue(WikiAttribute attr, Collection<RDFNode> obj) {
			this.attr = attr;
			values.addAll(obj);
		}
		
		/**
		 * Gets the values.
		 *
		 * @return the values
		 */
		public Stream<Value> getValues() {
			return values.stream().map(v -> (Value) v.visitWith(VALUE_VISITOR));
		}
		
		/**
		 * Write to.
		 *
		 * @param model the model
		 * @return the resource
		 */
		public Resource writeTo(Model model) {
			Resource res = WikiInstance.this.res(model);
			Property p = attr.prop(model);
			for(RDFNode val : values) res.addProperty(p, val);
			return res(model);
		}

		/**
		 * Compare to.
		 *
		 * @param o the o
		 * @return the int
		 */
		@Override
		public int compareTo(WikiAttributeValue o) {
			return attr.compareTo(o.attr);
		}
		
	}
}
