package de.sdsd.projekt.prototype.jsonrpc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.client.model.Filters;

import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.applogic.TripleFunctions;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.QueryResult;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilQuerySolution;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.WikiFormat;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.WikiRes;
import de.sdsd.projekt.prototype.applogic.WikinormiaFunctions.Sorting;
import de.sdsd.projekt.prototype.applogic.WikinormiaFunctions.WikiInstanceCount;
import de.sdsd.projekt.prototype.data.DraftFormat;
import de.sdsd.projekt.prototype.data.DraftItem;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;
import de.sdsd.projekt.prototype.data.WikiAttribute;
import de.sdsd.projekt.prototype.data.WikiClass;
import de.sdsd.projekt.prototype.data.WikiEntry;
import de.sdsd.projekt.prototype.data.WikiInstance;
import de.sdsd.projekt.prototype.data.WikiInstance.WikiAttributeValue;

/**
 * JSONRPC-Endpoint for wikinormia functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class WikinormiaEndpoint extends JsonRpcEndpoint {

	/**
	 * Instantiates a new wikinormia endpoint.
	 *
	 * @param application the application
	 */
	public WikinormiaEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	/** The Constant JS_INSTCOUNT. */
	private static final String JS_FORMAT="format", JS_CLASS="class", JS_COMMENT="description", 
			JS_INSTANCE="isInstance", JS_INSTANCEOF="instanceOf", 
			JS_BASE="base", JS_SUBTYPES="subtypes", JS_PARTOF="partof", JS_PARTS="parts",
			JS_PROPS="attributes", JS_PROP="attrib", JS_RANGE="range", JS_VALUE="value",
			JS_INSTCOUNT="instCount";
	
	
	/**
	 * Wiki head to json.
	 *
	 * @param wiki the wiki
	 * @return the JSON object
	 */
	public static JSONObject wikiHeadToJson(WikiEntry wiki) {
		if(wiki == null) return null;
		return new JSONObject()
				.put("value", wiki.getUri())
				.put("identifier", wiki.getIdentifier())
				.put("label", wiki.getLabel());
	}
	
	/**
	 * Wiki list to json sorted.
	 *
	 * @param wiki the wiki
	 * @return the JSON array
	 */
	public static JSONArray wikiListToJsonSorted(Collection<WikiEntry> wiki) {
		return wiki.stream()
				.sorted()
				.map(WikinormiaEndpoint::wikiHeadToJson)
				.collect(Util.toJSONArray());
	}
	
	/**
	 * Wiki list to json.
	 *
	 * @param wiki the wiki
	 * @return the JSON array
	 */
	public static JSONArray wikiListToJson(Collection<WikiEntry> wiki) {
		return wiki.stream()
				.map(WikinormiaEndpoint::wikiHeadToJson)
				.collect(Util.toJSONArray());
	}
	
	/**
	 * Wiki attr to json.
	 *
	 * @param attr the attr
	 * @return the JSON object
	 */
	public static JSONObject wikiAttrToJson(WikiAttribute attr) {
		return new JSONObject()
				.put(JS_PROP, wikiHeadToJson(attr))
				.put(JS_RANGE, wikiHeadToJson(attr.getRange()));
	}
	
	/**
	 * Wiki attr value to json.
	 *
	 * @param val the val
	 * @return the stream
	 */
	public static Stream<JSONObject> wikiAttrValueToJson(WikiAttributeValue val) {
		return val.getValues()
				.map(WikinormiaEndpoint::wikiValueToJson)
				.map(value -> wikiAttrToJson(val.attr).put(JS_VALUE, value));
	}
	
	/**
	 * Wiki value to json.
	 *
	 * @param value the value
	 * @return the object
	 */
	private static Object wikiValueToJson(WikiInstance.Value value) {
		if(value.isLiteral()) {
			Object val = value.asLiteral().getValue();
			if(val instanceof String 
					|| val instanceof Integer || val instanceof Long 
					|| val instanceof Double  || val instanceof Float)
				return val;
			else
				return val.toString();
		} else {
			return new JSONObject()
					.put("value", value.uri)
					.put("label", value.label);
		}
	}
	
	/**
	 * Gets the.
	 *
	 * @param req the req
	 * @param page the page
	 * @param inheritance the inheritance
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject get(HttpServletRequest req, String page, boolean inheritance) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("wikinormia get: user(" + (user != null ? user.getName() : "none") + ") page(" + page 
					+ ") inheritance(" + inheritance + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				WikiEntry entry = application.wiki.get(WikiRes.page(page), inheritance);
				if(entry instanceof WikiInstance) {
					WikiInstance wiki = (WikiInstance) entry;
					return new JSONObject()
							.put(JS_INSTANCE, true)
							.put(JS_INSTANCEOF, wikiHeadToJson(wiki.getType().get()))
							.put(JS_CLASS, wikiHeadToJson(wiki))
							.put(JS_COMMENT, wiki.getComment().orElse(null))
							.put(JS_PARTOF, wikiListToJsonSorted(wiki.getPartOf()))
							.put(JS_PARTS, wikiListToJsonSorted(wiki.getParts()))
							.put(JS_PROPS, wiki.getAttributes().stream()
									.sorted()
									.flatMap(WikinormiaEndpoint::wikiAttrValueToJson)
									.collect(Util.toJSONArray()));
				}
				else {
					WikiClass wiki = (WikiClass) entry;
					WikiInstanceCount cnt = application.wiki.countInstances(ResourceFactory.createResource(wiki.getUri()), true);
					return new JSONObject()
							.put(JS_INSTANCE, false)
							.put(JS_FORMAT, wikiHeadToJson(wiki.getFormat().orElse(null)))
							.put(JS_CLASS, wikiHeadToJson(wiki))
							.put(JS_COMMENT, wiki.getComment().orElse(null))
							.put(JS_INSTCOUNT, cnt.instances)
							.put(JS_BASE, wikiListToJsonSorted(wiki.getBase()))
							.put(JS_SUBTYPES, wikiListToJsonSorted(wiki.getSubTypes()))
							.put(JS_PARTOF, wikiListToJsonSorted(wiki.getPartOf()))
							.put(JS_PARTS, wikiListToJsonSorted(wiki.getParts()))
							.put(JS_PROPS, wiki.getAttributes().stream()
									.sorted()
									.map(WikinormiaEndpoint::wikiAttrToJson)
									.collect(Util.toJSONArray()));
				}
			}
		} catch (NoSuchElementException e) {
			throw createError(user, new SDSDException("Page '" + page + "' not found!"));
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * List literal types.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listLiteralTypes(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("wikinormia listLiteralTypes: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				JSONArray list = new JSONArray();
				
				Var URI=Var.alloc("res"), LABEL=Var.alloc("label");
				Query query = new SelectBuilder()
						.addVar(URI).addVar(LABEL)
						.from(TripleFunctions.TBOX)
						.addWhere(URI, RDFS.subClassOf, TripleFunctions.DEFAULT_TYPES)
						.addWhere(URI, RDFS.label, LABEL)
						.addOrderBy(LABEL)
						.build();
				try(QueryResult qr = application.triple.query(query)) {
					for(UtilQuerySolution qs : qr.iterate()) {
						list.put(new Res(qs.getUri(URI), qs.getString(LABEL)));
					}
				}

				return new JSONObject().put("literals", list);
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * List autocomplete types.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listAutocompleteTypes(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("wikinormia listAutocompleteTypes: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				Groups formats = new Groups(10);
				
				for(DraftFormat format : application.list.draftFormats.getList(user)) {
					formats.addGroup(format.getId().toHexString(), "Draft: " + format.getLabel());
				}
				for(DraftItem item : application.list.draftItems.getList(user)) {
					formats.addItem(item.getFormat().toHexString(), item.getId().toHexString(), item.getLabel());
				}
				
				Var URI=Var.alloc("res"), LABEL=Var.alloc("label"), ID=Var.alloc("id"), FORMAT=Var.alloc("format"), FORMATLABEL=Var.alloc("flabel");
				Query query = new SelectBuilder()
						.addVar(URI).addVar(LABEL).addVar(ID).addVar(FORMAT).addVar(FORMATLABEL)
						.from(TripleFunctions.TBOX)
						.addWhere(URI, DCTerms.format, FORMAT)
						.addWhere(FORMAT, RDFS.label, FORMATLABEL)
						.addWhere(URI, RDF.type, RDFS.Class)
						.addWhere(URI, RDFS.label, LABEL)
						.addWhere(URI, DCTerms.identifier, ID)
						.addOrderBy(FORMATLABEL).addOrderBy(LABEL)
						.build();
				try(QueryResult qr = application.triple.query(query)) {
					for(UtilQuerySolution qs : qr.iterate()) {
						formats.addGroup(qs.getUri(FORMAT), qs.getString(FORMATLABEL));
						formats.addItem(qs.getUri(FORMAT), qs.getUri(URI), String.format("%s (%s)", qs.getString(LABEL), qs.getString(ID)));
					}
				}

				return new JSONObject().put("types", formats.toJson());
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * List types.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listTypes(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("wikinormia listTypes: user(%s)\n", 
					user != null ? user.getName() : "none");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				Var URI=Var.alloc("res"), LABEL=Var.alloc("label"), FORMAT=Var.alloc("format"), FORMATLABEL=Var.alloc("flabel");
				Query query = new SelectBuilder()
						.addVar(URI).addVar(LABEL).addVar(FORMATLABEL)
						.from(TripleFunctions.TBOX)
						.addWhere(URI, DCTerms.format, FORMAT)
						.addWhere(FORMAT, RDFS.label, FORMATLABEL)
						.addWhere(URI, RDF.type, RDFS.Class)
						.addWhere(URI, RDFS.label, LABEL)
						.build();
				JSONArray types = new JSONArray();
				try(QueryResult qr = application.triple.query(query)) {
					for(UtilQuerySolution qs : qr.iterate()) {
						String label = String.format("%s - %s", qs.getString(FORMATLABEL), qs.getString(LABEL));
						types.put(new Res(qs.getUri(URI), label));
					}
				}
				
				return new JSONObject().put("types", types);
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Gets the instances.
	 *
	 * @param req the req
	 * @param types the types
	 * @return the instances
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getInstances(HttpServletRequest req, JSONArray types) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("wikinormia getInstances: user(%s) types%s\n", 
					user != null ? user.getName() : "none", types.toString());
			
			if (user == null) 
				throw new NoLoginException();
			else {
				List<ObjectId> drafttypes = new ArrayList<>();
				List<Node> wikitypes = new ArrayList<>();
				for(int t = 0; t < types.length(); ++t) {
					DraftItem.Ref type = DraftItem.Ref.of(types.getString(t));
					if(type == null) continue;
					else if(type.isDraft()) drafttypes.add(type.asObjectId());
					else wikitypes.add(type.asNode());
				}
				
				Groups groups = new Groups(types.length());
				
				if(drafttypes.size() > 0) {
					for(DraftItem item : application.list.draftItems.get(user, Filters.in(DraftItem.ID, drafttypes))) {
						String type = item.getId().toHexString();
						groups.addGroup(type, item.getLabel());
						for(DraftItem.Instance inst : item.getInstances()) {
							groups.addItem(type, type + '_' + inst.getIdentifier(), inst.getLabel());
						}
					}
				}
				
				if(wikitypes.size() > 0) {
					Var URI=Var.alloc("res"), LABEL=Var.alloc("label"), 
							TYPE=Var.alloc("type"), TLABEL=Var.alloc("tLabel"), BASE=Var.alloc("base");
					SelectBuilder query = new SelectBuilder()
							.addVar(URI).addVar(LABEL).addVar(TYPE).addVar(TLABEL)
							.from(TripleFunctions.TBOX)
							.addWhere(URI, RDF.type, TYPE)
							.addWhere(TYPE, TripleFunctions.SUBCLASS, BASE)
							.addWhere(URI, RDFS.label, LABEL)
							.addWhere(TYPE, RDFS.label, TLABEL)
							.addValueVar(BASE, wikitypes.toArray());
					try(QueryResult qr = application.triple.query(query.build())) {
						for(UtilQuerySolution qs : qr.iterate()) {
							String type = qs.getUri(TYPE);
							groups.addGroup(type, qs.getString(TLABEL));
							groups.addItem(type, qs.getUri(URI), qs.getString(LABEL));
						}
					}
				}
				
				return new JSONObject().put("instances", groups.toJson());
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * List instances.
	 *
	 * @param req the req
	 * @param type the type
	 * @param offset the offset
	 * @param limit the limit
	 * @param inheritance the inheritance
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listInstances(HttpServletRequest req, String type, int offset, int limit, boolean inheritance) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("wikinormia listInstances: user(%s) type(%s) offset(%d) limit(%d) inheritance(%b)\n", 
					(user != null ? user.getName() : "none"), type, offset, limit, inheritance);
			
			if (user == null) 
				throw new NoLoginException();
			else {
				Resource typeRes = new WikiFormat(type);
				
				WikiInstanceCount cnt = application.wiki.countInstances(typeRes, inheritance);
				List<WikiEntry> instances = application.wiki.getInstances(typeRes, inheritance, Sorting.LABEL, offset, limit);
				
				return new JSONObject()
						.put("title", cnt.typeLabel)
						.put("list", wikiListToJson(instances))
						.put("pos", new JSONObject()
								.put("offset", offset)
								.put("total", cnt.instances));
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * List draft.
	 *
	 * @param req the req
	 * @param formatID the format ID
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listDraft(HttpServletRequest req, String formatID) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("wikinormia listDraft: user(%s) formatID(%s)\n", 
					user != null ? user.getName() : "none", formatID);
			
			if (user == null) 
				throw new NoLoginException();
			else {
				JSONArray items = application.list.draftItems.get(user, DraftItem.filterFormat(new ObjectId(formatID))).stream()
						.map(item -> new Res(item.getId().toHexString(), item.getLabel()))
						.collect(Util.toJSONArray());
				return new JSONObject().put("items", items);
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Gets the draft.
	 *
	 * @param req the req
	 * @param itemID the item ID
	 * @return the draft
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getDraft(HttpServletRequest req, String itemID) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("wikinormia getDraft: user(%s) itemID(%s)\n", 
					user != null ? user.getName() : "none", itemID);
			
			if (user == null) 
				throw new NoLoginException();
			else {
				return application.list.draftItems.get(user, itemID).getJson();
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Sets the draft.
	 *
	 * @param req the req
	 * @param formatID the format ID
	 * @param itemID the item ID
	 * @param input the input
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject setDraft(HttpServletRequest req, String formatID, String itemID, JSONObject input) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("wikinormia setDraft: user(%s) formatID(%s) itemID(%s) title(%s)\n", 
					user != null ? user.getName() : "none", formatID, itemID, input.optString("label"));
			
			if (user == null) 
				throw new NoLoginException();
			else if(!application.list.draftFormats.exists(user, formatID))
				throw new SDSDException("Given formatID doesn't exist");
			else {
				ObjectId format = new ObjectId(formatID), id = itemID != null ? new ObjectId(itemID) : null;
				
				String identifier = input.getString(DraftItem.IDENTIFIER);
				if(identifier.isBlank()) throw new SDSDException("Identifier must not be empty");
				if(input.getString(DraftItem.LABEL).isBlank()) throw new SDSDException("Label must not be empty");
				if(application.list.draftItems.exists(user, DraftItem.filter(user, format, identifier, id)))
					throw new SDSDException("Another class with this identifier already exists in the draft");
				
				JSONArray arr = input.getJSONArray(DraftItem.ATTRIBUTES);
				Map<String, String> attributes = new HashMap<>(arr.length());
				for(int i = 0; i < arr.length(); ++i) {
					JSONObject attr = arr.getJSONObject(i);
					String attrid = attr.getString(DraftItem.IDENTIFIER);
					if(attrid.isBlank()) throw new SDSDException("Identifier must not be empty");
					if(attr.getString(DraftItem.LABEL).isBlank()) throw new SDSDException("Label must not be empty");
					String type = attr.getString(DraftItem.TYPE);
					if(type.isBlank()) throw new SDSDException("Type must not be empty");
					if(attributes.put(attrid, type) != null)
						throw new SDSDException("Multiple attributes with the same identifier");
				}
				
				arr = input.getJSONArray(DraftItem.INSTANCES);
				Set<String> instances = new HashSet<>(arr.length());
				for(int i = 0; i < arr.length(); ++i) {
					JSONObject inst = arr.getJSONObject(i);
					String instid = inst.getString(DraftItem.IDENTIFIER);
					if(instid.isBlank()) throw new SDSDException("Identifier must not be empty");
					if(inst.getString(DraftItem.LABEL).isBlank()) throw new SDSDException("Label must not be empty");
					if(!instances.add(instid))
						throw new SDSDException("Multiple instances with the same identifier");
					//TODO: check instance values
				}
				
				boolean ok;
				DraftItem item;
				if(itemID == null) {
					item = application.list.draftItems.add(user, DraftItem.create(user, new ObjectId(formatID), input));
					ok = true;
				} else if(!application.list.draftItems.exists(user, itemID)) {
					throw new SDSDException("Given itemID doesn't exist");
				} else {
					item = application.list.draftItems.get(user, itemID);
					ok = application.list.draftItems.update(user, item, item.setContent(input));
				}

				return success(ok).put("id", item.getId().toHexString());
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Drop draft.
	 *
	 * @param req the req
	 * @param itemID the item ID
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject dropDraft(HttpServletRequest req, String itemID) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("wikinormia dropDraft: user(%s) itemID(%s)\n", 
					user != null ? user.getName() : "none", itemID);
			
			if (user == null) 
				throw new NoLoginException();
			else {
				return success(application.list.draftItems.delete(user, itemID));
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * List units.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listUnits(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("wikinormia listUnits: user(%s)\n", 
					(user != null ? user.getName() : "none"));
			
			if (user == null) 
				throw new NoLoginException();
			else {
				Var UNIT = Var.alloc("u"), ULABEL = Var.alloc("ul"), CAT = Var.alloc("c"), CLABEL = Var.alloc("cl");
				SelectBuilder query = new SelectBuilder()
						.addVar(UNIT).addVar(ULABEL).addVar(CAT).addVar(CLABEL)
						.from(TripleFunctions.TBOX)
						.addWhere(UNIT, RDF.type, TripleFunctions.FORMAT_UNKNOWN.res("unit"))
						.addWhere(UNIT, RDFS.label, ULABEL)
						.addWhere(UNIT, DCTerms.isPartOf, CAT)
						.addWhere(CAT, RDFS.label, CLABEL)
						.addOrderBy(CLABEL).addOrderBy(ULABEL);
				
				Groups groups = new Groups(10);
				try(QueryResult qr = application.triple.query(query.build())) {
					for(UtilQuerySolution qs : qr.iterate()) {
						groups.addGroup(qs.getUri(CAT), qs.getString(CLABEL));
						groups.addItem(qs.getUri(CAT), qs.getUri(UNIT), qs.getString(ULABEL));
					}
				}
				
				return new JSONObject().put("units", groups.toJson());
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Gets the family tree.
	 *
	 * @param req the req
	 * @param bases the bases
	 * @return the family tree
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject getFamilyTree(HttpServletRequest req, JSONArray bases) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("wikinormia getFamilyTree: user(%s) base(%s)\n", 
					(user != null ? user.getName() : "none"), bases.toString());
			
			if (user == null) 
				throw new NoLoginException();
			else {
				Queue<ObjectId> basequeue = new LinkedList<>();
				List<Node> wikibases = new ArrayList<>();
				for(int i = 0; i < bases.length(); ++i) {
					String base = bases.getString(i);
					if(Util.isObjectId(base)) basequeue.add(new ObjectId(base));
					else wikibases.add(NodeFactory.createURI(base));
				}
				
				Map<String, JSONObject> tree = new LinkedHashMap<>();
				while(basequeue.size() > 0) {
					DraftItem item = application.list.draftItems.get(user, basequeue.poll());
					if(item.getContent().isEmpty()) continue;
					
					JSONArray attrs = new JSONArray();
					for(DraftItem.Attribute attr : item.getAttributes()) {
						attrs.put(new JSONObject()
								.put(DraftItem.IDENTIFIER, attr.getIdentifier())
								.put(DraftItem.LABEL, attr.getLabel())
								.put(DraftItem.TYPE, attr.getType().ref)
								.put(DraftItem.UNIT, attr.getUnit()));
					}
					
					tree.put(item.getId().toHexString(), new JSONObject()
							.put("uri", item.getId().toHexString())
							.put(DraftItem.LABEL, item.getLabel())
							.put(DraftItem.ATTRIBUTES, attrs));
					
					for(DraftItem.Ref ref : item.getBase()) {
						if(ref.isDraft()) basequeue.add(ref.asObjectId());
						else wikibases.add(ref.asNode());
					}
				}
				
				if (wikibases.size() > 0) {
					Var RES = Var.alloc("r"), BASE = Var.alloc("base"), BLABEL = Var.alloc("bl"), PROP = Var.alloc("p"),
							PID = Var.alloc("pi"), PLABEL = Var.alloc("pl"), PTYPE = Var.alloc("pt"), PUNIT = Var.alloc("pu");
					SelectBuilder query = new SelectBuilder()
							.addVar(BASE).addVar(BLABEL).addVar(PID).addVar(PLABEL).addVar(PTYPE).addVar(PUNIT)
							.from(TripleFunctions.TBOX)
							.addWhere(RES, TripleFunctions.SUBCLASS, BASE)
							.addWhere(BASE, RDFS.label, BLABEL)
							.addWhere(PROP, RDFS.domain, BASE)
							.addWhere(PROP, DCTerms.identifier, PID)
							.addWhere(PROP, RDFS.label, PLABEL)
							.addOptional(PROP, RDFS.range, PTYPE)
							.addOptional(PROP, TripleFunctions.UNIT, PUNIT)
							.addValueVar(RES, wikibases.toArray());
					try (QueryResult qr = application.triple.query(query.build())) {
						for (UtilQuerySolution qs : qr.iterate()) {
							String uri = "wkn:" + qs.getUri(BASE).substring(TripleFunctions.NS_WIKI.length());
							JSONObject base = tree.get(uri);
							if(base == null) {
								tree.put(uri, base = new JSONObject()
										.put("uri", uri)
										.put(DraftItem.LABEL, qs.getString(BLABEL))
										.put(DraftItem.ATTRIBUTES, new JSONArray()));
							}
							
							base.getJSONArray(DraftItem.ATTRIBUTES).put(new JSONObject()
									.put(DraftItem.IDENTIFIER, qs.getString(PID))
									.put(DraftItem.LABEL, qs.getString(PLABEL))
									.put(DraftItem.TYPE, qs.getUri(PTYPE))
									.put(DraftItem.UNIT, qs.getString(PUNIT)));
						}
					} 
				}
				
				return new JSONObject().put("tree", tree.values());
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Publish draft.
	 *
	 * @param req the req
	 * @param formatID the format ID
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject publishDraft(HttpServletRequest req, String formatID) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("wikinormia publishDraft: user(%s) formatID(%s)\n", 
					user != null ? user.getName() : "none", formatID);
			
			ObjectId format = new ObjectId(formatID);
			if (user == null) 
				throw new NoLoginException();
			else if(!application.list.draftFormats.exists(user, format))
				throw new SDSDException("Given formatID doesn't exist");
			else {
				return success(application.wiki.publishDraft(user, application.list.draftFormats.get(user, format)));
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
}
