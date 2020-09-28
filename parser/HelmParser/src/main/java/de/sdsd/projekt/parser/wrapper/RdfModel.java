package de.sdsd.projekt.parser.wrapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * Wrapper class for a (loosely connected) Jena RDF model, together with all the
 * referencing ID attributes and GPS coordinates.
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class RdfModel {
	private Model model;
	private List<IdRef> refs;
	private List<HelmCoordinate> coords;

	public RdfModel() {
		this.model = ModelFactory.createDefaultModel();
		this.refs = new ArrayList<IdRef>();
		this.coords = new ArrayList<HelmCoordinate>();
	}

	public boolean isEmpty() {
		return this.model == null || this.model.isEmpty();
	}

	public void addPartOfReference(IdRef ref) {
		this.refs.add(ref);
	}

	public void addCoordinate(HelmCoordinate coord) {
		this.coords.add(coord);
	}

	public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public List<IdRef> getRefs() {
		return refs;
	}

	public void setRefs(List<IdRef> refs) {
		this.refs = refs;
	}

	public List<HelmCoordinate> getCoords() {
		return coords;
	}

	public void setCoords(List<HelmCoordinate> coords) {
		this.coords = coords;
	}

	@Override
	public String toString() {
		return "RdfModel [model=" + model + ", refs=" + refs + ", coords=" + coords + "]";
	}
}