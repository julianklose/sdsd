package de.sdsd.projekt.prototype.data;

import static de.sdsd.projekt.prototype.applogic.TripleFunctions.VALUEINFO;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;

import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.QueryResult;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilQuerySolution;

/**
 * Represents a presentation information for a timelog value from the triplestore.
 * 
 * @see Timelog
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ValueInfo {
	private static final Var VURI = Var.alloc("vuri"), DESIG = Var.alloc("desig"), 
			OFFSET = Var.alloc("offset"), SCALE = Var.alloc("scale"), NOD = Var.alloc("nod"), UNIT = Var.alloc("unit");
	
	public static List<ValueInfo> getValueInfos(ApplicationLogic app, String fileUri, @Nullable List<String> valueUris) {
		if(valueUris != null && valueUris.isEmpty()) return Collections.emptyList();
		SelectBuilder query = new SelectBuilder()
				.addVar(VURI).addVar(DESIG).addVar(OFFSET).addVar(SCALE).addVar(NOD).addVar(UNIT)
				.from(fileUri)
				.addWhere(VURI, RDF.type, Var.ANON)
				.addOptional(VURI, VALUEINFO.prop("designator"), DESIG)
				.addOptional(VURI, VALUEINFO.prop("offset"), OFFSET)
				.addOptional(VURI, VALUEINFO.prop("scale"), SCALE)
				.addOptional(VURI, VALUEINFO.prop("numberOfDecimals"), NOD)
				.addOptional(VURI, VALUEINFO.prop("unit"), UNIT);
		if(valueUris != null)
			query.addValueVar(VURI, valueUris.stream().map(NodeFactory::createURI).toArray());
		List<ValueInfo> list = new ArrayList<>(valueUris != null ? valueUris.size() : 10);
		try(QueryResult qr = app.triple.query(query.build())) {
			for(UtilQuerySolution qs : qr.iterate()) {
				list.add(new ValueInfo(qs));
			}
		}
		return list;
	}
	
	public static ValueInfo getValueInfo(ApplicationLogic app, String fileUri, String valueUri) {
		return getValueInfos(app, fileUri, Collections.singletonList(valueUri)).get(0);
	}
	
	public final String valueUri;
	public final String designator;
	public final long offset;
	public final double scale;
	public final int numberOfDecimals;
	public final String unit;
	
	private ValueInfo(UtilQuerySolution qs) {
		this.valueUri = qs.getUri(VURI);
		this.designator = qs.contains(DESIG) ? qs.getString(DESIG) : "";
		this.offset = qs.contains(OFFSET) ? qs.getLiteral(OFFSET).getLong() : 0;
		this.scale = qs.contains(SCALE) ? qs.getLiteral(SCALE).getDouble() : 1.;
		this.numberOfDecimals = qs.contains(NOD) ? qs.getLiteral(NOD).getInt() : 0;
		this.unit = qs.contains(UNIT) ? qs.getString(UNIT) : "";
	}
	
	private ValueInfo(String valueUri) {
		this.valueUri = valueUri;
		this.designator = "";
		this.offset = 0;
		this.scale = 1.;
		this.numberOfDecimals = 0;
		this.unit = "";
	}
	
	public String valueUri() {
		return valueUri;
	}

	public String getDesignator() {
		return designator;
	}
	
	public long getOffset() {
		return offset;
	}
	
	public double getScale() {
		return scale;
	}

	public int getNumberOfDecimals() {
		return numberOfDecimals;
	}

	public String getUnit() {
		return unit;
	}
	
	public double translateValue(long value) {
		return new BigDecimal(value)
				.add(new BigDecimal(offset))
				.multiply(new BigDecimal(scale))
				.doubleValue();
	}
	
	public String formatValue(double translatedValue) {
		NumberFormat format = DecimalFormat.getInstance();
		format.setMaximumFractionDigits(numberOfDecimals);
		return format.format(translatedValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(valueUri);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ValueInfo))
			return false;
		ValueInfo other = (ValueInfo) obj;
		return Objects.equals(valueUri, other.valueUri);
	}
}
