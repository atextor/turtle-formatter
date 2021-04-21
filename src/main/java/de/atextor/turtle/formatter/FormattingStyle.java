package de.atextor.turtle.formatter;

import lombok.Builder;
import lombok.Value;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.XSD;

import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

@Builder
public class FormattingStyle {
    public static final KnownPrefix PREFIX_FMT = new KnownPrefix( "fmt", URI.create( FMT.NS ) );

    public static final KnownPrefix PREFIX_RDF = new KnownPrefix( "rdf", URI.create( RDF.uri ) );

    public static final KnownPrefix PREFIX_RDFS = new KnownPrefix( "rdfs", URI.create( RDFS.uri ) );

    public static final KnownPrefix PREFIX_XSD = new KnownPrefix( "xsd", URI.create( XSD.NS ) );

    public static final KnownPrefix PREFIX_OWL = new KnownPrefix( "owl", URI.create( OWL2.NS ) );

    public static final KnownPrefix PREFIX_DCTERMS = new KnownPrefix( "dcterms", URI.create( DCTerms.NS ) );

    public static final FormattingStyle DEFAULT = builder().build();

    public static final KnownPrefix PREFIX_VANN = new KnownPrefix( "vann",
        URI.create( "http://purl.org/vocab/vann/" ) );

    public static final KnownPrefix PREFIX_SKOS = new KnownPrefix( "skos", URI.create( SKOS.getURI() ) );

    public static final KnownPrefix PREFIX_EX = new KnownPrefix( "ex", URI.create( "http://example.org/" ) );

    @Builder.Default
    Set<KnownPrefix> knownPrefixes = Set.of(
        PREFIX_RDF,
        PREFIX_RDFS,
        PREFIX_XSD,
        PREFIX_OWL,
        PREFIX_DCTERMS,
        PREFIX_FMT
    );

    @Builder.Default
    GapStyle afterClosingParenthesis = GapStyle.NOTHING;

    @Builder.Default
    GapStyle afterClosingSquareBracket = GapStyle.SPACE;

    @Builder.Default
    GapStyle afterComma = GapStyle.SPACE;

    @Builder.Default
    GapStyle afterDot = GapStyle.NEWLINE;

    @Builder.Default
    GapStyle afterOpeningParenthesis = GapStyle.SPACE;

    @Builder.Default
    GapStyle afterOpeningSquareBracket = GapStyle.NEWLINE;

    @Builder.Default
    GapStyle afterSemicolon = GapStyle.NEWLINE;

    @Builder.Default
    Alignment alignPrefixes = Alignment.OFF;

    @Builder.Default
    GapStyle beforeClosingParenthesis = GapStyle.SPACE;

    @Builder.Default
    GapStyle beforeClosingSquareBracket = GapStyle.NEWLINE;

    @Builder.Default
    GapStyle beforeComma = GapStyle.NOTHING;

    @Builder.Default
    GapStyle beforeDot = GapStyle.SPACE;

    @Builder.Default
    GapStyle beforeOpeningParenthesis = GapStyle.SPACE;

    @Builder.Default
    GapStyle beforeOpeningSquareBracket = GapStyle.SPACE;

    @Builder.Default
    GapStyle beforeSemicolon = GapStyle.SPACE;

    @Builder.Default
    Charset charset = Charset.UTF_8;

    @Builder.Default
    NumberFormat doubleFormat = new DecimalFormat( "0.####E0" );

    @Builder.Default
    EndOfLineStyle endOfLine = EndOfLineStyle.LF;

    @Builder.Default
    IndentStyle indentStyle = IndentStyle.SPACE;

    @Builder.Default
    WrappingStyle wrapListItems = WrappingStyle.FOR_LONG_LINES;

    @Builder.Default
    boolean firstPredicateInNewLine = false;

    @Builder.Default
    boolean useAForRdfType = true;

    @Builder.Default
    boolean useCommaByDefault = false;

    @Builder.Default
    Set<Property> commaForPredicate = Set.of( RDF.type );

    @Builder.Default
    Set<Property> noCommaForPredicate = Set.of();

    @Builder.Default
    boolean useShortLiterals = true;

    @Builder.Default
    boolean alignBaseIRI = false;

    @Builder.Default
    boolean alignObjects = false;

    @Builder.Default
    boolean alignPredicates = false;

    @Builder.Default
    int continuationIndentSize = 4;

    @Builder.Default
    boolean indentPrediates = true;

    @Builder.Default
    boolean insertFinalNewline = true;

    @Builder.Default
    int indentSize = 2;

    @Builder.Default
    int maxLineLength = 100;

    @Builder.Default
    boolean trimTrailingWhitespace = true;

    @Builder.Default
    List<String> prefixOrder = List.of(
        "rdf",
        "rdfs",
        "xsd",
        "owl"
    );

    @Builder.Default
    List<Resource> subjectOrder = List.of(
        RDFS.Class,
        OWL2.Ontology,
        OWL2.Class,
        RDF.Property,
        OWL2.ObjectProperty,
        OWL2.DatatypeProperty,
        OWL2.AnnotationProperty,
        OWL2.NamedIndividual,
        OWL2.AllDifferent,
        OWL2.Axiom
    );

    @Builder.Default
    List<Property> predicateOrder = List.of(
        RDF.type
    );

    @Builder.Default
    List<RDFNode> objectOrder = List.of(
        OWL2.NamedIndividual,
        OWL2.ObjectProperty,
        OWL2.DatatypeProperty,
        OWL2.AnnotationProperty,
        OWL2.FunctionalProperty,
        OWL2.InverseFunctionalProperty,
        OWL2.TransitiveProperty,
        OWL2.SymmetricProperty,
        OWL2.AsymmetricProperty,
        OWL2.ReflexiveProperty,
        OWL2.IrreflexiveProperty
    );

    @Builder.Default
    BiFunction<Resource, Integer, String> anonymousNodeIdGenerator = ( resource, integer ) -> "_:gen" + integer;

    public enum Alignment {
        LEFT,
        OFF,
        RIGHT
    }

    public enum Charset {
        LATIN1,
        UTF_16_BE,
        UTF_16_LE,
        UTF_8,
        UTF_8_BOM
    }

    public enum EndOfLineStyle {
        CR,
        CRLF,
        LF
    }

    public enum GapStyle {
        NEWLINE,
        NOTHING,
        SPACE
    }

    public enum IndentStyle {
        SPACE,
        TAB
    }

    public enum WrappingStyle {
        ALWAYS,
        FOR_LONG_LINES,
        NEVER
    }

    @Value
    public static class KnownPrefix {
        String prefix;

        URI iri;
    }
}
