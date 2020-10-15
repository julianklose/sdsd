package de.sdsd.projekt.prototype.jsonrpc;

import static de.sdsd.projekt.prototype.applogic.TripleFunctions.FORMAT_UNKNOWN;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Hex;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.client.model.Filters;

import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.applogic.TripleFunctions;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.DeduplicatedResource;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.QueryResult;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilQuerySolution;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.WikiInst;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.WikiType;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.GeoElement;
import de.sdsd.projekt.prototype.data.GeoElement.ElementType;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;

/**
 * JSONRPC-Endpoint for dashboard functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class DashboardEndpoint extends JsonRpcEndpoint {

	/**
	 * Instantiates a new dashboard endpoint.
	 *
	 * @param application the application
	 */
	public DashboardEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	/**
	 * Gets the all fields.
	 *
	 * @param req the req
	 * @return the all fields
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getAllFields(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("getAllFields: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				List<String> graphs = application.list.files.getList(user).stream()
						.sorted(File.CMP_RECENT_CORE)
						.map(File::getURI)
						.collect(Collectors.toList());
				
				Map<String, DeduplicatedResource> fields = application.triple.getAll(user, TripleFunctions.FIELD, graphs, null);
				
				List<GeoElement> geos = application.geo.find(user, Filters.and(
						Filters.in(GeoElement.URI, fields.keySet()), 
						GeoElement.filterType(ElementType.Field)));
				JSONArray list = new JSONArray();
				for(GeoElement geo : geos) {
					DeduplicatedResource dRes = fields.get(geo.getUri());
					if(dRes == null) continue;
					Set<String> labels = dRes.getLabels();
					if(geo.getLabel() != null && !geo.getLabel().isEmpty())
						labels.add(geo.getLabel());
					
					
					JSONObject props = new JSONObject()
							.put("res", dRes.getSameAs().stream()
									.map(ri -> new JSONObject()
											.put("uri", ri.uri)
											.put("graph", ri.graph))
									.collect(Util.toJSONArray()))
							.put("label", labels)
							.put("prefLabel", dRes.getPreferredLabel())
							.put("area", Double.isFinite(geo.getArea()) ? geo.getArea() : null);
					
					list.put(geo.getFeatureJson()
							.put("properties", props));
				}

				return new JSONObject().put("fields", list);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Gets the all machines.
	 *
	 * @param req the req
	 * @return the all machines
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getAllMachines(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("getAllMachines: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				List<String> graphs = application.list.files.getList(user).stream()
						.sorted(File.CMP_RECENT_CORE)
						.map(File::getURI)
						.collect(Collectors.toList());
				Map<String, DeduplicatedResource> machines = application.triple.getAll(user, TripleFunctions.MACHINE, graphs, null);
				JSONObject machineTypes = getMachineTypes(req, machines.keySet());
				
				JSONArray list = new JSONArray();
				for(Entry<String, DeduplicatedResource> e : machines.entrySet()) {
					JSONObject props = machineTypes.optJSONObject(e.getKey());
					if(props == null) {
						//System.err.println("Clientname not found for " + e.getKey());
						props = new JSONObject();
					}
					props.put("res", e.getValue().getSameAs().stream()
									.map(ri -> new JSONObject()
											.put("uri", ri.uri)
											.put("graph", ri.graph))
									.collect(Util.toJSONArray()))
							.put("label", e.getValue().getLabels());
					list.put(props);
				}

				return new JSONObject().put("machines", list);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Read machine type.
	 *
	 * @param clientname the clientname
	 * @param res the res
	 * @param out the out
	 */
	private void readMachineType(byte[] clientname, Map<Resource, Res> res, JSONObject out) {
		int group = (clientname[0] >>> 4) & 0x7;
		int system = clientname[1] >>> 1;
		int function = Byte.toUnsignedInt(clientname[2]);
		
		WikiInst r = FORMAT_UNKNOWN.res("functionIndustryGroup").inst(String.format("g%d", group));
		Res jr = res.get(r);
		if(jr == null) res.put(r, jr = new Res(r.getURI(), "Unknown"));
		out.put("group", jr);
		r = FORMAT_UNKNOWN.res("functionSystem").inst(String.format("g%ds%d", group, system));
		jr = res.get(r);
		if(jr == null) res.put(r, jr = new Res(r.getURI(), "Unknown"));
		out.put("system", jr);
		r = FORMAT_UNKNOWN.res("function").inst(String.format("g%ds%df%d", group, system, function));
		jr = res.get(r);
		if(jr == null) res.put(r, jr = new Res(r.getURI(), "Unknown"));
		out.put("function", jr);
	}
	
	/**
	 * Gets the resource labels.
	 *
	 * @param res the res
	 * @return the resource labels
	 */
	private void getResourceLabels(Map<Resource, Res> res) {
		if(res.isEmpty()) return;
		Var LABEL = Var.alloc("label"), VAL = Var.alloc("val");
		Query query = new SelectBuilder()
				.addVar(VAL).addVar(LABEL)
				.from(TripleFunctions.TBOX)
				.addWhere(VAL, RDFS.label, LABEL)
				.addValueVar(VAL, res.keySet().toArray())
				.build();
		try(QueryResult qr = application.triple.query(query)) {
			for(UtilQuerySolution qs : qr.iterate()) {
				res.get(qs.getResource(VAL)).setLabel(qs.getString(LABEL));
			}
		}
	}
	
	/**
	 * Gets the machine type.
	 *
	 * @param req the req
	 * @param clientname the clientname
	 * @return the machine type
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getMachineType(HttpServletRequest req, String clientname) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("getMachineType: user(%s) clientname(%s)\n", 
					user != null ? user.getName() : "none", clientname);
			
			if (user == null) 
				throw new NoLoginException();
			else {
				Map<Resource, Res> res = new HashMap<>(4);
				JSONObject out = new JSONObject();
				readMachineType(Hex.decodeHex(clientname), res, out);
				
				WikiType device = T_ISOXML.res("DVC"), property = T_ISOXML.res("DPT"), acp = FORMAT_UNKNOWN.res("functionACP");
				
				JSONArray jacp = new JSONArray();
				Literal idLit = ResourceFactory.createTypedLiteral(clientname, XSDDatatype.XSDhexBinary);
				Var DVC = Var.alloc("dvc"), DPT = Var.alloc("dpt"), VAL = Var.alloc("val");
				Query query = new SelectBuilder()
						.addVar(VAL)
						.from(application.triple.getFileGraphs(user))
						.addWhere(DVC, device.prop("D"), idLit)
						.addWhere(DPT, DCTerms.isPartOf, DVC)
						.addWhere(DPT, property.prop("B"), ResourceFactory.createTypedLiteral(179))
						.addWhere(DPT, property.prop("C"), VAL)
						.build();
				try(QueryResult qr = application.triple.query(query)) {
					for(UtilQuerySolution qs : qr.iterate()) {
						WikiInst r = acp.inst(qs.getLiteral(VAL).getInt());
						Res jr = new Res(r.getURI(), "Unknown");
						jacp.put(jr);
						res.put(r, jr);
					}
				}
				if(jacp.length() > 0)
					out.put("acp", jacp);
				
				getResourceLabels(res);

				return out;
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Gets the machine types.
	 *
	 * @param req the req
	 * @param uris the uris
	 * @return the machine types
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getMachineTypes(HttpServletRequest req, Collection<String> uris) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("getMachineTypes: user(%s) uris(%d)\n", 
					user != null ? user.getName() : "none", uris.size());
			
			if (user == null) 
				throw new NoLoginException();
			else {
				Map<Resource, Res> res = new HashMap<>(4);
				Map<String, byte[]> clientname = new HashMap<>();
				JSONObject out = new JSONObject();
				
				WikiType device = T_ISOXML.res("DVC"), property = T_ISOXML.res("DPT"), acp = FORMAT_UNKNOWN.res("functionACP");
				
				Var DVC = Var.alloc("dvc"), CLI = Var.alloc("cli"), SN = Var.alloc("sn"), 
						SA = Var.alloc("sa"), DPT = Var.alloc("dpt"), VAL = Var.alloc("val");
				Query query = new SelectBuilder().setDistinct(true)
						.addVar(DVC).addVar(CLI).addVar(SN).addVar(VAL)
						.from(user.getGraphUri())
						.from(application.triple.getFileGraphs(user))
						.addWhere(DVC, device.prop("D"), CLI)
						.addOptional(DVC, device.prop("E"), SN)
						.addOptional(new WhereBuilder()
								.addWhere(DVC, OWL.sameAs, SA)
								.addWhere(DPT, DCTerms.isPartOf, SA)
								.addWhere(DPT, property.prop("B"), ResourceFactory.createTypedLiteral(179))
								.addWhere(DPT, property.prop("C"), VAL))
						.addValueVar(DVC, uris.stream().map(NodeFactory::createURI).toArray())
						.build();
				try(QueryResult qr = application.triple.query(query)) {
					for(UtilQuerySolution qs : qr.iterate()) {
						String dvc = qs.getUri(DVC);
						if(!clientname.containsKey(dvc)) {
							Object value = qs.getLiteral(CLI).getValue();
							byte[] cl = value instanceof byte[] ? (byte[]) value : Hex.decodeHex((String) value);
							clientname.put(dvc, cl);
							out.put(dvc, new JSONObject()
									.put("serialnumber", qs.getString(SN)));
						}
						if(qs.contains(VAL)) {
							WikiInst r = acp.inst(qs.getLiteral(VAL).getInt());
							Res jr = res.get(r);
							if(jr == null) res.put(r, jr = new Res(r.getURI(), "Unknown"));
							out.getJSONObject(dvc).append("acp", jr);
						}
					}
				}
				
				clientname.forEach((dvc, cl) -> readMachineType(cl, res, out.getJSONObject(dvc)));
				getResourceLabels(res);

				return out;
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}

}
