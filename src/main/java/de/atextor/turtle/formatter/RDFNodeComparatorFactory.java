package de.atextor.turtle.formatter;

import de.atextor.turtle.formatter.blanknode.BlankNodeMetadata;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;

import java.util.Comparator;
import java.util.Optional;

public class RDFNodeComparatorFactory {

    private final PrefixMapping prefixMapping;
    private final BlankNodeMetadata blankNodeOrdering;
    private final RDFNodeComparator rdfNodeComparator = new RDFNodeComparator();

    public RDFNodeComparatorFactory(PrefixMapping prefixMapping, BlankNodeMetadata blankNodeOrdering) {
        this.prefixMapping = prefixMapping;
        this.blankNodeOrdering = blankNodeOrdering;
    }

    public RDFNodeComparatorFactory(PrefixMapping prefixMapping) {
        this(prefixMapping, null);
    }

    public RDFNodeComparator comparator() {
        return rdfNodeComparator;
    }

    private class RDFNodeComparator implements Comparator<RDFNode> {
        @Override public int compare(RDFNode left, RDFNode right) {
            if (left.isURIResource()){
                if (right.isURIResource()){
                    return prefixMapping.shortForm(left.asResource().getURI()).compareTo(prefixMapping.shortForm(right.asResource().getURI()));
                } else if (right.isAnon()) {
                    return -1 ; // uris first
                }
            } else if (left.isAnon()) {
                if (right.isAnon()) {
                    if (blankNodeOrdering != null) {
                        return Optional.ofNullable(blankNodeOrdering.getOrder(left.asResource().asNode()))
                                        .orElse(Long.MAX_VALUE)
                                        .compareTo(Optional.ofNullable(
                                                                        blankNodeOrdering.getOrder(right.asResource().asNode()))
                                                        .orElse(Long.MAX_VALUE));
                    }
                } else if (right.isResource()) {
                    return 1; // uris first
                }
            }
            //fall-through for all other cases, especially if we don't have a blank node ordering
            return left.toString().compareTo(right.toString());
        }
    }
}
