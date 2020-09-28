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
	private Optional<String> comment = Optional.empty();
	private final Map<String, WikiEntry> partOf = new HashMap<>();
	private final Map<String, WikiEntry> parts = new HashMap<>();
	private final Map<String, WikiAttributeValue> attributes = new HashMap<>();
	
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
	
	public WikiInstance(WikiEntry type, String identifier, String label) {
		super(TripleFunctions.createWikiInstanceUri(type.uri, identifier), type, identifier, label);
	}
	
	public WikiInstance(String uri, WikiEntry type, String identifier, String label) {
		super(uri, type, identifier, label);
	}
	
	@Override
	public WikiInstance setLabel(String label) {
		this.label = label;
		return this;
	}
	
	public Optional<String> getComment() {
		return comment;
	}

	public WikiInstance setComment(@Nullable String comment) {
		this.comment = Optional.ofNullable(comment);
		return this;
	}

	public Collection<WikiEntry> getPartOf() {
		return Collections.unmodifiableCollection(partOf.values());
	}
	
	public WikiInstance addPartOf(WikiEntry parent) {
		partOf.put(parent.uri, parent);
		if(parent instanceof WikiInstance)
			((WikiInstance)parent).parts.put(this.uri, this);
		return this;
	}
	
	public WikiInstance removePartOf(String uri) {
		WikiEntry parent = partOf.remove(uri);
		if(parent instanceof WikiInstance)
			((WikiInstance)parent).parts.remove(this.uri);
		return this;
	}

	public Collection<WikiEntry> getParts() {
		return Collections.unmodifiableCollection(parts.values());
	}

	public Collection<WikiAttributeValue> getAttributes() {
		return Collections.unmodifiableCollection(attributes.values());
	}
	
	public WikiInstance addAttribute(WikiAttributeValue val) {
		attributes.put(val.attr.getUri(), val);
		return this;
	}
	public WikiInstance addAttribute(WikiAttribute attr, RDFNode... obj) {
		return addAttribute(new WikiAttributeValue(attr, obj));
	}
	public WikiInstance addAttribute(WikiAttribute attr, Collection<RDFNode> obj) {
		return addAttribute(new WikiAttributeValue(attr, obj));
	}
	
	public WikiInstance removeAttribute(String uri) {
		attributes.remove(uri);
		return this;
	}
	
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
		public final RDFNode obj;
		public final String label;
		@CheckForNull
		public final String uri;
		
		Value(RDFNode obj, String label, @Nullable String uri) {
			this.obj = obj;
			this.label = label;
			this.uri = uri;
		}
		
		public boolean isLiteral() {
			return obj instanceof Literal;
		}
		
		public Literal asLiteral() {
			return (Literal)obj;
		}
		
		public boolean isResource() {
			return obj instanceof Resource;
		}
		
		public Resource asResource() {
			return (Resource)obj;
		}
	}
	
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
		
		public final WikiAttribute attr;
		protected final List<RDFNode> values = new ArrayList<>();

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
		
		public WikiAttributeValue(WikiAttribute attr, RDFNode... obj) {
			this.attr = attr;
			for(RDFNode o : obj) {
				values.add(o);
			}
		}
		public WikiAttributeValue(WikiAttribute attr, Collection<RDFNode> obj) {
			this.attr = attr;
			values.addAll(obj);
		}
		
		public Stream<Value> getValues() {
			return values.stream().map(v -> (Value) v.visitWith(VALUE_VISITOR));
		}
		
		public Resource writeTo(Model model) {
			Resource res = WikiInstance.this.res(model);
			Property p = attr.prop(model);
			for(RDFNode val : values) res.addProperty(p, val);
			return res(model);
		}

		@Override
		public int compareTo(WikiAttributeValue o) {
			return attr.compareTo(o.attr);
		}
		
	}
}
