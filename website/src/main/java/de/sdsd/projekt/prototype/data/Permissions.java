package de.sdsd.projekt.prototype.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.applogic.TripleFunctions;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.QueryResult;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilQuerySolution;

/**
 * Helper for saving and retrieving service permissions from/to the triplestore.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class Permissions {
	
	/** The Constant ACCESS_DENIED. */
	public static final SDSDException ACCESS_DENIED = new SDSDException("Access denied");
	
	/** The Constant RPermission. */
	//classes
	public static final Resource RPermission = TripleFunctions.createInternResource("Permission");
	
	/** The Constant RServiceInstance. */
	public static final Resource RServiceInstance = TripleFunctions.createInternResource("ServiceInstance");
	
	/** The Constant Pperm. */
	public static final Property Pperm = TripleFunctions.createInternProperty("hasPermission");
	
	/** The Constant Pabout. */
	public static final Property Pabout = TripleFunctions.createInternProperty("about");
	
	/** The Constant Pallow. */
	public static final Property Pallow = TripleFunctions.createInternProperty("allow");
	
	/** The Constant Pobject. */
	public static final Property Pobject = TripleFunctions.createInternProperty("object");
	
	/**
	 * Represents a service permission for a specific wikinormia information type.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class Permission {
		
		/** The Constant OBJ. */
		private static final Var ID=Var.alloc("per"), TYPE=Var.alloc("type"), NAME=Var.alloc("name"), DESC=Var.alloc("desc"), 
				ALLOW=Var.alloc("allow"), OBJLST=Var.alloc("objlist"), OBJ=Var.alloc("object");
		
		/**
		 * Creates the query.
		 *
		 * @param userGraphUri the user graph uri
		 * @param instance the instance
		 * @return the query
		 */
		static Query createQuery(String userGraphUri, Resource instance) {
			
			return new SelectBuilder()
					.addVar(ID).addVar(TYPE).addVar(NAME).addVar(DESC).addVar(ALLOW)
					.addVar(new ExprAggregator(OBJLST, AggregatorFactory.createGroupConcat(false, new ExprVar(OBJ), ",", null)), OBJLST)
					.from(TripleFunctions.TBOX).from(userGraphUri)
					.addWhere(instance, Pperm, ID)
					.addWhere(ID, Pabout, TYPE)
					.addWhere(TYPE, RDFS.label, NAME)
					.addOptional(TYPE, RDFS.comment, DESC)
					.addOptional(ID, Pallow, ALLOW)
					.addOptional(ID, Pobject, OBJ)
					.addGroupBy(ID).addGroupBy(TYPE).addGroupBy(NAME).addGroupBy(DESC).addGroupBy(ALLOW)
					.addOrderBy(TYPE)
					.build();
		}
		
		/**
		 * Creates the object query.
		 *
		 * @param userGraphUri the user graph uri
		 * @param instance the instance
		 * @param type the type
		 * @return the query
		 */
		static Query createObjectQuery(String userGraphUri, Resource instance, String type) {
			Node uri = NodeFactory.createURI(type);
			return new SelectBuilder()
					.addVar(OBJ)
					.from(userGraphUri)
					.addWhere(instance, Pperm, ID)
					.addWhere(ID, Pabout, uri)
					.addWhere(ID, Pobject, OBJ)
					.build();
		}
		
		/**
		 * Read objects.
		 *
		 * @param stream the stream
		 * @return the sets the
		 */
		static Set<Resource> readObjects(Stream<UtilQuerySolution> stream) {
			return stream
					.map(qs -> qs.getResource(OBJ))
					.collect(Collectors.toSet());
		}
		
		/** The id. */
		private final String id;
		
		/** The description. */
		private final String type, name, description;
		
		/** The allowed. */
		private final boolean allowed;
		
		/** The objs. */
		private final String[] objs;
		
		/**
		 * Instantiates a new permission.
		 *
		 * @param qs the qs
		 */
		Permission(UtilQuerySolution qs) {
			this.id = qs.getResource(ID).getURI();
			this.type = qs.getResource(TYPE).getURI();
			this.name = qs.getLiteral(NAME).getString();
			Literal l = qs.getLiteral(DESC);
			this.description = l != null ? l.getString() : "";
			this.allowed = qs.contains(ALLOW);
			String objlst = qs.getLiteral(OBJLST).getString();
			this.objs = objlst.isEmpty() ? new String[0] : objlst.split(",");
		}
		
		/**
		 * Can read.
		 *
		 * @param qs the qs
		 * @return true, if successful
		 */
		static boolean canRead(UtilQuerySolution qs) {
			return qs.contains(ID);
		}

		/**
		 * Gets the id.
		 *
		 * @return the id
		 */
		public String getId() {
			return id;
		}
		
		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		public String getType() {
			return type;
		}
		
		/**
		 * Checks if is type GPS.
		 *
		 * @return true, if is type GPS
		 */
		public boolean isTypeGPS() {
			return GPS.equals(type);
		}
		
		/**
		 * Checks if is type file.
		 *
		 * @return true, if is type file
		 */
		public boolean isTypeFile() {
			return FILE.equals(type);
		}

		/**
		 * Gets the name.
		 *
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Gets the description.
		 *
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}

		/**
		 * Checks if is allowed.
		 *
		 * @return true, if is allowed
		 */
		public boolean isAllowed() {
			return allowed;
		}

		/**
		 * Gets the objects.
		 *
		 * @return the objects
		 */
		public String[] getObjects() {
			return objs;
		}
	}
	
	/** The app. */
	private final ApplicationLogic app;
	
	/** The instance. */
	private final ServiceInstance instance;
	
	/** The instance res. */
	private final Resource instanceRes;
	
	/** The user. */
	private final User user;
	
	/**
	 * Instantiates a new permissions.
	 *
	 * @param app the app
	 * @param instance the instance
	 */
	public Permissions(ApplicationLogic app, ServiceInstance instance) {
		this.app = app;
		this.instance = instance;
		this.instanceRes = ResourceFactory.createResource("serviceInst:" + instance.getId().toHexString());
		this.user = app.user.getUser(instance.getUser());
	}
	
	/**
	 * Creates the new.
	 *
	 * @param accessUris the access uris
	 */
	public void createNew(Set<String> accessUris) {
		//create model
		Model model = ModelFactory.createDefaultModel();
		
		model.add(instanceRes, RDF.type, RServiceInstance);
		
		//for each wikinormia uri a permission
		for(String wikinormiaUri : accessUris) {
			Resource permissionInstance = ResourceFactory.createResource(Util.createUuidUri());
			model.add(permissionInstance, RDF.type, RPermission);
			model.add(permissionInstance, Pabout, ResourceFactory.createResource(wikinormiaUri));
			model.add(instanceRes, Pperm, permissionInstance);
		}
		
		//TODO only debug
//		model.write(System.out, "TTL");
		
		//we will add to users graph
		app.triple.insertData(model, user.getGraphUri());
	}
	
	/**
	 * Delete instance.
	 */
	public void deleteInstance() {
		ParameterizedSparqlString pss = new ParameterizedSparqlString(
				"WITH ?user " + 
				"DELETE { ?inst ?pi ?oi. ?per ?pp ?op } " + 
				"WHERE { " + 
				"  ?inst a ?rinst. ?inst ?pi ?oi. " + 
				"  OPTIONAL { ?inst ?pperm ?per. ?per ?pp ?op } " + 
				"}"
		);
		
		pss.setIri("?user", user.getGraphUri());
		pss.setParam("?rinst", RServiceInstance);
		pss.setParam("?pperm", Pperm);
		pss.setParam("?inst", instanceRes);
		
		app.triple.update(pss.asUpdate());
	}
	
	/**
	 * Gets the permissions.
	 *
	 * @return the permissions
	 */
	public List<Permission> getPermissions() {
		try(QueryResult qr = app.triple.query(Permission.createQuery(user.getGraphUri(), instanceRes))) {
			return qr.stream()
					.filter(Permission::canRead)
					.map(Permission::new)
					.collect(Collectors.toList());
		}
	}
	
	/**
	 * Gets the permission objects.
	 *
	 * @param accessTypeUri the access type uri
	 * @return the permission objects
	 */
	public Set<Resource> getPermissionObjects(String accessTypeUri) {
		Query query = Permission.createObjectQuery(user.getGraphUri(), instanceRes, accessTypeUri);
		try(QueryResult qr = app.triple.query(query)) {
			return Permission.readObjects(qr.stream());
		}
	}
	
	/**
	 * Gets the files.
	 *
	 * @param filter the filter
	 * @return the files
	 * @throws SDSDException the SDSD exception
	 */
	public List<File> getFiles(@Nullable Bson filter) throws SDSDException {
		List<File> files = filter == null ? app.list.files.getList(user) : app.list.files.get(user, filter);
		if(files.isEmpty()) return files;
		if(instance.hasTimePermission()) {
			List<File> permitted = new ArrayList<>(files.size());

			for(File f : files) {
				Instant from = instance.getPermissionFrom(), until = instance.getPermissionUntil();
				if(from != null && f.getCreated().isBefore(from))
					continue;
				if(until != null && f.getCreated().isAfter(until))
					continue;
				permitted.add(f);
			}

			if(permitted.isEmpty()) throw ACCESS_DENIED;
			return permitted;
		}
		return files;
	}
	
	/**
	 * Gets the files.
	 *
	 * @return the files
	 * @throws SDSDException the SDSD exception
	 */
	public List<File> getFiles() throws SDSDException {
		return getFiles((Bson)null);
	}
	
	/**
	 * Gets the files.
	 *
	 * @param fileUri the file uri
	 * @return the files
	 * @throws SDSDException the SDSD exception
	 */
	public List<File> getFiles(@Nullable String fileUri) throws SDSDException {
		return getFiles(fileUri != null ? Filters.eq(File.toID(fileUri)) : null);
	}
	
	/**
	 * Gets the file graphs.
	 *
	 * @param fileUri the file uri
	 * @return the file graphs
	 * @throws SDSDException the SDSD exception
	 */
	public List<String> getFileGraphs(@Nullable String fileUri) throws SDSDException {
		List<File> files = getFiles(fileUri);
		List<String> graphs = new ArrayList<>(files.size());
		for(File f : files) {
			graphs.add(f.getURI());
		}
		return graphs;
	}
	
	/**
	 * Gets the graphs.
	 *
	 * @param fileUri the file uri
	 * @return the graphs
	 * @throws SDSDException the SDSD exception
	 */
	public List<String> getGraphs(@Nullable String fileUri) throws SDSDException {
		List<File> files = getFiles(fileUri);
		List<String> graphs = new ArrayList<>(files.size() + 2);
		graphs.add(user.getGraphUri());
		graphs.add(TripleFunctions.TBOX);
		for(File f : files) {
			graphs.add(f.getURI());
		}
		return graphs;
	}
	
	/** The Constant Vobj. */
	private static final Var Vs = Var.alloc("s"), Vperm = Var.alloc("perm"), Vtype = Var.alloc("type"), Vallow = Var.alloc("allow"), Vobj = Var.alloc("obj");
	
	/** The Constant EXPR. */
	private static final ExprFactory EXPR = new ExprFactory();
	
	/**
	 * Filter allowed.
	 *
	 * @param graphs the graphs
	 * @param accessUri the access uri
	 * @return the sets the
	 */
	public Set<Node> filterAllowed(List<String> graphs, Node... accessUri) {
		if(accessUri.length == 0) return Collections.emptySet();
		Query query = new SelectBuilder()
				.addVar(Vs)
				.from(graphs)
				.addWhere(instanceRes, Pperm, Vperm)
				.addWhere(Vperm, Pabout, Vtype)
				.addWhere(Vperm, Pallow, Vallow)
				.addWhere(Vs, TripleFunctions.subclass(TripleFunctions.partOf(RDF.type)), Vtype)
				.addOptional(Vperm, Pobject, Vobj)
				.addFilter(EXPR.cond(EXPR.bound(Vobj), EXPR.eq(Vs, Vobj), EXPR.asExpr(true)))
				.addValueVar(Vs, (Object[])accessUri)
				.build();
		
		Set<Node> allowed = new HashSet<>(accessUri.length);
		try(QueryResult qr = app.triple.query(query)) {
			for(UtilQuerySolution qs : qr.iterate()) {
				allowed.add(qs.getResource(Vs).asNode());
			}
		}
		return allowed;
	}
	
	/**
	 * Filter allowed.
	 *
	 * @param graphs the graphs
	 * @param accessUris the access uris
	 * @return the sets the
	 */
	public Set<Node> filterAllowed(List<String> graphs, Collection<Node> accessUris) {
		return filterAllowed(graphs, accessUris.toArray(new Node[accessUris.size()]));
	}
	
	/**
	 * Checks if is allowed.
	 *
	 * @param accessTypeUri the access type uri
	 * @return true, if is allowed
	 */
	public boolean isAllowed(String accessTypeUri) {
		Query query = new AskBuilder()
				.from(user.getGraphUri())
				.from(TripleFunctions.TBOX)
				.addWhere(instanceRes, Pperm, Vperm)
				.addWhere(Vperm, Pallow, Vallow)
				.addWhere(Vperm, TripleFunctions.subclass(Pabout), NodeFactory.createURI(accessTypeUri))
				.build();
		try(QueryResult qr = app.triple.query(query)) {
			return qr.ask();
		}
	}
	
	/** The Constant FILE. */
	public static final String GPS = TripleFunctions.NS_WIKI + "DdiGroupGPSGeoPosition", FILE = TripleFunctions.NS_WIKI + "file";
	
	/**
	 * Checks if is GPS allowed.
	 *
	 * @return true, if is GPS allowed
	 */
	public boolean isGPSAllowed() {
		return isAllowed(GPS);
	}
	
	/**
	 * Checks if is file allowed.
	 *
	 * @return true, if is file allowed
	 */
	public boolean isFileAllowed() {
		return isAllowed(FILE);
	}
	
	/**
	 * Sets the all permissions.
	 *
	 * @param allow the new all permissions
	 */
	public void setAllPermissions(boolean allow) {
		ParameterizedSparqlString pss = new ParameterizedSparqlString(
				"WITH ?user " + 
				(allow ? "INSERT { ?per ?pallow true } " : "DELETE { ?per ?pallow ?_ } ") + 
				"WHERE { ?inst ?pperm ?per. OPTIONAL { ?per ?pallow ?_ } }"
		);
		
		pss.setIri("?user", user.getGraphUri());
		pss.setParam("?pallow", Pallow);
		pss.setParam("?pperm", Pperm);
		pss.setParam("?inst", instanceRes);
		
		app.triple.update(pss.asUpdate());
	}
	
	/**
	 * Sets the permission.
	 *
	 * @param permissionId the permission id
	 * @param allow the allow
	 * @param objects the objects
	 */
	public void setPermission(String permissionId, boolean allow, Collection<String> objects) {
		ParameterizedSparqlString pss = new ParameterizedSparqlString(
				"WITH ?user " + 
				"DELETE { ?per ?pobj ?old. "
		);
		if(!allow)
			pss.append("?per ?pallow ?allow. ");
		if(allow || objects.size() > 0) {
			pss.append("} INSERT { ");
			if(allow) 
				pss.append("?per ?pallow true. ");
			if(objects.size() > 0) 
				pss.append("?per ?pobj ?new. ");
		}
		pss.append("} WHERE { ?inst ?pperm ?per. OPTIONAL { ?per ?pobj ?old } ");
		if(!allow)
			pss.append("OPTIONAL { ?per ?pallow ?allow } ");
		if(objects.size() > 0) {
			pss.append("VALUES ?new {");
			objects.forEach(o -> pss.appendIri(o));
			pss.append('}');
		}
		pss.append('}');
		
		pss.setIri("?user", user.getGraphUri());
		pss.setParam("?pobj", Pobject);
		pss.setParam("?pallow", Pallow);
		pss.setParam("?pperm", Pperm);
		pss.setParam("?inst", instanceRes);
		pss.setIri("?per", permissionId);
		
		app.triple.update(pss.asUpdate());
	}
	
}
