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

import java.io.StringReader;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;

public class TurtleFormatterPropertyTest {
    @Provide
    Arbitrary<String> anyString() {
        return Arbitraries.strings().ofMaxLength( 5 );
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

    // Providing only one for each of the integer number types is sufficient for testing the formatter
    // as there is no variation in their serialzition, but greatly reduces the test space
    @Provide
    Arbitrary<Literal> anyIntegerNumberLiteral() {
        return Arbitraries.of(
            ResourceFactory.createTypedLiteral( "1", XSDDatatype.XSDint ),
            ResourceFactory.createTypedLiteral( "2", XSDDatatype.XSDlong ),
            ResourceFactory.createTypedLiteral( "3", XSDDatatype.XSDbyte ),
            ResourceFactory.createTypedLiteral( "4", XSDDatatype.XSDbyte ),
            ResourceFactory.createTypedLiteral( "5", XSDDatatype.XSDunsignedByte )
        );
    }

    @Provide
    Arbitrary<Literal> anyLiteral() {
        return Arbitraries.oneOf( anyStringLiteral(), anyLangStringLiteral(), anyFloatLiteral(), anyDoubleLiteral(),
            anyIntegerNumberLiteral() );
    }

    @Provide
    Arbitrary<Resource> anyAnonymousResource() {
        return Arbitraries.of( ResourceFactory.createResource() );
    }

    @Provide
    Arbitrary<String> anyUrl() {
        return Arbitraries.integers().between( 0, 10 ).map( number -> "http://example.com/" + number );
    }

    @Provide
    Arbitrary<String> anyUrn() {
        return Arbitraries.integers().between( 0, 10 ).map( number -> "urn:ex:" + number );
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
        return Arbitraries.lazyOf( elements, elements ).list().ofMaxSize( 3 )
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

    // Creates styles for some of the options that control the actual formatting and are difficult to test
    // in isolation.
    @Provide
    Arbitrary<FormattingStyle> anyStyle() {
        final Arbitrary<Boolean> onOff = Arbitraries.of( true, false );

        return Combinators.combine( onOff, onOff, onOff, onOff, onOff )
            .as( ( firstPredicateInNewLine, useAForRdfType, useComma, alignPredicates, alignObjects ) ->
                FormattingStyle.builder()
                    .firstPredicateInNewLine( firstPredicateInNewLine )
                    .useAForRdfType( useAForRdfType )
                    .useCommaByDefault( useComma )
                    .alignPredicates( alignPredicates )
                    .alignObjects( alignObjects )
                    .build() );
    }

    @Provide
    Arbitrary<Model> anyModel() {
        final Model model = ModelFactory.createDefaultModel();
        return anyListOfStatements( model ).map( statements -> {
            model.add( statements );
            return model;
        } );
    }

    @Property( tries = 300 )
    public void anyPrettyPrintedModelIsSyntacticallyValid( @ForAll( "anyModel" ) final Model model,
                                                           @ForAll( "anyStyle" ) final FormattingStyle style ) {
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        final Model newModel = ModelFactory.createDefaultModel();
        try {
            newModel.read( new StringReader( result ), "", "TURTLE" );
        } catch ( final RuntimeException e ) {
            fail();
        }
    }
}
