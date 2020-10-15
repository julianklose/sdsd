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

    /** The qs. */
    private QuerySolution qs;
    
    /** The var name 2 qslist. */
    /*package*/ Map<String, List<CustomQuerySolution>> varName2qslist;

    /**
     * Instantiates a new custom query solution.
     *
     * @param qs the qs
     */
    public CustomQuerySolution(QuerySolution qs) {
        this.qs = qs;
        this.varName2qslist = new HashMap<>();
    }

    /**
     * Gets the.
     *
     * @param varName the var name
     * @return the RDF node
     */
    @Override
    public RDFNode get(String varName) {
        return qs.get(varName);
    }

    /**
     * Gets the resource.
     *
     * @param varName the var name
     * @return the resource
     */
    @Override
    public Resource getResource(String varName) {
        return qs.getResource(varName);
    }

    /**
     * Gets the literal.
     *
     * @param varName the var name
     * @return the literal
     */
    @Override
    public Literal getLiteral(String varName) {
        return qs.getLiteral(varName);
    }

    /**
     * List.
     *
     * @param varName the var name
     * @return the list
     */
    public List<CustomQuerySolution> list(String varName) {
        return varName2qslist.get(varName);
    }

    /**
     * Contains.
     *
     * @param varName the var name
     * @return true, if successful
     */
    @Override
    public boolean contains(String varName) {
        return qs.contains(varName) || varName2qslist.containsKey(varName);
    }

    /**
     * Var names.
     *
     * @return the iterator
     */
    @Override
    public Iterator<String> varNames() {
        List<String> list = new ArrayList<>();
        qs.varNames().forEachRemaining(list::add);
        list.addAll(varName2qslist.keySet());
        return list.iterator();
    }

    /**
     * Transform.
     *
     * @param qss the qss
     * @return the list
     */
    public static List<CustomQuerySolution> transform(List<QuerySolution> qss) {
        return qss.stream().map(q -> new CustomQuerySolution(q)).collect(toList());
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return StringEscapeUtils.escapeHtml4(qs.toString()).replaceAll("\\n", "<br/>");
    }
    
}
