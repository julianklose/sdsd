package de.sdsd.projekt.prototype.smartdatabrowser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static java.util.stream.Collectors.toList;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

/**
 * A Custom Query Solution that can hold recursively List&lt;CustomQuerySolution&gt;.
 * @author Markus Schr&ouml;der
 */
public class CustomQuerySolution implements QuerySolution {

    private QuerySolution qs;
    /*package*/ Map<String, List<CustomQuerySolution>> varName2qslist;

    public CustomQuerySolution(QuerySolution qs) {
        this.qs = qs;
        this.varName2qslist = new HashMap<>();
    }

    @Override
    public RDFNode get(String varName) {
        return qs.get(varName);
    }

    @Override
    public Resource getResource(String varName) {
        return qs.getResource(varName);
    }

    @Override
    public Literal getLiteral(String varName) {
        return qs.getLiteral(varName);
    }

    public List<CustomQuerySolution> list(String varName) {
        return varName2qslist.get(varName);
    }

    @Override
    public boolean contains(String varName) {
        return qs.contains(varName) || varName2qslist.containsKey(varName);
    }

    @Override
    public Iterator<String> varNames() {
        List<String> list = new ArrayList<>();
        qs.varNames().forEachRemaining(list::add);
        list.addAll(varName2qslist.keySet());
        return list.iterator();
    }

    public static List<CustomQuerySolution> transform(List<QuerySolution> qss) {
        return qss.stream().map(q -> new CustomQuerySolution(q)).collect(toList());
    }

    @Override
    public String toString() {
        return StringEscapeUtils.escapeHtml4(qs.toString()).replaceAll("\\n", "<br/>");
    }
    
}
