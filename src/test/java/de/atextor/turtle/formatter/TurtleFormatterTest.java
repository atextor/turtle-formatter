package de.atextor.turtle.formatter;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TurtleFormatterTest {
    @Test
    public void testPrefixAlignmentLeft() {
        final Model model = prefixModel();
        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .beforeDot( FormattingStyle.GapStyle.SPACE )
            .alignPrefixes( FormattingStyle.Alignment.LEFT )
            .insertFinalNewline( false )
            .keepUnusedPrefixes( true )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        final String expected = """
            @prefix       : <http://example.com/> .
            @prefix a     : <http://example.com/a> .
            @prefix abc   : <http://example.com/abc> .
            @prefix abcdef: <http://example.com/abc> .

            """;
        assertThat( result ).isEqualTo( expected );
    }

    @Test
    public void testPrefixAlignmentOff() {
        final Model model = prefixModel();
        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .beforeDot( FormattingStyle.GapStyle.SPACE )
            .alignPrefixes( FormattingStyle.Alignment.OFF )
            .insertFinalNewline( false )
            .keepUnusedPrefixes( true )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        final String expected = """
            @prefix : <http://example.com/> .
            @prefix a: <http://example.com/a> .
            @prefix abc: <http://example.com/abc> .
            @prefix abcdef: <http://example.com/abc> .

            """;
        assertThat( result ).isEqualTo( expected );
    }

    @Test
    public void testPrefixAlignmentRight() {
        final Model model = prefixModel();
        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .beforeDot( FormattingStyle.GapStyle.SPACE )
            .alignPrefixes( FormattingStyle.Alignment.RIGHT )
            .insertFinalNewline( false )
            .keepUnusedPrefixes( true )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        final String expected = """
            @prefix       : <http://example.com/> .
            @prefix      a: <http://example.com/a> .
            @prefix    abc: <http://example.com/abc> .
            @prefix abcdef: <http://example.com/abc> .

            """;
        assertThat( result ).isEqualTo( expected );
    }

    @Test
    public void testLiterals() {
        final String modelString = """
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            @prefix : <http://example.com/> .

            :foo1 :bar 1 .

            :foo2 :bar "2" .

            :foo3 :bar true .

            :foo4 :bar -5.0 .

            :foo5 :bar 4.2E9 .

            :foo6 :bar "2021-01-01"^^xsd:date .

            :foo7 :bar "something"^^:custom .

            :foo8 :bar "something"@en .
            """;
        final Model model = modelFromString( modelString );
        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        assertThat( result.trim() ).isEqualTo( modelString.trim() );
    }

    @Test
    public void testPredicateAlignmentWithFirstPredicateInSameLine() {
        final String modelString = """
            @prefix : <http://example.com/> .

            :foo1 :bar1 1 ;
                  :bar2 2 ;
                  :bar3 3 .

            :something :bar1 1 ;
                       :bar2 2 ;
                       :bar3 3 .
            """;
        final Model model = modelFromString( modelString );

        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .alignPredicates( true )
            .firstPredicateInNewLine( false )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        assertThat( result.trim() ).isEqualTo( modelString.trim() );
    }

    @Test
    public void testIndentationWithTabs() {
        final String modelString = """
            @prefix : <http://example.com/> .

            :foo1 :bar1 1 ;
            \t:bar2 2 ;
            \t:bar3 3 .
            """;
        final Model model = modelFromString( modelString );

        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .firstPredicateInNewLine( false )
            .indentStyle( FormattingStyle.IndentStyle.TAB )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        assertThat( result.trim() ).isEqualTo( modelString.trim() );
    }

    @Test
    public void testPredicateAlignmentWithFirstPredicateInNewLine() {
        final String modelString = """
            @prefix : <http://example.com/> .

            :foo1
              :bar1 1 ;
              :bar2 2 ;
              :bar3 3 .

            :something
              :bar1 1 ;
              :bar2 2 ;
              :bar3 3 .
            """;
        final Model model = modelFromString( modelString );

        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .firstPredicateInNewLine( true )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        assertThat( result.trim() ).isEqualTo( modelString.trim() );
    }

    @Test
    public void testPredicateAndObjectAlignment() {
        final String modelString = """
            @prefix : <http://example.com/> .

            :foo1 :bar       1 ;
                  :bar234    2 ;
                  :bar567890 3 .
            """;
        final Model model = modelFromString( modelString );

        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .firstPredicateInNewLine( false )
            .alignPredicates( true )
            .alignObjects( true )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        assertThat( result.trim() ).isEqualTo( modelString.trim() );
    }

    @Test
    public void testPredicateAndObjectAlignmentWithFirstPredicateInNewLine() {
        final String modelString = """
            @prefix : <http://example.com/> .

            :foo1
              :bar       1 ;
              :bar234    2 ;
              :bar567890 3 .
            """;
        final Model model = modelFromString( modelString );

        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .firstPredicateInNewLine( true )
            .alignPredicates( true )
            .alignObjects( true )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        assertThat( result.trim() ).isEqualTo( modelString.trim() );
    }

    @Test
    public void testMultipleReferencedAnonNodes() {
        // Note how the anonymous node can not be serialized using [ ] because it is referenced multiple times.
        final String modelString = """
            @prefix : <http://example.com/> .

            :a :foo _:gen0 ;
              :bar 1 .

            :b :foo _:gen0 .

            _:gen0 :bar 2 .
            """;
        final Model model = modelFromString( modelString );

        final String ex = "http://example.com/";
        final Property foo = ResourceFactory.createProperty( ex + "foo" );
        final Property bar = ResourceFactory.createProperty( ex + "bar" );
        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .predicateOrder( List.of( foo, bar ) )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        assertThat( result.trim() ).isEqualTo( modelString.trim() );
    }

    @Test
    public void testTopLevelAnonymousNode() {
        final String modelString = """
            @prefix : <http://example.com/> .

            [
              :foo 1 ;
              :bar 2 ;
              :baz 3 ;
            ] .
            """;
        final Model model = modelFromString( modelString );

        final String ex = "http://example.com/";
        final Property foo = ResourceFactory.createProperty( ex + "foo" );
        final Property bar = ResourceFactory.createProperty( ex + "bar" );
        final Property baz = ResourceFactory.createProperty( ex + "baz" );
        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .predicateOrder( List.of( foo, bar, baz ) )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        assertThat( result.trim() ).isEqualTo( modelString.trim() );
    }

    @Test
    public void testListWrappingNever() {
        final String modelString = """
            @prefix : <http://example.com/> .

            :foo :bar ( "foo1" "foo2" "foo3" "foo4" ) .
            """;
        // Line length ruler
        //  ###############################################
        //  1        10        20        30        40
        final Model model = modelFromString( modelString );

        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .maxLineLength( 30 )
            .wrapListItems( FormattingStyle.WrappingStyle.NEVER )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        assertThat( result.trim() ).isEqualTo( modelString.trim() );
    }

    @Test
    public void testListWrappingAlways() {
        final String modelString = """
            @prefix : <http://example.com/> .

            :foo :bar ( "foo1" "foo2" "foo3" "foo4" ) .
            """;
        // Line length ruler
        //  ###############################################
        //  1        10        20        30        40
        final Model model = modelFromString( modelString );

        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .maxLineLength( 30 )
            .wrapListItems( FormattingStyle.WrappingStyle.ALWAYS )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        final String expected = """
            @prefix : <http://example.com/> .
                        
            :foo :bar (
                "foo1"
                "foo2"
                "foo3"
                "foo4"
               ) .
            """;
        assertThat( result.trim() ).isEqualTo( expected.trim() );
    }

    @Test
    public void testListWrappingForLongLines() {
        final String modelString = """
            @prefix : <http://example.com/> .

            :foo :bar ( "foo1" "foo2" "foo3" "foo4" ) .
            """;
        // Line length ruler
        //  ###############################################
        //  1        10        20        30        40
        final Model model = modelFromString( modelString );

        final FormattingStyle style = FormattingStyle.builder()
            .knownPrefixes( Set.of() )
            .maxLineLength( 30 )
            .wrapListItems( FormattingStyle.WrappingStyle.FOR_LONG_LINES )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        final String expected = """
            @prefix : <http://example.com/> .

            :foo :bar ( "foo1" "foo2"
                "foo3" "foo4" ) .
            """;
        assertThat( result.trim() ).isEqualTo( expected.trim() );
    }

    @Test
    public void testUtf8BomCharset() {
        final Model model = prefixModel();
        final FormattingStyle style = FormattingStyle.builder()
            .charset( FormattingStyle.Charset.UTF_8_BOM )
            .build();
        final TurtleFormatter formatter = new TurtleFormatter( style );
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        formatter.accept( model, outputStream );
        assertThat( outputStream.toByteArray() ).startsWith( (byte) 0xEF, (byte) 0xBB, (byte) 0xBF );
    }

    @Test
    public void testFormatting() {
        final Model model = ModelFactory.createDefaultModel();
        model
            .read( "https://raw.githubusercontent.com/atextor/turtle-formatting/main/turtle-formatting.ttl", "TURTLE" );
        final FormattingStyle style = FormattingStyle.builder().build();

        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        final Model resultModel = modelFromString( result );
        assertThat( model.isIsomorphicWith( resultModel ) ).isTrue();

    }

    @Test
    public void testSubjectsNotInSubjectOrder() {
        final String modelString = """
            @prefix : <http://example.com/> .
            @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

            :Person a rdfs:Class .
            :name a rdf:Property .
            :address a rdf:Property .
            :city a rdf:Property .
            :Max a :Person ;
                :name "Max" ;
                :address [
                    :city "City Z"
                ] .
            """;

        final Model model = modelFromString( modelString );
        final FormattingStyle style = FormattingStyle.builder().build();

        final TurtleFormatter formatter = new TurtleFormatter( style );
        final String result = formatter.apply( model );
        final Model resultModel = modelFromString( result );
        assertThat( model.isIsomorphicWith( resultModel ) ).isTrue();
    }

    private Model modelFromString( final String content ) {
        final Model model = ModelFactory.createDefaultModel();
        final InputStream stream = new ByteArrayInputStream( content.getBytes( StandardCharsets.UTF_8 ) );
        model.read( stream, "", "TURTLE" );
        return model;
    }

    private Model prefixModel() {
        final Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix( "", "http://example.com/" );
        model.setNsPrefix( "a", "http://example.com/a" );
        model.setNsPrefix( "abc", "http://example.com/abc" );
        model.setNsPrefix( "abcdef", "http://example.com/abc" );
        return model;
    }
}
