package de.atextor.turtle.formatter;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFLanguages;

import java.io.StringReader;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;

public class TurtleFormatterPropertyTest {
    private final TurtleFormatter formatter = new TurtleFormatter( FormattingStyle.builder().build() );

    @Provide
    Arbitrary<String> anyString() {
        return Arbitraries.strings().ofMaxLength( 10 );
    }

    @Provide
    Arbitrary<Literal> anyStringLiteral() {
        return anyString().map( ResourceFactory::createStringLiteral );
    }

    @Provide
    Arbitrary<String> anyLanguageCode() {
        return Arbitraries.of( "en", "de" );
    }

    @Provide
    Arbitrary<Literal> anyLangStringLiteral() {
        return Combinators.combine( anyString(), anyLanguageCode() ).as( ResourceFactory::createLangLiteral );
    }

    @Provide
    Arbitrary<Literal> anyFloatLiteral() {
        return Arbitraries.floats()
            .map( value -> ResourceFactory.createTypedLiteral( value.toString(), XSDDatatype.XSDfloat ) );
    }

    @Provide
    Arbitrary<Literal> anyDoubleLiteral() {
        return Arbitraries.doubles()
            .map( value -> ResourceFactory.createTypedLiteral( value.toString(), XSDDatatype.XSDdouble ) );
    }

    @Provide
    Arbitrary<Literal> anyIntLiteral() {
        return Arbitraries.integers()
            .map( value -> ResourceFactory.createTypedLiteral( value.toString(), XSDDatatype.XSDint ) );
    }

    @Provide
    Arbitrary<Literal> anyLongLiteral() {
        return Arbitraries.longs()
            .map( value -> ResourceFactory.createTypedLiteral( value.toString(), XSDDatatype.XSDlong ) );
    }

    @Provide
    Arbitrary<Literal> anyShortLiteral() {
        return Arbitraries.shorts()
            .map( value -> ResourceFactory.createTypedLiteral( value.toString(), XSDDatatype.XSDshort ) );
    }

    @Provide
    Arbitrary<Literal> anyByteLiteral() {
        return Arbitraries.bytes()
            .map( value -> ResourceFactory.createTypedLiteral( value.toString(), XSDDatatype.XSDbyte ) );
    }

    @Provide
    Arbitrary<Literal> anyUnsignedByteLiteral() {
        return Arbitraries.integers().between( 0, 255 )
            .map( value -> ResourceFactory.createTypedLiteral( value.toString(), XSDDatatype.XSDunsignedByte ) );
    }

    @Provide
    Arbitrary<Literal> anyLiteral() {
        return Arbitraries
            .oneOf( anyStringLiteral(), anyLangStringLiteral(), anyFloatLiteral(), anyDoubleLiteral(),
                anyIntLiteral(), anyLongLiteral(), anyShortLiteral(), anyByteLiteral(), anyUnsignedByteLiteral() );
    }

    @Provide
    Arbitrary<Resource> anyAnonymousResource() {
        return Arbitraries.of( ResourceFactory.createResource() );
    }

    @Provide
    Arbitrary<String> anyUrl() {
        return Arbitraries.integers().between( 0, 100 ).map( number -> "http://example.com/" + number );
    }

    @Provide
    Arbitrary<String> anyUrn() {
        return Arbitraries.integers().map( number -> "urn:ex:" + number );
    }

    @Provide
    Arbitrary<String> anyUri() {
        return Arbitraries.oneOf( anyUrl(), anyUrn() );
    }

    @Provide
    Arbitrary<Resource> anyNamedResource() {
        return anyUri().map( ResourceFactory::createResource );
    }

    @Provide
    Arbitrary<Resource> anyResource() {
        return Arbitraries.oneOf( anyAnonymousResource(), anyNamedResource() );
    }

    @Provide
    Arbitrary<org.apache.jena.rdf.model.Property> anyProperty() {
        return anyUri().map( ResourceFactory::createProperty );
    }

    @Provide
    Arbitrary<Statement> anyStatement( final Model model ) {
        return Combinators.combine( anyResource(), anyProperty(), anyRdfNode( model ) )
            .as( ResourceFactory::createStatement );
    }

    @Provide
    Arbitrary<RDFNode> anyList( final Model model ) {
        final Supplier<Arbitrary<? extends RDFNode>> elements = () -> anyRdfNode( model );
        return Arbitraries.lazyOf( elements, elements ).list().ofMaxSize( 10 )
            .map( list -> model.createList( list.iterator() ) );
    }

    @Provide
    Arbitrary<RDFNode> anyRdfNode( final Model model ) {
        return Arbitraries.oneOf( anyLiteral(), anyResource(), anyList( model ) );
    }

    @Provide
    Arbitrary<List<Statement>> anyListOfStatements( final Model model ) {
        return anyStatement( model ).list().ofMaxSize( 5 );
    }

    @Provide
    Arbitrary<Model> anyModel() {
        final Model model = ModelFactory.createDefaultModel();
        return anyListOfStatements( model ).map( statements -> {
            model.add( statements );
            return model;
        } );
    }

    @Property
    public void anyPrettyPrintedModelIsSyntacticallyValid( @ForAll( "anyModel" ) final Model model ) {
        final String result = formatter.apply( model );
        final Model newModel = ModelFactory.createDefaultModel();
        try {
            newModel.read( new StringReader( result ), "", RDFLanguages.strLangTurtle );
        } catch ( final RuntimeException e ) {
            fail();
        }
    }
}
