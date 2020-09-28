package de.sdsd.projekt.parser.wrapper;

import org.apache.jena.rdf.model.Resource;

/**
 * Wrapper class for referencing ID attributes associated with their
 * corresponding, randomly generated UriResource identifier
 * {@code referncingResource}.
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class IdRef {
	private Id id;
	private Resource referencingResource;

	public IdRef(Id id, Resource referencingResource) {
		this.id = id;
		this.referencingResource = referencingResource;
	}

	public Id getId() {
		return id;
	}

	public Resource getReferencingResource() {
		return referencingResource;
	}

	@Override
	public String toString() {
		return "IdRef [id=" + id + ", referencingResource=" + referencingResource + "]";
	}
}