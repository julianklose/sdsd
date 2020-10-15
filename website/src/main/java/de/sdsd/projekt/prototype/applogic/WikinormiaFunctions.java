package de.sdsd.projekt.prototype.applogic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.bson.types.ObjectId;

import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.QueryResult;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilQuerySolution;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.WikiInst;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.WikiRes;
import de.sdsd.projekt.prototype.data.DraftFormat;
import de.sdsd.projekt.prototype.data.DraftItem;
import de.sdsd.projekt.prototype.data.DraftItem.AttributeValues;
import de.sdsd.projekt.prototype.data.DraftItem.Ref;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.SDSDType;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.WikiAttribute;
import de.sdsd.projekt.prototype.data.WikiClass;
import de.sdsd.projekt.prototype.data.WikiEntry;
import de.sdsd.projekt.prototype.data.WikiInstance;

/**
 * Provides all wikinormia functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class WikinormiaFunctions {
	
	/** The app. */
	private final ApplicationLogic app;

	/**
	 * Instantiates a new wikinormia functions.
	 *
	 * @param app the app
	 */
	public WikinormiaFunctions(ApplicationLogic app) {
		this.app = app;
	}

	/**
	 * Gets the.
	 *
	 * @param res the res
	 * @param inheritance the inheritance
	 * @return the wiki entry
	 * @throws NoSuchElementException the no such element exception
	 */
	public WikiEntry get(WikiRes res, boolean inheritance) throws NoSuchElementException {
		return res instanceof WikiInst ? getInstance((WikiInst) res, inheritance) : getClass(res, inheritance);
	}
	
	/**
	 * Gets the class.
	 *
	 * @param res the res
	 * @param inheritance the inheritance
	 * @return the class
	 * @throws NoSuchElementException the no such element exception
	 */
	public WikiClass getClass(WikiRes res, boolean inheritance) throws NoSuchElementException {
		Var P=Var.alloc("p"), O=Var.alloc("o"),
				PARENT=Var.alloc("parent"), PIDENT=Var.alloc("pIdent"), PLABEL=Var.alloc("pLabel"),
				CHILD=Var.alloc("child"), CIDENT=Var.alloc("cIdent"), CLABEL=Var.alloc("cLabel"),
				ATTRIB=Var.alloc("attrib"), AIDENT=Var.alloc("aIdent"), ALABEL=Var.alloc("aLabel"), 
				RANGE=Var.alloc("aRange"), RLABEL=Var.alloc("arLabel");
		Var FORMAT=Var.alloc("format"), FIDENT=Var.alloc("fIdent"), FLABEL=Var.alloc("fLabel"),
				BASE=Var.alloc("subclass"), BIDENT=Var.alloc("bIdent"), BLABEL=Var.alloc("bLabel"), 
				SUB=Var.alloc("subtype"), SIDENT=Var.alloc("sIdent"), SLABEL=Var.alloc("sLabel");
		Query query = new ConstructBuilder()
				.addConstruct(res, P, O)
				.addConstruct(FORMAT, DCTerms.identifier, FIDENT)
				.addConstruct(FORMAT, RDFS.label, FLABEL)
				.addConstruct(BASE, DCTerms.identifier, BIDENT)
				.addConstruct(BASE, RDFS.label, BLABEL)
				.addConstruct(SUB, RDFS.subClassOf, res)
				.addConstruct(SUB, DCTerms.identifier, SIDENT)
				.addConstruct(SUB, RDFS.label, SLABEL)
				.addConstruct(PARENT, DCTerms.identifier, PIDENT)
				.addConstruct(PARENT, RDFS.label, PLABEL)
				.addConstruct(CHILD, DCTerms.isPartOf, res)
				.addConstruct(CHILD, DCTerms.identifier, CIDENT)
				.addConstruct(CHILD, RDFS.label, CLABEL)
				.addConstruct(ATTRIB, RDFS.domain, res)
				.addConstruct(ATTRIB, DCTerms.identifier, AIDENT)
				.addConstruct(ATTRIB, RDFS.label, ALABEL)
				.addConstruct(ATTRIB, RDFS.range, RANGE)
				.addConstruct(RANGE, RDFS.label, RLABEL)
				.from(TripleFunctions.TBOX)
				.addWhere(res, P, O)
				.addWhere(res, RDF.type, RDFS.Class)
				.addOptional(new WhereBuilder()
						.addWhere(res, DCTerms.format, FORMAT)
						.addWhere(FORMAT, DCTerms.identifier, FIDENT)
						.addWhere(FORMAT, RDFS.label, FLABEL))
				.addOptional(new WhereBuilder()
						.addWhere(res, RDFS.subClassOf, BASE)
						.addWhere(BASE, DCTerms.identifier, BIDENT)
						.addWhere(BASE, RDFS.label, BLABEL))
				.addOptional(new WhereBuilder()
						.addWhere(SUB, RDFS.subClassOf, res)
						.addWhere(SUB, DCTerms.identifier, SIDENT)
						.addWhere(SUB, RDFS.label, SLABEL))
				.addOptional(new WhereBuilder()
						.addWhere(res, DCTerms.isPartOf, PARENT)
						.addWhere(PARENT, DCTerms.identifier, PIDENT)
						.addWhere(PARENT, RDFS.label, PLABEL))
				.addOptional(new WhereBuilder()
						.addWhere(CHILD, DCTerms.isPartOf, res)
						.addWhere(CHILD, DCTerms.identifier, CIDENT)
						.addWhere(CHILD, RDFS.label, CLABEL))
				.addOptional(new WhereBuilder()
						.addWhere(ATTRIB, inheritance ? TripleFunctions.baseclass(RDFS.domain) : RDFS.domain, res)
						.addWhere(ATTRIB, DCTerms.identifier, AIDENT)
						.addWhere(ATTRIB, RDFS.label, ALABEL)
						.addWhere(ATTRIB, RDFS.range, RANGE)
						.addOptional(RANGE, RDFS.label, RLABEL))
				.build();
		try(QueryResult qr = app.triple.query(query)) {
			return new WikiClass(qr.construct(), res.getURI());
		}
	}
	
	/**
	 * Gets the single instance of WikinormiaFunctions.
	 *
	 * @param res the res
	 * @param inheritance the inheritance
	 * @return single instance of WikinormiaFunctions
	 * @throws NoSuchElementException the no such element exception
	 */
	public WikiInstance getInstance(WikiInst res, boolean inheritance) throws NoSuchElementException {
		Var TYPE=Var.alloc("type"), P=Var.alloc("p"), O=Var.alloc("o"),
				PARENT=Var.alloc("parent"), PIDENT=Var.alloc("pIdent"), PLABEL=Var.alloc("pLabel"),
				CHILD=Var.alloc("child"), CIDENT=Var.alloc("cIdent"), CLABEL=Var.alloc("cLabel"),
				ATTRIB=Var.alloc("attrib"), AIDENT=Var.alloc("aIdent"), ALABEL=Var.alloc("aLabel"), 
				RANGE=Var.alloc("aRange"), RLABEL=Var.alloc("arLabel");
		Var TIDENT=Var.alloc("tIdent"), TLABEL=Var.alloc("tLabel"), VALUE=Var.alloc("value"), VLABEL=Var.alloc("vlabel");
		Query query = new ConstructBuilder()
				.addConstruct(res, P, O)
				.addConstruct(TYPE, DCTerms.identifier, TIDENT)
				.addConstruct(TYPE, RDFS.label, TLABEL)
				.addConstruct(PARENT, DCTerms.identifier, PIDENT)
				.addConstruct(PARENT, RDFS.label, PLABEL)
				.addConstruct(CHILD, DCTerms.isPartOf, res)
				.addConstruct(CHILD, DCTerms.identifier, CIDENT)
				.addConstruct(CHILD, RDFS.label, CLABEL)
				.addConstruct(ATTRIB, RDFS.domain, TYPE)
				.addConstruct(ATTRIB, DCTerms.identifier, AIDENT)
				.addConstruct(ATTRIB, RDFS.label, ALABEL)
				.addConstruct(ATTRIB, RDFS.range, RANGE)
				.addConstruct(RANGE, RDFS.label, RLABEL)
				.addConstruct(VALUE, RDFS.label, VLABEL)
				.from(TripleFunctions.TBOX)
				.addWhere(res, P, O)
				.addWhere(res, RDF.type, TYPE)
				.addOptional(new WhereBuilder()
						.addWhere(TYPE, DCTerms.identifier, TIDENT)
						.addWhere(TYPE, RDFS.label, TLABEL))
				.addOptional(new WhereBuilder()
						.addWhere(res, DCTerms.isPartOf, PARENT)
						.addWhere(PARENT, DCTerms.identifier, PIDENT)
						.addWhere(PARENT, RDFS.label, PLABEL))
				.addOptional(new WhereBuilder()
						.addWhere(CHILD, DCTerms.isPartOf, res)
						.addWhere(CHILD, DCTerms.identifier, CIDENT)
						.addWhere(CHILD, RDFS.label, CLABEL))
				.addOptional(new WhereBuilder()
						.addWhere(ATTRIB, TripleFunctions.baseclass(RDFS.domain), TYPE)
						.addWhere(ATTRIB, RDFS.label, ALABEL)
						.addWhere(ATTRIB, RDFS.range, RANGE)
						.addOptional(RANGE, RDFS.label, RLABEL)
						.addOptional(new WhereBuilder()
								.addWhere(res, ATTRIB, VALUE)
								.addOptional(VALUE, RDFS.label, VLABEL)))
				.build();
		try(QueryResult qr = app.triple.query(query)) {
			return new WikiInstance(qr.construct(), res.getURI());
		}
	}
	
	/**
	 * Information about the number of wikinormia instances a type has.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class WikiInstanceCount {
		
		/** The type identifier. */
		public final String typeLabel, typeIdentifier;
		
		/** The instances. */
		public final int instances;
		
		/**
		 * Instantiates a new wiki instance count.
		 *
		 * @param typeLabel the type label
		 * @param typeIdentifier the type identifier
		 * @param instances the instances
		 */
		public WikiInstanceCount(String typeLabel, String typeIdentifier, int instances) {
			this.typeLabel = typeLabel;
			this.typeIdentifier = typeIdentifier;
			this.instances = instances;
		}
	}
	
	/**
	 * Count instances.
	 *
	 * @param res the res
	 * @param includeSubtypes the include subtypes
	 * @return the wiki instance count
	 * @throws NoSuchElementException the no such element exception
	 */
	public WikiInstanceCount countInstances(Resource res, boolean includeSubtypes) throws NoSuchElementException {
		Var TLABEL=Var.alloc("tLabel"), TIDENT=Var.alloc("tIdent"), URI=Var.alloc("res"), CNT=Var.alloc("count");
		Query query = new SelectBuilder()
				.addVar(TLABEL).addVar(TIDENT).addVar(new ExprAggregator(CNT, AggregatorFactory.createCountExpr(false, new ExprVar(URI))), CNT)
				.from(TripleFunctions.TBOX)
				.addWhere(res, RDFS.label, TLABEL)
				.addWhere(res, DCTerms.identifier, TIDENT)
				.addOptional(URI, includeSubtypes ? TripleFunctions.TYPE_SUBCLASS : RDF.type, res)
				.addGroupBy(TIDENT).addGroupBy(TLABEL)
				.build();
		try(QueryResult qr = app.triple.query(query)) {
			UtilQuerySolution qs = qr.first().get();
			return new WikiInstanceCount(qs.getString(TLABEL), qs.getString(TIDENT), qs.getLiteral(CNT).getInt());
		}
	}
	
	/**
	 * Sorting order of wikinormia resources.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static enum Sorting {
		
		/** The none. */
		NONE, 
 /** The identifier. */
 IDENTIFIER, 
 /** The label. */
 LABEL
	}
	
	/**
	 * Gets the instances.
	 *
	 * @param res the res
	 * @param includeSubtypes the include subtypes
	 * @param sort the sort
	 * @param offset the offset
	 * @param limit the limit
	 * @return the instances
	 * @throws NoSuchElementException the no such element exception
	 */
	public List<WikiEntry> getInstances(Resource res, boolean includeSubtypes, Sorting sort, int offset, int limit) throws NoSuchElementException {
		Var INST=Var.alloc("instance"), IIDENT=Var.alloc("iIdent"), ILABEL=Var.alloc("iLabel");
		ExprFactory ex = new ExprFactory();
		SelectBuilder query = new SelectBuilder()
				.addVar(INST).addVar(IIDENT).addVar(ILABEL)
				.from(TripleFunctions.TBOX)
				.addWhere(INST, includeSubtypes ? TripleFunctions.TYPE_SUBCLASS : RDF.type, res)
				.addWhere(INST, DCTerms.identifier, IIDENT)
				.addWhere(INST, RDFS.label, ILABEL)
				.setOffset(offset).setLimit(limit);
		if(sort == Sorting.IDENTIFIER) 
			query.addOrderBy(ex.lcase(ex.str(IIDENT)));
		else if(sort == Sorting.LABEL) 
			query.addOrderBy(ex.lcase(ex.str(ILABEL)));
		
		List<WikiEntry> instances = new ArrayList<>();
		try(QueryResult qr = app.triple.query(query.build())) {
			for(UtilQuerySolution qs : qr.iterate()) {
				instances.add(new WikiEntry(qs.getResource(INST)
						.addLiteral(DCTerms.identifier, qs.getLiteral(IIDENT))
						.addLiteral(RDFS.label, qs.getLiteral(ILABEL))));
			}
		}
		return instances;
	}
	
	/**
	 * Helper to get wikinormia types for draft items.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class WikiRefMap extends HashMap<ObjectId, WikiClass> {
		
		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = -8012125406740493140L;

		/**
		 * Instantiates a new wiki ref map.
		 *
		 * @param initialCapacity the initial capacity
		 */
		public WikiRefMap(int initialCapacity) {
			super(initialCapacity);
		}
		
		/**
		 * Deref.
		 *
		 * @param ref the ref
		 * @return the wiki entry
		 * @throws SDSDException the SDSD exception
		 */
		public WikiEntry deref(DraftItem.Ref ref) throws SDSDException {
			if(ref.isDraft()) {
				WikiClass cls = get(ref.asObjectId());
				if(cls == null) throw new SDSDException("Refernece not found: " + ref);
				return cls;
			}
			Node wref = ref.asNode();
			return new WikiEntry(wref.getURI(), null, wref.getLocalName(), wref.getLocalName());
		}
	}
	
	/**
	 * Publish draft.
	 *
	 * @param user the user
	 * @param format the format
	 * @return true, if successful
	 * @throws SDSDException the SDSD exception
	 * @throws ARException the AR exception
	 */
	public boolean publishDraft(User user, DraftFormat format) throws SDSDException, ARException {
		if(format.getIdentifier() == null || format.getIdentifier().isBlank())
			throw new SDSDException("Format must have an identifier");
		if(format.getLabel() == null || format.getLabel().isBlank())
			throw new SDSDException("Format must have a label");
		if(format.getMimeType() == null || format.getMimeType().isBlank())
			throw new SDSDException("Format must have a mimetype");
		format.getArType();
		
		Model model = ModelFactory.createDefaultModel();
		WikiClass formatCls = new WikiClass(null, format.getIdentifier().trim(), format.getLabel().trim());
		if(format.getComment() != null && !format.getComment().isBlank())
			formatCls.setComment(format.getComment().trim());
		formatCls.writeTo(model);
		
		List<DraftItem> items = app.list.draftItems.get(user, DraftItem.filterFormat(format.getId()));
		WikiRefMap classes = new WikiRefMap(items.size());
		for(DraftItem item : items) {
			if(item.getIdentifier() == null || item.getIdentifier().isBlank())
				throw new SDSDException("All classes must have an identifier");
			if(item.getLabel() == null || item.getLabel().isBlank())
				throw new SDSDException(item.getIdentifier() + ": Class must have a label");
			
			WikiClass cls = new WikiClass(formatCls, item.getIdentifier().trim(), item.getLabel().trim());
			String comment = item.getComment();
			if(comment != null && !comment.isBlank())
				cls.setComment(comment.trim());
			classes.put(item.getId(), cls);
		}
		
		for(DraftItem item : items) {
			try {
				WikiClass cls = classes.get(item.getId());
				
				for(DraftItem.Ref base : item.getBase()) {
					cls.addBase(classes.deref(base));
				}
				
				for(DraftItem.Ref partOf : item.getPartOf()) {
					cls.addPartOf(classes.deref(partOf));
				}
				
				
				for(DraftItem.Attribute attr : item.getAttributes()) {
					String identifier = attr.getIdentifier();
					String label = attr.getLabel();
					Ref type = attr.getType();
					if(identifier == null || identifier.isBlank())
						throw new SDSDException("All attributes must have an identifier");
					if(label == null || label.isBlank())
						throw new SDSDException("All attributes must have a label");
					if(type == null)
						throw new SDSDException("All attributes must have a type");
					
					WikiAttribute wattr = cls.createAttribute(identifier, label, classes.deref(type));
					String unit = attr.getUnit();
					if(unit != null && !unit.isBlank())
						wattr.setUnit(unit);
				}
				
				for(DraftItem.Instance inst : item.getInstances()) {
					if(inst.getIdentifier() == null || inst.getIdentifier().isBlank())
						throw new SDSDException("All instances must have an identifier");
					if(inst.getLabel() == null || inst.getLabel().isBlank())
						throw new SDSDException("All instances must have a label");
					
					WikiInstance winst = cls.createInstance(inst.getIdentifier(), inst.getLabel());
					
					for(Entry<DraftItem.Ref, String> partof : inst.getPartOf().entrySet()) {
						winst.addPartOf(new WikiInstance(classes.deref(partof.getKey()), partof.getValue(), partof.getValue()));
					}
					
					for(Entry<Ref, Map<String, AttributeValues>> vtype : inst.getAttributes().entrySet()) {
						WikiEntry domain = vtype.getKey().ref.equals("self") ? cls : classes.deref(vtype.getKey());
						for(Entry<String, AttributeValues> vattr : vtype.getValue().entrySet()) {
							List<Object> values = vattr.getValue().getValues();
							if(values.isEmpty()) continue;
							
							Ref type = vattr.getValue().getType();
							if(type == null) throw new SDSDException("Attribute missing type");
							WikiEntry range = classes.deref(type);
							WikiAttribute attr = new WikiAttribute(domain, vattr.getKey(), vattr.getKey(), range);
							
							RDFDatatype rdftype = TypeMapper.getInstance().getTypeByName(range.getUri());
							
							List<RDFNode> nodes = new ArrayList<>(values.size());
							for(Object o : values) {
								if(rdftype == null && o instanceof String && ((String)o).lastIndexOf('_') > 0) {
									int index = ((String)o).lastIndexOf('_');
									Ref attrValType = Ref.of(((String)o).substring(0, index));
									if(attrValType != null && attrValType.isDraft()) o = TripleFunctions.createWikiInstanceUri(
											classes.deref(attrValType).getUri(), ((String)o).substring(index));
									nodes.add(ResourceFactory.createResource((String) o));
								} else if(rdftype != null && rdftype.isValid(o.toString()))
									nodes.add(ResourceFactory.createTypedLiteral(o.toString(), rdftype));
								else
									throw new SDSDException(String.format("Attribute value of wrong type: %s (%s)", 
											o.toString(), range.getLabel()));
							}
							winst.addAttribute(attr, nodes);
						}
					}
					
					winst.writeTo(model);
				}
				
				cls.writeTo(model);
			} catch (SDSDException e) {
				throw new SDSDException(item.getIdentifier() + ": " + e.getMessage(), e);
			}
		}
		
		app.list.types.add(user, SDSDType.create(user, formatCls.getUri(), format.getLabel(), format.getMimeType(), format.getArType()));
		app.triple.insertData(model, TripleFunctions.TBOX);
		boolean ok = app.list.draftFormats.delete(user, format.filter());
		if(ok) ok = app.list.draftItems.delete(user, DraftItem.filterFormat(format.getId()));
		System.out.format("Created format %s (%s) by %s: %b\n", format.getLabel(), format.getIdentifier(), user.getName(), ok);
		return ok;
	}
}
