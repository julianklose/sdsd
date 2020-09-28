package de.sdsd.projekt.prototype.applogic;

import static de.sdsd.projekt.prototype.Main.DEBUG_MODE;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import de.sdsd.projekt.prototype.applogic.TripleFunctions.QueryResult;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilQuerySolution;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.WikiAttr;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.WikiType;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.GeoElement;
import de.sdsd.projekt.prototype.data.GeoElement.ElementType;
import de.sdsd.projekt.prototype.data.User;

/**
 * Functions for finding equal existing elements.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class DuplicateFinderFunctions {
	private final ApplicationLogic app;
	
	DuplicateFinderFunctions(ApplicationLogic app) {
		this.app = app;
	}
	
	public void deleteFileRelations(User user, String fileUri) {
		ParameterizedSparqlString pss = new ParameterizedSparqlString(
				"DELETE { " + 
				"  GRAPH ?ug { " + 
				"    ?s ?sameAs ?o; " + 
				"       <sdsd:relatedTimeLog> ?o. " + 
				"  } " + 
				"} " + 
				"WHERE { " + 
				"  GRAPH ?ug { ?s ?sameAs|^?sameAs|<sdsd:relatedTimeLog>|^<sdsd:relatedTimeLog> ?o } " + 
				"  GRAPH ?fg { ?o ?type ?_ } " + 
				"}");
		pss.setParam("?sameAs", OWL.sameAs);
		pss.setParam("?type", RDF.type);
		pss.setIri("?ug", user.getGraphUri());
		pss.setIri("?fg", fileUri);
		app.triple.update(pss.asUpdate());
	}
	
	public void findDuplicates(User user, File file) {
		try {
			Model model = ModelFactory.createDefaultModel();
			fields(model, user, file);
//			timelogs(model, user, file);
			machines(model, user, file);
			if(model.size() > 0)
				app.triple.insertData(model, user.getGraphUri());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private static final Bson BOUNDARY_FILTER = GeoElement.filterType(GeoElement.ElementType.Field);
	private static final Var DUP=Var.alloc("dup");
	protected void fields(Model model, User user, File file) {
		for(GeoElement geo : app.geo.find(user, Filters.and(GeoElement.filterFile(file.getId()), BOUNDARY_FILTER))) {
			Resource field = ResourceFactory.createResource(geo.getUri());
			model.add(field, OWL.sameAs, field);
			List<Resource> dupfields = new ArrayList<>();
			for(GeoElement dup : app.geo.find(user, 
					Filters.and(Filters.ne(GeoElement.ID, geo.getId()), BOUNDARY_FILTER, geo.filterIntersects()))) {
				try {
					Resource dupfield = ResourceFactory.createResource(dup.getUri());
					if(dupfield.equals(field) || model.contains(field, OWL.sameAs, dupfield)) continue;
					if(!GeoFunctions.equals(geo, dup)) {
						if(DEBUG_MODE) System.out.format("Field %s(%s) intersects %s(%s), but is not equal\n", 
								field.getURI(), file.getURI(), dupfield.getURI(), File.toURI(dup.getFile()));
						continue;
					}
					
					for(Resource d : dupfields) { // create missing sameAs relations
						model.add(dupfield, OWL.sameAs, d);
						model.add(d, OWL.sameAs, dupfield);
					}

					model.add(field, OWL.sameAs, dupfield);
					model.add(dupfield, OWL.sameAs, field);
					try(QueryResult qr = app.triple.query(new SelectBuilder()
							.addVar(DUP)
							.from(user.getGraphUri())
							.addWhere(dupfield, OWL.sameAs, DUP)
							.build())) {
						for(UtilQuerySolution qs : qr.iterate()) {
							Resource d = qs.getResource(DUP);
							dupfields.add(d);
							model.add(field, OWL.sameAs, d);
							model.add(d, OWL.sameAs, field);
						}
					}

					if(DEBUG_MODE) System.out.format("Field %s(%s) equals %s(%s)\n", 
							field.getURI(), file.getURI(), dupfield.getURI(), File.toURI(dup.getFile()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static final Bson TLG_FILTER = GeoElement.filterType(GeoElement.ElementType.TimeLog);
	protected void timelogs(Model model, User user, File file) {
		for(GeoElement geo : app.geo.find(user, Filters.and(GeoElement.filterFile(file.getId())))) {
			if(geo.getType() == ElementType.TimeLog) {
				try {
					Resource tlg = ResourceFactory.createResource(geo.getUri());
					for(GeoElement dup : app.geo.find(user, 
							Filters.and(Filters.ne(GeoElement.ID, geo.getId()), BOUNDARY_FILTER, geo.filterIntersects()))) {
						try {
							Resource field = ResourceFactory.createResource(dup.getUri());
							if(field.equals(tlg)) continue;
							model.add(field, TripleFunctions.RELTIMELOG, tlg);
							if(DEBUG_MODE) System.out.format("Timelog %s(%s) is related to field %s(%s)\n", 
									tlg.getURI(), file.getURI(), field.getURI(), File.toURI(dup.getFile()));
						} catch (NoSuchElementException e) {}
					}
				} catch (NoSuchElementException e) {}
			} else if(geo.getType() == ElementType.Field) {
				try {
					Resource field = ResourceFactory.createResource(geo.getUri());
					for(GeoElement dup : app.geo.find(user, 
							Filters.and(Filters.ne(GeoElement.ID, geo.getId()), TLG_FILTER, geo.filterIntersects()))) {
						try {
							Resource tlg = ResourceFactory.createResource(dup.getUri());
							if(field.equals(tlg)) continue;
							model.add(field, TripleFunctions.RELTIMELOG, tlg);
							if(DEBUG_MODE) System.out.format("Timelog %s(%s) is related to field %s(%s)\n", 
									tlg.getURI(), File.toURI(dup.getFile()), field.getURI(), file.getURI());
						} catch (NoSuchElementException e) {}
					}
				} catch (NoSuchElementException e) {}
			}
		}
	}
	
	protected void machines(Model model, User user, File file) {
		Var MACHINE=Var.alloc("m"), CLIENTNAME=Var.alloc("c"), STRLABEL=Var.alloc("s"), LOCLABEL=Var.alloc("l"), 
				GRAPH=Var.alloc("g"), DUPMACHINE=Var.alloc("dm");
		WikiType dvc = new TripleFunctions.WikiFormat("isoxml").res("DVC");
		WikiAttr P_CLIENTNAME = dvc.prop("D"), P_STRLBAEL = dvc.prop("F"), P_LOCLABEL = dvc.prop("G");
		
		Query query = new SelectBuilder()
				.addVar(MACHINE).addVar(DUPMACHINE).addVar(GRAPH)
				.fromNamed(app.triple.getFileGraphs(user))
				.addGraph(NodeFactory.createURI(file.getURI()), new WhereBuilder()
						.addWhere(MACHINE, P_CLIENTNAME, CLIENTNAME)
						.addWhere(MACHINE, P_STRLBAEL, STRLABEL)
						.addWhere(MACHINE, P_LOCLABEL, LOCLABEL))
				.addGraph(GRAPH, new WhereBuilder()
						.addWhere(DUPMACHINE, P_CLIENTNAME, CLIENTNAME)
						.addWhere(DUPMACHINE, P_STRLBAEL, STRLABEL)
						.addWhere(DUPMACHINE, P_LOCLABEL, LOCLABEL))
				.build();
		
		try(QueryResult qr = app.triple.query(query)) {
			for(UtilQuerySolution qs : qr.iterate()) {
				Resource machine = qs.getResource(MACHINE);
				Resource dupmachine = qs.getResource(DUPMACHINE);
				
				model.add(machine, OWL.sameAs, dupmachine);
				if(dupmachine.equals(machine)) continue;
				model.add(dupmachine, OWL.sameAs, machine);
				if(DEBUG_MODE) System.out.format("Device %s(%s) equals %s(%s)\n", 
						machine.getURI(), file.getURI(), dupmachine.getURI(), qs.getUri(GRAPH));
			}
		}
	}
	
}
