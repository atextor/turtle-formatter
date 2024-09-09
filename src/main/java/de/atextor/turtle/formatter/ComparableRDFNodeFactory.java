package de.atextor.turtle.formatter;

import de.atextor.turtle.formatter.blanknode.BlankNodeMetadata;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;

public class ComparableRDFNodeFactory {

    private final PrefixMapping prefixMapping;
    private final BlankNodeMetadata blankNodeOrdering;

    public ComparableRDFNodeFactory(PrefixMapping prefixMapping, BlankNodeMetadata blankNodeOrdering) {
        this.prefixMapping = prefixMapping;
        this.blankNodeOrdering = blankNodeOrdering;
    }

    public ComparableRDFNodeFactory(PrefixMapping prefixMapping) {
        this(prefixMapping, null);
    }

    public ComparableResource makeComparable(Resource resource){
        if (resource.isURIResource()) {
            return ComparableResource.of(prefixMapping.shortForm(resource.getURI()));
        } else if (resource.isAnon() && this.blankNodeOrdering != null){
            Long index = blankNodeOrdering.getOrder(resource.asNode());
            if (index != null){
                return ComparableResource.of(index);
            }
        }
        return ComparableResource.of(resource.toString());
    }

    public ComparableResource makeComparable(RDFNode rdfNode){
        if (rdfNode.isResource()){
            return makeComparable(rdfNode.asResource());
        }
        return ComparableResource.of(rdfNode.toString());
    }


    public static class ComparableResource implements Comparable<ComparableResource>{
        private final Long blankNodeIndex;
        private final String stringValue;

        private ComparableResource(Long blankNodeIndex, String stringValue) {
            this.blankNodeIndex = blankNodeIndex;
            this.stringValue = stringValue;
        }

        private static ComparableResource of(Long blankNodeIndex){
            return new ComparableResource(blankNodeIndex, null);
        }

        private static ComparableResource of(String stringValue){
            return new ComparableResource(null, stringValue);
        }

        private boolean hasBlankNodeIndex(){
            return this.blankNodeIndex != null;
        }

        private boolean hasStringValue(){
            return this.stringValue != null;
        }

        @Override public int compareTo(ComparableResource other) {
            if (this.hasBlankNodeIndex()){
                if (other.hasBlankNodeIndex()){
                    return this.blankNodeIndex.compareTo(other.blankNodeIndex);
                }
                else if (other.hasStringValue()){
                    return 1; //strings first
                }
            } else if (this.hasStringValue()){
                if (other.hasStringValue()){
                    return this.stringValue.compareTo(other.stringValue);
                } else if (other.hasBlankNodeIndex()) {
                    return -1; //strings first
                }
            }
            throw new IllegalStateException("ComparableResource must have blankNodeIndex or String Value!");
        }
    }

}
