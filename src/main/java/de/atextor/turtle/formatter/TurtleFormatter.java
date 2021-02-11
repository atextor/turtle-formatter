package de.atextor.turtle.formatter;

import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.collection.Stream;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.function.Function;

public class TurtleFormatter implements Function<Model, String> {
    private static final Logger LOG = LoggerFactory.getLogger( TurtleFormatter.class );

    private final FormattingStyle style;

    private final String beforeDot;

    private final String endOfLine;

    private final Comparator<Tuple2<String, String>> prefixOrder;

    private final Comparator<RDFNode> objectOrder;

    public TurtleFormatter( final FormattingStyle style ) {
        this.style = style;

        endOfLine = switch ( style.endOfLine ) {
            case CR -> "\r";
            case LF -> "\n";
            case CRLF -> "\r\n";
        };

        beforeDot = switch ( style.beforeDot ) {
            case SPACE -> " ";
            case NOTHING -> "";
            case NEWLINE -> endOfLine;
        };

        prefixOrder = Comparator.<Tuple2<String, String>>comparingInt( entry ->
            style.prefixOrder.contains( entry._1() ) ?
                style.prefixOrder.indexOf( entry._1() ) :
                Integer.MAX_VALUE
        ).thenComparing( Tuple2::_1 );

        objectOrder = Comparator.<RDFNode>comparingInt( object ->
            style.objectOrder.contains( object ) ?
                style.objectOrder.indexOf( object ) :
                Integer.MAX_VALUE
        ).thenComparing( RDFNode::toString );
    }

    private static List<Statement> statements( final Model model ) {
        return List.ofAll( model.listStatements().toList() );
    }

    private static List<Statement> statements( final Model model, final Resource subject, final Property predicate,
                                               final RDFNode object ) {
        return List.ofAll( model.listStatements( subject, predicate, object ).toList() );
    }

    @Override
    public String apply( final Model model ) {
        final PrefixMapping prefixMapping = buildPrefixMapping( model );

        final Comparator<Property> predicateOrder = Comparator.<Property>comparingInt( property ->
            style.predicateOrder.contains( property ) ?
                style.predicateOrder.indexOf( property ) :
                Integer.MAX_VALUE
        ).thenComparing( property -> prefixMapping.shortForm( property.getURI() ) );

        final State initialState = Stream
            .ofAll( anonymousResourcesThatNeedAnId( model ) )
            .zipWithIndex()
            .map( entry -> new Tuple2<>( entry._1(), style.anonymousNodeIdGenerator.apply( entry._1(), entry._2() ) ) )
            .foldLeft( new State( model, predicateOrder, prefixMapping ), ( state, entry ) ->
                state.withIdentifiedAnonymousResource( entry._1(), entry._2() ) );

        final State prefixesWritten = writePrefixes( initialState );

        final Comparator<Statement> subjectComparator =
            Comparator.comparing( statement -> statement.getSubject().isURIResource() ?
                prefixMapping.shortForm( statement.getSubject().getURI() ) : statement.getSubject().toString() );

        final List<Statement> wellKnownSubjects = List.ofAll( style.subjectOrder ).flatMap( subjectType ->
            statements( model, null, RDF.type, subjectType ).sorted( subjectComparator ) );
        final List<Statement> otherSubjects = statements( model )
            .filter( statement -> !statement.getPredicate().equals( RDF.type ) )
            .sorted( subjectComparator );
        final List<Statement> statements = wellKnownSubjects.appendAll( otherSubjects )
            .filter( statement -> !( statement.getSubject().isAnon()
                && model.contains( null, null, statement.getSubject() ) ) );

        final State namedResourcesWritten = statements
            .map( Statement::getSubject )
            .foldLeft( prefixesWritten, ( state, resource ) ->
                resource.isURIResource() ? writeSubject( resource, state.withIndentationLevel( 0 ) ) :
                    writeAnonymousResource( resource, state.withIndentationLevel( 0 ) ) );

        final State finalState = List.ofAll( namedResourcesWritten.identifiedAnonymousResources.keySet() )
            .foldLeft( namedResourcesWritten, ( state, resource ) ->
                writeSubject( resource, state.withIndentationLevel( 0 ) ) );

        LOG.debug( "Written {} resources, with {} named anonymous resources", finalState.visitedResources.size(),
            finalState.identifiedAnonymousResources.size() );

        return finalState.print();
    }

    /**
     * Anonymous resources that are referred to more than once need to be given an internal id and
     * can not be serialized using [ ] notation.
     *
     * @param model the input model
     * @return the set of anonymous resources that are referred to more than once
     */
    private Set<Resource> anonymousResourcesThatNeedAnId( final Model model ) {
        return List.ofAll( model::listObjects )
            .filter( RDFNode::isResource )
            .map( RDFNode::asResource )
            .filter( RDFNode::isAnon )
            .filter( object -> statements( model, null, null, object ).toList().size() > 1 )
            .toSet();
    }

    private PrefixMapping buildPrefixMapping( final Model model ) {
        final Map<String, String> prefixMap = Stream.ofAll( style.knownPrefixes )
            .filter( knownPrefix -> model.getNsPrefixURI( knownPrefix.getPrefix() ) == null )
            .toMap( FormattingStyle.KnownPrefix::getPrefix, knownPrefix -> knownPrefix.getIri().toString() );
        return PrefixMapping.Factory.create().setNsPrefixes( model.getNsPrefixMap() )
            .setNsPrefixes( prefixMap.toJavaMap() );
    }

    private State writePrefixes( final State state ) {
        final Map<String, String> prefixes = HashMap.ofAll( state.prefixMapping.getNsPrefixMap() );
        final int maxPrefixLength = prefixes.keySet().map( String::length ).max().getOrElse( 0 );
        final String prefixFormat = switch ( style.alignPrefixes ) {
            case OFF -> "@prefix %s: <%s>" + beforeDot + ".%n";
            case LEFT -> "@prefix %-" + maxPrefixLength + "s: <%s>" + beforeDot + ".%n";
            case RIGHT -> "@prefix %" + maxPrefixLength + "s: <%s>" + beforeDot + ".%n";
        };

        final State prefixesWritten = prefixes.toStream().sorted( prefixOrder ).foldLeft( state,
            ( newState, entry ) -> newState.write( String.format( prefixFormat, entry._1(), entry._2() ) ) );

        return prefixesWritten.newLine();
    }

    private String indent( final int level ) {
        final String singleIndent = switch ( style.indentStyle ) {
            case SPACE -> " ".repeat( style.indentSize );
            case TAB -> "\\t";
        };
        return singleIndent.repeat( level );
    }

    private String continuationIndent( final int level ) {
        final String continuation = switch ( style.indentStyle ) {
            case SPACE -> " ".repeat( style.continuationIndentSize );
            case TAB -> "\\t".repeat( 2 );
        };
        return indent( level - 1 ) + continuation;
    }

    private State writeDelimiter( final String delimiter, final FormattingStyle.GapStyle before,
                                  final FormattingStyle.GapStyle after, final String indentation,
                                  final State state ) {
        final State beforeState = switch ( before ) {
            case SPACE -> state.write( " " );
            case NOTHING -> state;
            case NEWLINE -> state.newLine().write( indentation );
        };

        return switch ( after ) {
            case SPACE -> beforeState.write( delimiter + " " );
            case NOTHING -> beforeState.write( delimiter );
            case NEWLINE -> beforeState.write( delimiter ).newLine().write( indentation );
        };
    }

    private State writeComma( final State state ) {
        return writeDelimiter( ",", style.beforeComma, style.afterComma,
            continuationIndent( state.indentationLevel ), state );
    }

    private State writeSemicolon( final State state, final boolean omitLineBreak,
                                  final boolean omitSpaceBeforeSemicolon ) {
        final FormattingStyle.GapStyle beforeSemicolon = omitSpaceBeforeSemicolon ? FormattingStyle.GapStyle.NOTHING :
            style.beforeSemicolon;
        final FormattingStyle.GapStyle afterSemicolon = omitLineBreak ? FormattingStyle.GapStyle.NOTHING :
            style.afterSemicolon;
        return writeDelimiter( ";", beforeSemicolon, afterSemicolon,
            style.alignPredicates ? "" : indent( state.indentationLevel ), state );
    }

    private State writeDot( final State state, final boolean omitSpaceBeforeDot ) {
        final FormattingStyle.GapStyle beforeDot = omitSpaceBeforeDot ? FormattingStyle.GapStyle.NOTHING :
            style.beforeDot;
        return writeDelimiter( ".", beforeDot, style.afterDot, "", state );
    }

    private State writeOpeningSquareBracket( final State state ) {
        final FormattingStyle.GapStyle beforeBracket = state.indentationLevel > 0 ? style.beforeOpeningSquareBracket :
            FormattingStyle.GapStyle.NOTHING;
        return writeDelimiter( "[", beforeBracket, style.afterOpeningSquareBracket,
            indent( state.indentationLevel ), state );
    }

    private State writeClosingSquareBracket( final State state ) {
        return writeDelimiter( "]", style.beforeClosingSquareBracket, style.afterClosingSquareBracket,
            indent( state.indentationLevel ), state );
    }

    private boolean isList( final RDFNode node, final State state ) {
        return node.equals( RDF.nil ) ||
            ( node.isResource() && state.model.contains( node.asResource(), RDF.rest, (RDFNode) null ) );
    }

    private State writeResource( final Resource resource, final State state ) {
        if ( isList( resource, state ) ) {
            return writeList( resource, state );
        }
        if ( resource.isURIResource() ) {
            return writeUriResource( resource, state );
        }
        return writeAnonymousResource( resource, state );
    }

    private State writeList( final Resource resource, final State state ) {
        final State opened = writeDelimiter( "(", style.beforeOpeningParenthesis, style.afterOpeningParenthesis,
            continuationIndent( state.indentationLevel ), state );
        final java.util.List<RDFNode> elementList = resource.as( RDFList.class ).asJavaList();
        final State elementsWritten = List.ofAll( elementList ).zipWithIndex()
            .foldLeft( opened, ( currentState, indexedElement ) -> {
                RDFNode element = indexedElement._1();
                int index = indexedElement._2();
                boolean firstElement = index == 0;
                State spaceWritten = firstElement ? currentState : currentState.write( " " );
                return writeRdfNode( element, spaceWritten );
            } );

        return writeDelimiter( ")", style.beforeClosingParenthesis, style.afterClosingParenthesis,
            continuationIndent( state.indentationLevel ), elementsWritten );
    }

    private State writeAnonymousResource( final Resource resource, final State state ) {
        if ( state.identifiedAnonymousResources.keySet().contains( resource ) ) {
            return state.write( state.identifiedAnonymousResources.getOrElse( resource, "" ) );
        }

        if ( !state.model.contains( resource, null, (RDFNode) null ) ) {
            return state.write( "[]" );

        }
        final State afterOpeningSquareBracket = writeOpeningSquareBracket( state );
        final State afterContent = writeSubject( resource, afterOpeningSquareBracket ).removeIndentationLevel();
        return writeClosingSquareBracket( afterContent );
    }

    private State writeUriResource( final Resource resource, final State state ) {
        if ( resource.getURI().equals( RDF.type.getURI() ) && style.useAForRdfType ) {
            return state.write( "a" );
        }

        final String uri = resource.getURI();
        final String shortForm = state.prefixMapping.shortForm( uri );
        return state.write( shortForm.equals( uri ) ? "<" + uri + ">" : shortForm );
    }

    private State writeLiteral( final Literal literal, final State state ) {
        if ( literal.getDatatypeURI().equals( XSD.xboolean.getURI() ) ) {
            return state.write( literal.getBoolean() ? "true" : "false" );
        }
        if ( literal.getDatatypeURI().equals( XSD.xstring.getURI() ) ) {
            return state.write( "\"" + literal.getValue().toString() + "\"" );
        }
        if ( literal.getDatatypeURI().equals( XSD.decimal.getURI() ) ) {
            return state.write( literal.getLexicalForm() );
        }
        if ( literal.getDatatypeURI().equals( XSD.integer.getURI() ) ) {
            return state.write( literal.getValue().toString() );
        }
        if ( literal.getDatatypeURI().equals( XSD.xdouble.getURI() ) ) {
            return state.write( "" + literal.getDouble() );
        }
        if ( literal.getDatatypeURI().equals( RDF.langString.getURI() ) ) {
            return state.write( "\"" + literal.getLexicalForm() + "\"@" + literal.getLanguage() );
        }

        final Resource typeResource = ResourceFactory.createResource( literal.getDatatypeURI() );
        final State literalWritten = state.write( "\"" + literal.getLexicalForm() + "\"^^" );
        return writeUriResource( typeResource, literalWritten );
    }

    private State writeRdfNode( final RDFNode node, final State state ) {
        if ( node.isResource() ) {
            return writeResource( node.asResource(), state );
        }

        if ( node.isLiteral() ) {
            return writeLiteral( node.asLiteral(), state );
        }

        return state;
    }

    private State writeProperty( final Property property, final State state ) {
        return writeUriResource( property, state );
    }

    private State writeSubject( final Resource resource, final State state ) {
        if ( state.visitedResources.contains( resource ) ) {
            return state;
        }

        // indent
        final State indentedSubject = state.write( indent( state.indentationLevel ) );
        // subject
        final boolean isIdentifiedAnon = state.identifiedAnonymousResources.keySet().contains( resource );
        final State stateWithSubject = resource.isURIResource() || isIdentifiedAnon ?
            writeResource( resource, indentedSubject ).withVisitedResource( resource ) :
            indentedSubject.withVisitedResource( resource );

        final State gapAfterSubject = style.firstPredicateInNewLine || ( resource.isAnon() && !isIdentifiedAnon ) ?
            stateWithSubject : stateWithSubject.write( " " );

        final int predicateAlignment = style.firstPredicateInNewLine ? style.indentSize : gapAfterSubject.alignment;

        // predicates and objects
        final Set<Property> properties = Stream.ofAll( resource::listProperties )
            .map( Statement::getPredicate ).toSet();

        return Stream
            .ofAll( properties )
            .sorted( state.predicateOrder )
            .zipWithIndex()
            .foldLeft( stateWithSubject.addIndentationLevel(), ( currentState, indexedProperty ) -> {
                final Property property = indexedProperty._1();
                final int index = indexedProperty._2();
                final boolean firstProperty = index == 0;
                final boolean lastProperty = index == properties.size() - 1;
                return writeProperty( resource, property, firstProperty, lastProperty, predicateAlignment,
                    currentState );
            } );
    }

    private State writeProperty( final Resource subject, final Property predicate, final boolean firstProperty,
                                 final boolean lastProperty, final int alignment, final State state ) {
        final Set<RDFNode> objects =
            Stream.ofAll( () -> subject.listProperties( predicate ) ).map( Statement::getObject ).toSet();

        final boolean useComma = ( style.useCommaByDefault && !style.noCommaForPredicate.contains( predicate ) )
            || ( !style.useCommaByDefault && style.commaForPredicate.contains( predicate ) );

        final State wrappedPredicate = firstProperty && style.firstPredicateInNewLine ?
            state.newLine().write( indent( state.indentationLevel ) ) : state;

        final State predicateAlignment = !firstProperty && style.alignPredicates ?
            wrappedPredicate.write( " ".repeat( alignment ) ) : wrappedPredicate;

        final State predicateWrittenOnce = useComma ?
            writeProperty( predicate, predicateAlignment ).write( " " ) : predicateAlignment;

        return Stream
            .ofAll( objects )
            .sorted( objectOrder )
            .zipWithIndex()
            .foldLeft( predicateWrittenOnce, ( currentState, indexedObject ) -> {
                final RDFNode object = indexedObject._1();
                final int index = indexedObject._2();
                final boolean lastObject = index == objects.size() - 1;

                final State predicateWritten = useComma ? currentState :
                    writeProperty( predicate, currentState );

                final boolean isAnonWithBrackets = object.isAnon()
                    && !predicateWritten.identifiedAnonymousResources.keySet().contains( object.asResource() );
                final State spaceWritten = !isAnonWithBrackets && !isList( object, predicateWritten ) && !useComma ?
                    predicateWritten.write( " " ) :
                    predicateWritten;

                final State objectWritten = writeRdfNode( object, spaceWritten );
                if ( useComma && !lastObject ) {
                    return writeComma( objectWritten );
                }

                final boolean omitSpaceBeforeDelimiter = object.isResource()
                    && object.isAnon()
                    && !( isList( object, objectWritten ) && style.afterClosingParenthesis == FormattingStyle.GapStyle.NOTHING )
                    && !currentState.identifiedAnonymousResources.keySet().contains( object.asResource() );
                if ( lastProperty && lastObject && objectWritten.indentationLevel == 1 ) {
                    return writeDot( objectWritten, omitSpaceBeforeDelimiter ).newLine();
                }
                return writeSemicolon( objectWritten, lastProperty && lastObject, omitSpaceBeforeDelimiter );
            } );
    }

    @Value
    @With
    @AllArgsConstructor
    private class State {
        StringBuffer buffer;

        Model model;

        Set<Resource> visitedResources;

        Map<Resource, String> identifiedAnonymousResources;

        Comparator<Property> predicateOrder;

        PrefixMapping prefixMapping;

        int indentationLevel;

        int alignment;

        public State( final Model model, final Comparator<Property> predicateOrder,
                      final PrefixMapping prefixMapping ) {
            this( new StringBuffer(), model, HashSet.empty(), HashMap.empty(), predicateOrder, prefixMapping, 0, 0 );
        }

        public State withIdentifiedAnonymousResource( final Resource anonymousResource, final String id ) {
            return withIdentifiedAnonymousResources( identifiedAnonymousResources.put( anonymousResource, id ) );
        }

        public State withVisitedResource( final Resource visitedResource ) {
            return withVisitedResources( visitedResources.add( visitedResource ) );
        }

        public State addIndentationLevel() {
            return withIndentationLevel( indentationLevel + 1 );
        }

        public State removeIndentationLevel() {
            return withIndentationLevel( indentationLevel - 1 );
        }

        public State newLine() {
            return write( endOfLine ).withAlignment( 0 );
        }

        public State write( final String content ) {
            // Interface pretends to use immutable data structures, while the implementation actually reuses the
            // same StringBuffer
            buffer.append( content );
            return withAlignment( alignment + content.length() );
        }

        public String print() {
            return buffer.toString();
        }
    }
}
