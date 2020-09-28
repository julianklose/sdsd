package de.sdsd.projekt.prototype.jsonrpc;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.update.Update;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.client.model.Filters;

import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.api.IsoZipOutputStream;
import de.sdsd.projekt.api.isoxml.IsoxmlCreator;
import de.sdsd.projekt.api.isoxml.Partfield.LineString;
import de.sdsd.projekt.api.isoxml.Partfield.LineStringType;
import de.sdsd.projekt.api.isoxml.Partfield.Polygon;
import de.sdsd.projekt.api.isoxml.Partfield.PolygonType;
import de.sdsd.projekt.api.isoxml.Point.PointType;
import de.sdsd.projekt.api.isoxml.Point.XmlPoint;
import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.QueryResult;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.ResourceInfo;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilQuerySolution;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.GeoElement;
import de.sdsd.projekt.prototype.data.GeoElement.ElementType;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;

/**
 * JSONRPC-Endpoint for field boundary functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class FeldgrenzenEndpoint extends JsonRpcEndpoint {

	public FeldgrenzenEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	/**
	 * Class for sorting resources.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class ResourceResult extends ResourceInfo implements Comparable<ResourceResult> {
		public final int index;

		public ResourceResult(String uri, String graph, @Nullable String label, @Nullable String prefLabel, int index) {
			super(uri, graph, label, prefLabel);
			this.index = index;
		}

		@Override
		public int compareTo(ResourceResult o) {
			if(prefLabel != null && o.prefLabel == null) return -1;
			if(prefLabel == null && o.prefLabel != null) return 1;
			int cmp = Integer.compare(index, o.index);
			if(cmp != 0) return cmp;
			if(label != null && o.label != null) {
				cmp = label.compareToIgnoreCase(o.label);
				if(cmp != 0) return cmp;
			}
			return uri.compareTo(o.uri);
		}
		
	}
	
	public JSONObject getSameAs(HttpServletRequest req, String uri) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("getSameAs: user(%s) uri(%s)\n", user != null ? user.getName() : "none", uri);
			
			if (user == null) 
				throw new NoLoginException();
			else {
				List<File> files = application.list.files.getList(user);
				files.sort(File.CMP_RECENT_CORE);
				Map<String, Integer> graphs = new HashMap<>();
				for(int i = 0; i < files.size(); ++i) {
					File f = files.get(i);
					graphs.put(f.getURI(), i);
				}
				
				Node userGraph = NodeFactory.createURI(user.getGraphUri());
				Var LBL = Var.alloc("l"), GRP = Var.alloc("g"), SAMEAS = Var.alloc("sa"), PREF = Var.alloc("pl");
				SelectBuilder query = new SelectBuilder()
						.addVar(GRP).addVar(LBL).addVar(SAMEAS).addVar(PREF)
						.fromNamed(graphs.keySet())
						.fromNamed(user.getGraphUri())
						.addGraph(userGraph, NodeFactory.createURI(uri), OWL.sameAs, SAMEAS)
						.addOptional(new WhereBuilder().addGraph(userGraph, SAMEAS, SKOS.prefLabel, PREF))
						.addGraph(GRP, SAMEAS, RDF.type, Var.ANON)
						.addOptional(new WhereBuilder().addGraph(GRP, SAMEAS, RDFS.label, LBL));
				
				TreeMap<ResourceResult, String> fields = new TreeMap<>();
				try(QueryResult qr = application.triple.query(query.build())) {
					for(UtilQuerySolution qs : qr.iterate()) {
						String g = qs.getUri(GRP);
						ResourceResult result = new ResourceResult(qs.getUri(SAMEAS), g, qs.getString(LBL), qs.getString(PREF), graphs.get(g));
						fields.put(result, result.uri);
					}
				}

				List<GeoElement> geolist = application.geo.find(user, Filters.and(
						Filters.in(GeoElement.URI, fields.values()), 
						GeoElement.filterType(ElementType.Field)));
				Map<String, GeoElement> geos = new HashMap<>(geolist.size());
				for(GeoElement geo : geolist) {
					geos.put(geo.getUri(), geo);
				}
				
				JSONArray list = new JSONArray();
				for(ResourceResult res : fields.keySet()) {
					GeoElement geo = geos.get(res.getUri());
					if(geo == null) continue;

					JSONObject props = new JSONObject()
							.put("uri", res.getUri())
							.put("graph", res.getGraph())
							.put("label", res.getLabel())
							.put("geolabel", geo.getLabel())
							.put("prefLabel", res.getPrefLabel())
							.put("area", Double.isFinite(geo.getArea()) ? geo.getArea() : null);
					
					list.put(geo.getFeatureJson()
							.put("properties", props));
				}

				return new JSONObject().put("sameAs", list);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject setPreferred(HttpServletRequest req, String uri, String prefLabel) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("setPreferred: user(%s) uri(%s) prefLabel(%s)\n", user != null ? user.getName() : "none", uri, prefLabel);
			
			if (user == null) 
				throw new NoLoginException();
			else {
				if(prefLabel == null) prefLabel = "";
				Node res = NodeFactory.createURI(uri);
				Var SAMEAS = Var.alloc("sa"), PREF = Var.alloc("pl");
				Update update = new UpdateBuilder()
						.with(NodeFactory.createURI(user.getGraphUri()))
						.addDelete(SAMEAS, SKOS.prefLabel, PREF)
						.addInsert(res, SKOS.prefLabel, Util.lit(prefLabel))
						.addWhere(res, OWL.sameAs, SAMEAS)
						.addOptional(SAMEAS, SKOS.prefLabel, PREF)
						.build();
				
				application.triple.update(update);
				return success(true);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject createIsoxml(HttpServletRequest req, String uri, @Nullable String name) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("createIsoxml: user(%s) uri(%s) name(%s)\n", user != null ? user.getName() : "none", uri, name);
			
			if (user == null) 
				throw new NoLoginException();
			else {
				GeoElement geo = application.geo.find(user, 
						Filters.and(GeoElement.filterUri(uri), GeoElement.filterType(ElementType.Field))).get(0);
				if(name.isBlank()) name = geo.getLabel();
				
				IsoxmlCreator creator = new IsoxmlCreator("SDSD");
				Polygon pln = creator.root.addPartfield(name, Math.round(geo.getArea()))
						.addPolygon(PolygonType.PARTFIELD_BOUNDARY);
				
				JSONArray rings = geo.getGeometryJson().getJSONArray("coordinates");
				writeLineString(pln.addRing(LineStringType.POLYGON_EXTERIOR), rings.getJSONArray(0));
				for(int i = 1; i < rings.length(); ++i) {
					writeLineString(pln.addRing(LineStringType.POLYGON_INTERIOR), rings.getJSONArray(i));
				}
				
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				try(IsoZipOutputStream zip = new IsoZipOutputStream(out)) {
					zip.putNextEntry(new ZipEntry("TASKDATA.xml"));
					creator.writeTaskData(zip);
					zip.closeEntry();
				}
				
				File file = application.file.storeFile(user, name + ".zip", out.toByteArray(), 
						Instant.now(), "SDSD ISOXML Creator", ARMessageType.TASKDATA);
				
				if(file == null) throw new SDSDException("No storage task for this file!");
				return new JSONObject().put("filename", file.getFilename());
			}
		} catch (IndexOutOfBoundsException e) {
			throw createError(user, new SDSDException("Field not found"));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	private static void writeLineString(LineString lsg, JSONArray coords) {
		JSONArray coord;
		for(int c = 0; c < coords.length(); ++c) {
			coord = coords.getJSONArray(c);
			XmlPoint pnt = lsg.addPoint(PointType.OTHER, coord.getDouble(1), coord.getDouble(0));
			if(coord.length() > 2)
				pnt.setUp(coord.getDouble(2));
		}
	}

}
