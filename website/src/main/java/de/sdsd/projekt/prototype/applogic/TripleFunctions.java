package de.sdsd.projekt.prototype.applogic;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.RDFVisitor;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import de.sdsd.projekt.prototype.data.DDI;
import de.sdsd.projekt.prototype.data.DDI.DDICategory;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;
import de.sdsd.projekt.prototype.data.WikiClass;

/**
 * Provides functions for interacting with the triplestore.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class TripleFunctions implements Closeable {

	private final String sparqlQueryEndpoint;
	private final String sparqlUpdateEndpoint;
	private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
	private final ApplicationLogic app;
	
	private CloseableHttpClient client;
	
	private final static HttpRequestInterceptor ENCODING_FIXER = new HttpRequestInterceptor() {
		@Override
		public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
			if(request instanceof HttpEntityEnclosingRequest) {
				HttpEntityEnclosingRequest req = (HttpEntityEnclosingRequest)request;
				HttpEntity entity = req.getEntity();
				String content = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);
				StringEntity fixedEntity = new StringEntity(content, StandardCharsets.ISO_8859_1);
				fixedEntity.setContentType(entity.getContentType());
				req.setEntity(fixedEntity);
			}
		}
	};
	
	TripleFunctions(ApplicationLogic app) {
		this.app = app;
		JSONObject stardog = app.settings.getJSONObject("stardog");
		this.sparqlQueryEndpoint = stardog.getString("query");
		this.sparqlUpdateEndpoint = stardog.getString("update");
		Credentials credentials = new UsernamePasswordCredentials(stardog.getString("user"), stardog.getString("password"));
		credentialsProvider.setCredentials(AuthScope.ANY, credentials);
		recreateClient();
		
		insertDefaultsIntoWikinormia();
	}
	
	public void recreateClient() {
		try {
			if(client != null) 
				client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		client = HttpClients.custom()
				.setDefaultCredentialsProvider(credentialsProvider)
				.addInterceptorFirst(ENCODING_FIXER)
				.build();
	}
	
	private Map<Integer, DDI> ddis = null;
	private List<DDI.DDICategory> ddiCategories = null;
	
	private void readDDIs() {
		Resource base = ResourceFactory.createResource("https://app.sdsd-projekt.de/wikinormia.html?page=ddi");
		Var category = Var.alloc("cat"), catIdent = Var.alloc("catIdent"), catTitle = Var.alloc("catTitle"), catDesc = Var.alloc("catDesc");
		Var ddi = Var.alloc("ddi"), ddiIdent = Var.alloc("ddiIdent"), ddiTitle = Var.alloc("ddiTitle"), ddiDesc = Var.alloc("ddiDesc");
		
		Map<String, DDI.DDICategory> categories = new HashMap<>();
		List<DDI.DDICategory> ddiCategories = new ArrayList<>();
		Map<Integer, DDI> ddis = new HashMap<>();
		
		try(QueryResult res = query(new SelectBuilder()
				.addVar(category).addVar(catIdent).addVar(catTitle).addVar(catDesc)
				.from(TBOX)
				.addWhere(category, DCTerms.isPartOf, base)
				.addWhere(category, DCTerms.identifier, catIdent)
				.addWhere(category, RDFS.label, catTitle)
				.addWhere(category, RDFS.comment, catDesc)
				.addOrderBy(catIdent)
				.build())) {
			for (UtilQuerySolution qe : res.iterate()) {
				DDI.DDICategory cat = new DDI.DDICategory(Integer.parseInt(qe.getString(catIdent)), qe.getString(catTitle), qe.getString(catDesc));
				categories.put(qe.getUri(category), cat);
				ddiCategories.add(cat);
			}
		}
		try(QueryResult res = query(new SelectBuilder()
				.addVar(ddiIdent).addVar(ddiTitle).addVar(ddiDesc).addVar(category)
				.from(TBOX)
				.addWhere(ddi, DCTerms.isPartOf, category)
				.addWhere(category, DCTerms.isPartOf, base)
				.addWhere(ddi, DCTerms.identifier, ddiIdent)
				.addWhere(ddi, RDFS.label, ddiTitle)
				.addWhere(ddi, RDFS.comment, ddiDesc)
				.build())) {
			for (UtilQuerySolution qe : res.iterate()) {
				DDI.DDICategory cat = categories.get(qe.getUri(category));
				DDI d = new DDI(Integer.parseInt(qe.getString(ddiIdent)), qe.getString(ddiTitle), qe.getString(ddiDesc), cat);
				ddis.put(d.getDDI(), d);
			}
		}
		
		this.ddis = Collections.unmodifiableMap(ddis);
		this.ddiCategories = Collections.unmodifiableList(ddiCategories);
	}
	
	@Nonnull
	public DDI getDDI(int ddi) {
		if(ddis == null) readDDIs();
		DDI d = ddis.get(ddi);
		return d != null ? d : new DDI(ddi, String.format("%04X", ddi), "", ddiCategories.get(ddiCategories.size()-1));
	}
	
	public Map<Integer, DDI> getDDIs() {
		if(ddis == null) readDDIs();
		return ddis;
	}
	
	public List<DDICategory> getDDICategories() {
		if(ddiCategories == null) readDDIs();
		return ddiCategories;
	}
	
	public void insertData(Model model, String graphURI) {
		for(String prefix : model.getNsPrefixMap().keySet()) {
			model.removeNsPrefix(prefix);
		}
		StringWriter sw = new StringWriter();
		sw.write("INSERT DATA { GRAPH <"+ graphURI +"> { \n\n");
		model.write(sw, "TTL");
		sw.write("\n\n}}");
		UpdateRequest update = UpdateFactory.create(sw.toString());
		UpdateExecutionFactory.createRemote(update, sparqlUpdateEndpoint, client).execute();
	}
	
	public void updateFile(User user, File file) {
		Node graph = NodeFactory.createURI(user.getGraphUri()),
				fileNode = NodeFactory.createURI(file.getURI());
		Literal filename = ResourceFactory.createStringLiteral(file.getFilename());
		Var P = Var.alloc("p"), O = Var.alloc("o");
		UpdateRequest update = new UpdateBuilder()
				.with(graph)
				.addDelete(fileNode, P, O)
				.addInsert(fileNode, RDF.type, FILE)
				.addInsert(fileNode, FILE.prop("type"), NodeFactory.createURI(file.getType()))
				.addInsert(fileNode, FILE.prop("filename"), filename)
				.addInsert(fileNode, RDFS.label, filename)
				.addOptional(fileNode, P, O)
				.buildRequest();
		update(update);
	}
	
	public void deleteFile(User user, String fileUri) {
		try {
			deleteGraph(fileUri);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Node graph = NodeFactory.createURI(user.getGraphUri()),
				fileNode = NodeFactory.createURI(fileUri);
		Var P = Var.alloc("p"), O = Var.alloc("o");
		UpdateRequest update = new UpdateBuilder()
				.with(graph)
				.addDelete(fileNode, P, O)
				.addWhere(fileNode, P, O)
				.buildRequest();
		update(update);
	}
	
	public void clearAll(User user) {
		Var F = Var.alloc("f");
		Query query = new SelectBuilder()
				.addVar(F)
				.from(user.getGraphUri())
				.addWhere(F, RDF.type, FILE)
				.build();
		try(QueryResult qr = query(query)) {
			for(UtilQuerySolution qs : qr.iterate()) {
				try {
					deleteGraph(qs.getUri(F));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		try {
			deleteGraph(user.getGraphUri());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void deleteGraph(String graphURI) {
		UpdateRequest update = UpdateFactory.create("DROP SILENT GRAPH <" + graphURI + ">");
		UpdateExecutionFactory.createRemote(update, sparqlUpdateEndpoint, client).execute();
	}
	
	void tidyUp(Set<ObjectId> fileIds) {
		Collection<User> users = app.user.listUsers(false);
		
		Set<String> graphs = new HashSet<>(fileIds.size() + users.size() + 1);
		graphs.add(TBOX);
		for(ObjectId fileid : fileIds) {
			graphs.add(File.toURI(fileid));
		}
		for(User user : users) {
			graphs.add(user.getGraphUri());
		}
		
		Set<String> delete = new HashSet<>();
		try(QueryResult qr = query(QueryFactory.create("SELECT DISTINCT ?g WHERE { GRAPH ?g {} }"))) {
			for(QuerySolution qs : qr.iterate()) {
				String g = qs.getResource("?g").getURI();
				if(!graphs.contains(g))
					delete.add(g);
			}
		}
		
		System.out.println("Delete " + delete.size() + " graphs: " + delete.stream().collect(Collectors.joining(", ")));
		
		int i = 0;
		for(String g : delete) {
			System.out.format("Deleting graph %2d/%2d: %s...", ++i, delete.size(), g);
			try {
				deleteGraph(g);
				System.out.println("OK");
			} catch(Throwable e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	public Dataset constructGraph(String graphURI) {
		Query query = QueryFactory.create("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH ?g { ?s ?p ?o } } VALUES ?g { <"+ graphURI +"> }");
		QueryEngineHTTP qehttp = QueryExecutionFactory.createServiceRequest(sparqlQueryEndpoint, query, client);
		Dataset dataset = qehttp.execConstructDataset(); //in RAM
		qehttp.close();
		return dataset;
	}
	
	public Dataset constructAllOutIn(String from, String resourceURI, int offset, int limit) {
		Query query = QueryFactory.create("construct { ?s ?p1 ?o1 . ?s2 ?p2 ?s } " + from 
				+ " where { { ?s ?p1 ?o1 } union { ?s2 ?p2 ?s } } offset " + offset + " limit " + limit + " values ?s { <"+ resourceURI +"> }");
		QueryEngineHTTP qehttp = QueryExecutionFactory.createServiceRequest(sparqlQueryEndpoint, query, client);
		Dataset dataset = qehttp.execConstructDataset(); //in RAM
		qehttp.close();
		return dataset;
	}
	
	public QueryResult query(Query query) {
		return query(query, false);
	}
	
	public QueryResult query(Query query, boolean reasoning) {
		return new QueryResult(QueryExecutionFactory.createServiceRequest(sparqlQueryEndpoint + (reasoning ? "/reasoning" : ""), query, client));
	}
	
	public void update(Update update) {
		update(new UpdateRequest(update));
	}
	
	public void update(UpdateRequest update) {
		UpdateExecutionFactory.createRemote(update, sparqlUpdateEndpoint, client).execute();
	}
	
	public static final String TBOX = "https://app.sdsd-projekt.de/wikinormia/",
			NS_WIKI = "https://app.sdsd-projekt.de/wikinormia.html?page=",
			NS_INTERN = "sdsd:";
	public static final Node TBOX_N = NodeFactory.createURI(TBOX);
	
	public static boolean isWikiUri(String uri) {
		return uri.startsWith(NS_WIKI);
	}
	
	public static Resource createInternResource(String name) {
		return ResourceFactory.createResource(NS_INTERN + name);
	}
	public static Property createInternProperty(String name) {
		return ResourceFactory.createProperty(NS_INTERN + name);
	}
	
	public static String wkn(String page) {
		return NS_WIKI + page;
	}
	public static String createWikiResourceUri(String identifier) {
		return NS_WIKI + Util.toCamelCase(identifier, false);
	}
	public static String createWikiResourceUri(String formatUri, String identifier) {
		return formatUri.equals(FORMAT_UNKNOWN.getURI()) 
				? createWikiResourceUri(identifier) 
				: formatUri + Util.toCamelCase(identifier, true);
	}
	public static String createWikiPropertyUri(String typeUri, String identifier) {
		return typeUri + '#' + Util.toCamelCase(identifier, false);
	}
	public static String createWikiInstanceUri(String typeUri, String identifier) {
		return typeUri + '_' + Util.toCamelCase(identifier, false);
	}
	
	/**
	 * A general wikinormia resource.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class WikiRes extends ResourceImpl {
		protected WikiRes(String uri) {
			super(uri);
		}
		public static WikiRes page(String page) {
			int index = page.indexOf('#');
			if(index >= 0) page = page.substring(0, index);
			index = page.indexOf('_');
			return index < 0 ? FORMAT_UNKNOWN.res(page) 
					: new WikiInst(FORMAT_UNKNOWN.res(page.substring(0, index)), page.substring(index + 1));
		}
	}
	
	public static WikiFormat format(String identifier) {
		return new WikiFormat(identifier);
	}
	
	/**
	 * A wikinormia format resource.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class WikiFormat extends WikiRes {
		public WikiFormat(String identifier) {
			super(createWikiResourceUri(identifier));
		}
		public static WikiFormat fromUri(String uri) throws IllegalArgumentException {
			if(!uri.startsWith(NS_WIKI)) 
				throw new IllegalArgumentException("URI is no wikinormia uri");
			return new WikiFormat(uri.substring(NS_WIKI.length()));
		}
		public WikiType res(String identifier) {
			return new WikiType(this, identifier);
		}
	}

	/**
	 * A wikinormia class resource.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class WikiType extends WikiRes {
		public WikiType(WikiFormat format, String identifier) {
			super(createWikiResourceUri(format.getURI(), identifier));
		}
		public WikiAttr prop(String identifier) {
			return new WikiAttr(this, identifier);
		}
		public WikiInst inst(String identifier) {
			return new WikiInst(this, identifier);
		}
		public WikiInst inst(int identifier) {
			return new WikiInst(this, Integer.toString(identifier));
		}
	}

	/**
	 * A wikinormia attribute property.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class WikiAttr extends PropertyImpl {
		public WikiAttr(WikiType type, String identifier) {
			super(createWikiPropertyUri(type.getURI(), identifier));
		}
	}

	/**
	 * A wikinormia instance resource.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class WikiInst extends WikiRes {
		public WikiInst(WikiType type, String identifier) {
			super(createWikiInstanceUri(type.getURI(), identifier));
		}
	}
	
	public static final Property RELTIMELOG = createInternProperty("relatedTimeLog");
	public static final Property UNIT = createInternProperty("unit");
	
	public static final Path SUBCLASS = PathFactory.pathZeroOrMore1(Util.path(RDFS.subClassOf));
	public static final Path TYPE_SUBCLASS = subclass(RDF.type);
	public static final Path subclass(Property prop) {
		return subclass(Util.path(prop));
	}
	public static final Path subclass(Path prop) {
		return PathFactory.pathSeq(prop, SUBCLASS);
	}
	public static final Path BASECLASS = PathFactory.pathZeroOrMore1(PathFactory.pathInverse(Util.path(RDFS.subClassOf)));
	public static final Path baseclass(Property prop) {
		return baseclass(Util.path(prop));
	}
	public static final Path baseclass(Path prop) {
		return PathFactory.pathSeq(prop, BASECLASS);
	}
	public static final Path PARTOF = PathFactory.pathZeroOrMore1(Util.path(DCTerms.isPartOf));
	public static final Path partOf(Property prop) {
		return partOf(Util.path(prop));
	}
	public static final Path partOf(Path prop) {
		return PathFactory.pathSeq(PARTOF, prop);
	}
	
	public static final Expr groupConcat(boolean distinct, Var var, String separator) {
		return new ExprAggregator(null, AggregatorFactory.createGroupConcat(distinct, new ExprVar(var), separator, null));
	}
	
	private static final Var LABEL = Var.alloc("label");
	public Optional<String> getLabel(Resource res, String graph) {
		Query query = new SelectBuilder()
				.addVar(LABEL)
				.from(graph)
				.addWhere(res, RDFS.label, LABEL)
				.build();
		try(QueryResult qr = query(query)) {
			Optional<UtilQuerySolution> qs = qr.first();
			if(qs.isPresent())
				Optional.ofNullable(qs.get().getString(LABEL));
			return Optional.empty();
		}
	}
	public Optional<String> getLabel(Resource res, User user) {
		Query query = new SelectBuilder()
				.addVar(LABEL)
				.from(getGraphs(user))
				.addWhere(res, RDFS.label, LABEL)
				.build();
		try(QueryResult qr = query(query)) {
			Optional<UtilQuerySolution> qs = qr.first();
			if(qs.isPresent())
				Optional.ofNullable(qs.get().getString(LABEL));
			return Optional.empty();
		}
	}
	
	public JSONObject getFormatFromWikinormia(Resource base) {
		JSONObject format = new JSONObject();
		Var URI=Var.alloc("uri"), TAG=Var.alloc("tag"), 
				PROP=Var.alloc("prop"), ATTR=Var.alloc("attr"), TYPE=Var.alloc("type"), 
				PARENT=Var.alloc("parent");
		
		try(QueryResult select = query(new SelectBuilder()
				.addVar(URI).addVar(TAG)
				.from(TBOX)
				.addWhere(URI, RDFS.subClassOf, base)
				.addWhere(URI, DCTerms.identifier, TAG)
				.build())) {
			for(UtilQuerySolution qs : select.iterate()) {
				Resource uri = qs.getResource(URI);
				JSONObject clazz = new JSONObject()
						.put("uri", uri.getURI());
				
				try(QueryResult attrSelect = query(new SelectBuilder()
						.addVar(PROP).addVar(ATTR).addVar(TYPE)
						.from(TBOX)
						.addWhere(PROP, RDFS.domain, uri)
						.addWhere(PROP, DCTerms.identifier, ATTR)
						.addWhere(PROP, RDFS.range, TYPE)
						.build())) {
					JSONObject attribs = new JSONObject();
					for (UtilQuerySolution attrib : attrSelect.iterate()) {
						attribs.put(attrib.getString(ATTR), new JSONObject()
								.put("prop", attrib.getUri(PROP))
								.put("type", attrib.getUri(TYPE)));
					}
					clazz.put("attribs", attribs);
				}
				
				try(QueryResult parentSelect = query(new SelectBuilder()
						.addVar(PARENT)
						.from(TBOX)
						.addWhere(URI, DCTerms.isPartOf, PARENT)
						.build())) {
					clazz.put("parents", parentSelect.stream()
							.map(q -> q.getUri(PARENT))
							.collect(Util.toJSONArray()));
				}
				
				format.put(qs.getString(TAG), clazz);
			}
		}
		
		return format;
	}
	
	/**
	 * Information about a resource.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class ResourceInfo {
		public final String uri, graph, label;
		@CheckForNull
		public final String prefLabel;
		
		public ResourceInfo(String uri, String graph, @Nullable String label, @Nullable String prefLabel) {
			this.uri = uri;
			this.graph = graph;
			this.label = label;
			this.prefLabel = prefLabel;
		}
		
		public String getUri() {
			return uri;
		}
		public String getGraph() {
			return graph;
		}
		public String getLabel() {
			return label;
		}
		@CheckForNull
		public String getPrefLabel() {
			return prefLabel;
		}
		public boolean isPreferred() {
			return prefLabel != null;
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof ResourceInfo))
				return false;
			ResourceInfo other = (ResourceInfo) obj;
			return Objects.equals(uri, other.uri);
		}
	}
	
	/**
	 * A resource with sameAs relations.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class DeduplicatedResource {
		private final List<ResourceInfo> sameAs;
		
		public DeduplicatedResource(int sameAsCount) {
			this.sameAs = new ArrayList<>(sameAsCount);
		}
		
		public DeduplicatedResource addSameAs(ResourceResult res) {
			sameAs.add(res);
			return this;
		}
		
		public String getFirstUri() {
			return sameAs.get(0).getUri();
		}
		
		@CheckForNull
		public String getPreferredLabel() {
			return sameAs.get(0).getPrefLabel();
		}
		
		public List<ResourceInfo> getSameAs() {
			return Collections.unmodifiableList(sameAs);
		}
		
		public List<String> getUris() {
			return sameAs.stream()
					.map(ResourceInfo::getUri)
					.collect(Collectors.toList());
		}
		
		public LinkedHashSet<String> getLabels() {
			LinkedHashSet<String> labels = new LinkedHashSet<>(sameAs.size());
			for(ResourceInfo sa : sameAs) {
				if(sa.label == null || sa.label.isEmpty()) continue;
				labels.add(sa.label);
			}
			return labels;
			
		}
		
		public List<String> getGraphs() {
			return sameAs.stream()
					.map(ResourceInfo::getGraph)
					.collect(Collectors.toList());
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(getFirstUri());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof DeduplicatedResource))
				return false;
			DeduplicatedResource other = (DeduplicatedResource) obj;
			return Objects.equals(getFirstUri(), other.getFirstUri());
		}

		@Override
		public String toString() {
			return sameAs.stream()
					.map(r -> String.format("'%s': <%s> (<%s>)", r.getLabel(), r.getUri(), r.getGraph()))
					.collect(Collectors.joining(", "));
		}
	}
	
	/**
	 * A sameAs resource, that is comparable by existence of a preferred label and the file modification date.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class ResourceResult extends ResourceInfo implements Comparable<ResourceResult> {
		private final int index;
		public final List<String> sameAs = new ArrayList<>();
		
		public ResourceResult(String uri, String graph, @Nullable String label, int index, @Nullable String prefLabel) {
			super(uri, graph, label, prefLabel);
			this.index = index;
		}
		
		public void addSameAs(String sameAs) {
			this.sameAs.add(sameAs);
		}
		
		@Override
		public int compareTo(ResourceResult o) {
			if(prefLabel != null && o.prefLabel == null) return -1;
			if(prefLabel == null && o.prefLabel != null) return 1;
			return Integer.compare(index, o.index);
		}
	}
	
	public Map<String, DeduplicatedResource> getAll(User user, Resource type, List<String> filegraphs, @Nullable Set<Resource> resources) {
		Map<String, Integer> graphmap = new HashMap<>(filegraphs.size());
		for(int i = 0; i < filegraphs.size(); ++i) {
			graphmap.put(filegraphs.get(i), i);
		}
		
		Node userGraph = NodeFactory.createURI(user.getGraphUri());
		Var RES = Var.alloc("r"), LBL = Var.alloc("l"), GRP = Var.alloc("g"), SAMEAS = Var.alloc("sa"), T = Var.alloc("t"), PREF = Var.alloc("pl");
		SelectBuilder query = new SelectBuilder()
				.addVar(RES).addVar(GRP).addVar(LBL).addVar(SAMEAS).addVar(PREF)
				.fromNamed(filegraphs)
				.fromNamed(TBOX)
				.fromNamed(user.getGraphUri())
				.addGraph(TBOX_N, T, SUBCLASS, type)
				.addGraph(GRP, RES, RDF.type, T)
				.addOptional(new WhereBuilder().addGraph(GRP, RES, RDFS.label, LBL))
				.addGraph(userGraph, RES, OWL.sameAs, SAMEAS)
				.addOptional(new WhereBuilder().addGraph(userGraph, RES, SKOS.prefLabel, PREF));
		if(resources != null && !resources.isEmpty()) {
			Object[] values = resources.toArray();
			query.addValueVar(RES, values).addValueVar(SAMEAS, values);
		}
		
		Map<String, ResourceResult> results = new HashMap<>();
		try(QueryResult qr = query(query.build())) {
			for(UtilQuerySolution qs : qr.iterate()) {
				String uri = qs.getUri(RES);
				ResourceResult res = results.get(uri);
				if(res == null) {
					String graph = qs.getUri(GRP);
					results.put(uri, res = new ResourceResult(uri, graph, qs.getString(LBL), graphmap.get(graph), qs.getString(PREF)));
				}
				res.addSameAs(qs.getUri(SAMEAS));
			}
		}
		
		SameAsResolver sameAsResolver = new SameAsResolver(results.size());
		results.values().stream().sorted().forEachOrdered(sameAsResolver);

		return sameAsResolver.distinct;
	}
	
	/**
	 * Helper to find distict of a number of sameAs resources.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class SameAsResolver implements Consumer<ResourceResult> {
		public final Map<String, DeduplicatedResource> distinct;
		private final Map<String, DeduplicatedResource> sameAs;
		
		public SameAsResolver(int resourceCount) {
			this.distinct = new HashMap<>(resourceCount);
			this.sameAs = new HashMap<>(resourceCount);
		}

		@Override
		public void accept(ResourceResult res) {
			DeduplicatedResource dRes = sameAs.get(res.uri);
			if(dRes == null) {
				dRes = new DeduplicatedResource(res.sameAs.size());
				distinct.put(res.uri, dRes);
				for(String sa : res.sameAs) {
					sameAs.put(sa, dRes);
				}
			}
			dRes.addSameAs(res);
		}
		
	}
	
	public static final WikiFormat FORMAT_UNKNOWN = new WikiFormat("unknown"), SERVICERESULT = new WikiFormat("serviceresult");
	public static final WikiType DEFAULT_TYPES = new WikiType(FORMAT_UNKNOWN, "default"),
			FILE = new WikiType(FORMAT_UNKNOWN, "file"), 
			FIELD = new WikiType(FORMAT_UNKNOWN, "field"), 
			MACHINE = new WikiType(FORMAT_UNKNOWN, "machine"), 
			TIMELOG = new WikiType(FORMAT_UNKNOWN, "timelog"), 
			GRID = new WikiType(FORMAT_UNKNOWN, "grid"), 
			VALUEINFO = new WikiType(FORMAT_UNKNOWN, "valueinfo");
	
	public void insertDefaultsIntoWikinormia() {
		Model model = ModelFactory.createDefaultModel();
		
		WikiClass formatUnknown = new WikiClass(null, "unknown", "Unknown");
		formatUnknown.setComment("This datatype is always used if SDSD doesn't know the file type.").writeTo(model);
		
		WikiClass defaultTypes = new WikiClass(formatUnknown, "default", "Standard Typen");
		defaultTypes.writeTo(model);
		
		Map<String, WikiClass> xsd = new HashMap<>(6);
		
		BiConsumer<String, String> xsdAdd = (identifier, label) -> {
			WikiClass cls = new WikiClass("http://www.w3.org/2001/XMLSchema#" + identifier, formatUnknown, identifier, label)
					.addBase(defaultTypes);
			cls.writeTo(model);
			xsd.put(identifier, cls);
		};
		xsdAdd.accept("string", "Text");
		xsdAdd.accept("int", "Integer");
		xsdAdd.accept("long", "Long");
		xsdAdd.accept("float", "Float");
		xsdAdd.accept("double", "Double");
		xsdAdd.accept("boolean", "Boolean");
		xsdAdd.accept("dateTime", "DateTime");
		xsdAdd.accept("hexBinary", "Hex");
		
		defaultTypes.createPart("file", "File")
				.setComment("File access permissions")
				.addAttribute("filename", "Filename", xsd.get("string"))
				.addAttribute("type", "Wikinormia format", new WikiClass(null, "format", "Format"))
				.writeTo(model);
		WikiClass field = defaultTypes.createPart("field", "Field")
				.setComment("Base class for all fields, stored in SDSD");
		field.writeTo(model);
		defaultTypes.createPart("machine", "Machine")
				.setComment("Base class for machines known in SDSD")
				.writeTo(model);
		
		WikiClass timelog = defaultTypes.createPart("timelog", "TimeLog")
				.setComment("Base class of timelogs")
				.addAttribute("name", "Name", xsd.get("string"))
				.addAttribute("from", "From", xsd.get("dateTime"))
				.addAttribute("until", "Until", xsd.get("dateTime"))
				.addAttribute("count", "Count", xsd.get("int"));
		timelog.writeTo(model);
		WikiClass grid = defaultTypes.createPart("grid", "Grid")
				.setComment("Base class of grids")
				.addAttribute("name", "Name", xsd.get("string"));
		timelog.writeTo(model);
		defaultTypes.createPart("valueinfo", "ValueInfo")
				.setComment("Base class of value descriptions (valueUris) for timelogs and grids")
				.addPartOf(timelog).addPartOf(grid)
				.addAttribute("designator", "Designator", xsd.get("string"))
				.addAttribute("offset", "Offset", xsd.get("long"))
				.addAttribute("scale", "Scale", xsd.get("double"))
				.addAttribute("numberOfDecimals", "Number of decimals", xsd.get("int"))
				.addAttribute("unit", "Unit", xsd.get("string"))
				.writeTo(model);
		
		field.createSubType("applicationField", "Antragsfläche")
				.setComment("Basisklasse aller Felder aus Flächenanträgen.")
				.addAttribute("year", "Antragsjahr", xsd.get("int"))
				.addAttribute("number", "Schlagnummer", xsd.get("int"))
				.addAttribute("name", "Bezeichnung", xsd.get("string"))
				.addAttribute("flik", "FLIK", xsd.get("string"))
				.addAttribute("area", "Fläche (ha)", xsd.get("double"))
				.addAttribute("prevUsage", "Vorjahresnutzung", new WikiClass(formatUnknown, "kultur", "Kultur"))
				.addAttribute("usage", "Nutzung", new WikiClass(formatUnknown, "kultur", "Kultur"))
				.writeTo(model);
		
		new WikiClass(null, "bmp", "Bitmap Image").setComment("Bitmap Image").writeTo(model);
		new WikiClass(null, "jpeg", "JPEG Image").setComment("Joint Photographic Experts Group Image with lossy compression.").writeTo(model);
		new WikiClass(null, "png", "PNG Image").setComment("Portable Network Graphics Image with lossless compression.").writeTo(model);
		new WikiClass(null, "avi", "AVI Video").setComment("Audio Video Interleave multimedia container format.").writeTo(model);
		new WikiClass(null, "mp4", "MP4 Video").setComment("Moving Picture Experts Group Version 4 multimedia container format.").writeTo(model);
		new WikiClass(null, "wmv", "WMV Video").setComment("Windows Media Video video coding format.").writeTo(model);
		new WikiClass(null, "pdf", "PDF Document").setComment("The Portable Document Format (PDF) is a file format developed by Adobe in the 1990s to present documents, including text formatting and images, in a manner independent of application software, hardware, and operating systems.").writeTo(model);
		
		new WikiClass(null, "ttl", "Terse RDF Triple Language").setComment("Generic RDF triples. This format is appendable.").writeTo(model);
		new WikiClass(null, "serviceresult", "Service Result").setComment("This is the SDSD format for data import. Parsers must return this format.").writeTo(model);
		
		new WikiClass(null, "isoxml", "ISOXML").setComment("ISOXML base").writeTo(model);
		new WikiClass(null, "shape", "Shape").setComment("The shapefile format is a popular geospatial vector data format for geographic information system (GIS) software.").writeTo(model);
		new WikiClass(null, "efdiDeviceDescription", "EFDI Device Description").setComment("A protobuf message containing device descriptions.").writeTo(model);
		new WikiClass(null, "efdiTimelog", "EFDI Timelog").setComment("A protobuf message containing timelogs.").writeTo(model);
		new WikiClass(null, "gps", "GPS Info").setComment("This format is used to report the GPS position of a CU or virtual CU independend of an attached machine.").writeTo(model);
			
		insertData(model, TBOX);
		System.out.println("Written WikiNormia defaults to tbox");
	}
	
	public Set<String> getFileGraphs(User user) {
		return app.list.files.getList(user).stream()
				.map(File::getURI)
				.collect(Collectors.toSet());
	}
	
	public Set<String> getGraphs(User user) {
		Set<String> graphs = getFileGraphs(user);
		graphs.add(TBOX);
		graphs.add(user.getGraphUri());
		return graphs;
	}
	
	public static String createFromBlock(Set<String> graphs) {
		StringBuilder sb = new StringBuilder();
		graphs.forEach(g -> sb.append("FROM <").append(g).append("> "));
		return sb.toString();
	}
	
	@Override
	public void close() throws IOException {
		client.close();
	}

	
	/**
	 * The Result of a select, ask or construct query.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class QueryResult implements AutoCloseable {
		
		private final QueryEngineHTTP qe;
		
		public QueryResult(QueryEngineHTTP qe) {
			this.qe = qe;
		}
		
		public boolean ask() {
			return qe.execAsk();
		}
		
		public Model construct() {
			return qe.execConstruct();
		}
		
		public Optional<UtilQuerySolution> first() {
			ResultSet rs = qe.execSelect();
			if(rs.hasNext())
				return Optional.of(new UtilQuerySolution(rs.next()));
			else
				return Optional.empty();
		}
		
		public Stream<UtilQuerySolution> stream() {
			return Util.iteratorStream(qe.execSelect())
					.map(UtilQuerySolution::new);
		}
		
		public Iterable<UtilQuerySolution> iterate() {
			return () -> new ResultIterator(qe.execSelect());
		}
		
		private class ResultIterator implements Iterator<UtilQuerySolution> {
			private final ResultSet rs;
			public ResultIterator(ResultSet rs) {
				this.rs = rs;
			}

			@Override
			public boolean hasNext() {
				return rs.hasNext();
			}

			@Override
			public UtilQuerySolution next() {
				return new UtilQuerySolution(rs.next());
			}

		}
		
		public List<UtilQuerySolution> list() {
			return stream()
					.map(QueryResult::materialize)
					.collect(Collectors.toList());
		}
		
		private static UtilQuerySolution materialize(UtilQuerySolution qs) {
			Iterator<String> it = qs.varNames();
			while(it.hasNext()) {
				qs.get(it.next());
			}
			return qs;
		}

		@Override
		public void close() {
			qe.close();
		}
		
	}
	
	/**
	 * Base class for visitiors to read/convert triplestore objects.
	 * 
	 * @param <T> the result type.
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static interface UtilRDFVisitor<T> extends RDFVisitor {
		@Override
		T visitBlank(Resource r, AnonId id);
		@Override
		T visitURI(Resource r, String uri);
		@Override
		T visitLiteral(Literal l);
	}
	
	/**
	 * A result row of a triplestore select query.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class UtilQuerySolution implements QuerySolution {
		
		private final QuerySolution qs;
		
		public UtilQuerySolution(QuerySolution qs) {
			this.qs = qs;
		}

		@Override
		public RDFNode get(String varName) {
			return qs.get(varName);
		}
		public RDFNode get(Var var) {
			return qs.get(var.getVarName());
		}
		@SuppressWarnings("unchecked")
		public <T> T get(Var var, UtilRDFVisitor<T> rv) {
			return (T)get(var).visitWith(rv);
		}

		@Override
		public Resource getResource(String varName) {
			return qs.getResource(varName);
		}
		public Resource getResource(Var var) {
			return qs.getResource(var.getVarName());
		}
		public String getUri(Var var) {
			Resource resource = qs.getResource(var.getVarName());
			return resource == null ? null : resource.getURI();
		}

		@Override
		public Literal getLiteral(String varName) {
			return qs.getLiteral(varName);
		}
		public Literal getLiteral(Var var) {
			return qs.getLiteral(var.getVarName());
		}
		public String getString(Var var) throws UncheckedIOException {
			Literal literal = qs.getLiteral(var.getVarName());
			return literal == null ? null : literal.getString();
		}
		public Instant getDateTime(Var var) throws UncheckedIOException {
			Literal literal = qs.getLiteral(var.getVarName());
			return literal == null ? null : ((GregorianCalendar)literal.getValue()).toInstant();
		}
		public Object getLiteralValue(Var var) throws UncheckedIOException {
			Literal literal = qs.getLiteral(var.getVarName());
			return literal == null ? null : literal.getValue();
		}

		@Override
		public boolean contains(String varName) {
			return qs.contains(varName);
		}
		public boolean contains(Var var) {
			return qs.contains(var.getVarName());
		}

		@Override
		public Iterator<String> varNames() {
			return qs.varNames();
		}
	}
}
