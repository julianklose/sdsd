package de.sdsd.projekt.prototype.data;

import static de.sdsd.projekt.prototype.data.Util.lit;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * Represents a generic wikinormia entry.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class WikiEntry implements Comparable<WikiEntry> {
	
	private static Resource getMainResource(Model model, String uri) throws NoSuchElementException {
		Resource res = model.getResource(uri);
		if (!model.containsResource(res))
			throw new NoSuchElementException("The given URI doesn't exist in the model");
		return res;
	}

	protected final String uri;
	protected final Optional<WikiEntry> type;
	protected final String identifier;
	protected String label;
	
	public WikiEntry(Model model, String uri) throws NoSuchElementException {
		this(getMainResource(model, uri));
	}
	
	public WikiEntry(Resource res) throws NoSuchElementException {
		this.uri = res.getURI();
		Statement stmt = res.getProperty(RDF.type);
		this.type = stmt != null ? Optional.of(new WikiEntry(stmt.getResource())) : Optional.empty();
		stmt = res.getProperty(DCTerms.identifier);
		this.identifier = stmt != null ? stmt.getString() : res.getLocalName();
		stmt = res.getProperty(RDFS.label);
		this.label = stmt != null ? stmt.getString() : identifier;
	}
	
	public WikiEntry(String uri, @Nullable WikiEntry type, String identifier, String label) throws NoSuchElementException {
		this.uri = uri;
		this.type = Optional.ofNullable(type);
		this.identifier = identifier;
		this.label = label;
	}
	
	public Resource res(@Nullable Model model) {
		return model != null ? model.createResource(uri) : ResourceFactory.createResource(uri);
	}

	public String getUri() {
		return uri;
	}
	
	public Optional<WikiEntry> getType() {
		return type;
	}
	
	public String getIdentifier() {
		return identifier;
	}

	public String getLabel() {
		return label;
	}
	
	public WikiEntry setLabel(String label) {
		this.label = label;
		return this;
	}
	
	public Resource writeTo(Model model) {
		Resource res = model.createResource(uri);
		if(type.isPresent()) res.addProperty(RDF.type, model.createResource(type.get().uri));
		res.addLiteral(DCTerms.identifier, lit(identifier));
		if(label != null) res.addLiteral(RDFS.label, lit(label));
		return res;
	}
	
	@Override
	public String toString() {
		return String.format("%s(%s)", label, identifier);
	}

	@Override
	public int compareTo(WikiEntry o) {
		return identifier.compareToIgnoreCase(o.identifier);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uri);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof WikiEntry))
			return false;
		WikiEntry other = (WikiEntry) obj;
		return Objects.equals(uri, other.uri);
	}
	
}
