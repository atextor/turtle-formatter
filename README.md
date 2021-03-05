# turtle-formatter

[![build](https://github.com/atextor/turtle-formatter/actions/workflows/build.yml/badge.svg)](https://github.com/atextor/turtle-formatter/actions/workflows/build.yml) [![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)

**turtle-formatter** is a Java library for pretty printing
[RDF/Turtle](https://www.w3.org/TR/turtle/) documents in a _configurable_ and _reproducible_ way.

It takes as input a formatting style and an [Apache Jena](https://jena.apache.org) Model and
produces as output a pretty-printed RDF/Turtle document.

**Current Status**: The library is feature-complete.

## Why?

Every RDF library comes with its own serializers, for example an Apache Jena Model can be written
[in multiple ways](https://jena.apache.org/documentation/io/rdf-output.html), the easiest being
calling the write method on a model itself: `model.write(System.out, "TURTLE")`. However, due to the
nature of RDF, outgoing edges of a node in the graph have no order. When serializing a model, there
are multiple valid ways to do so. For example, the following two models are identical:

<table>
<tr>
<td>

```turtle
@prefix : <http://example.com/> .

:test
  :blorb "blorb" ;
  :floop "floop" .
```

</td>
<td>

```turtle
@prefix : <http://example.com/> .

:test
  :floop "floop" ;
  :blorb "blorb" .
```

</td>
</tr>
</table>

Therefore, when a model is serialized, one of many different (valid) serializations could be the
result. This is a problem when different versions of a model file are compared, for example when
used as artifacts in a git repository. Additionally, serialized files are often formatted in one
style hardcoded in the respective library. So while Apache Jena and for example
[libraptor2](http://librdf.org/raptor/) both write valid RDF/Turtle, the files are formatted
differently. You would not want the code of a project formatted differently in different files,
would you?
**turtle-formatter** addresses these problems by taking care of serialization order and providing a
way to customize the formatting style.

## Usage

### Add dependency

Add the following dependency to your Maven `pom.xml`:
```xml
<dependency>
  <groupId>de.atextor</groupId>
  <artifactId>turtle-formatter</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle/Groovy: `implementation 'de.atextor:turtle-formatter:1.0.0'`

Gradle/Kotlin: `implementation("de.atextor:turtle-formatter:1.0.0")`

### Calling the formatter

```java
import de.atextor.turtle.formatter.FormattingStyle;
import de.atextor.turtle.formatter.TurtleFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

// ...

TurtleFormatter formatter = new TurtleFormatter(FormattingStyle.DEFAULT);
// Build or load a Jena Model
Model model = RDFDataMgr.loadModel("data.ttl");
// Either create a string...
String prettyPrintedModel = formatter.apply(model);
// ...or write directly to an OutputStream
formatter.accept(model, System.out);
```

### Customizing the style

Instead of passing `FormattingStyle.DEFAULT`, you can create a custom `FormattingStyle` object.

```java
FormattingStyle style = FormattingStyle.builder(). ... .build();
```

The following options can be set on the FormattingStyle builder:

<table>
<tr>
<td>Option</td>
<td>Description</td>
<td>Default</td>
</tr>

<tr>
<td>

`alignPrefixes`

</td>
<td>Boolean. Example:

```turtle
# true
@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix example: <http://example.com/> .

# false
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix example: <http://example.com/> .
```

</td>
<td>false</td>
</tr>

<tr>
<td>

`alignPredicates`
`firstPredicateInNewLine`

</td>
<td>
Boolean. Example:

```turtle
# firstPredicateInNewLine false / alignPredicates true
:test a rdf:Resource ;
      :blorb "blorb" ;
      :floop "floop" .

# firstPredicateInNewLine false / alignPredicates false
:test a rdf:Resource ;
  :blorb "blorb" ;
  :floop "floop" .

# firstPredicateInNewLine true / alignPredicates does not matter
:test
  a rdf:Resource ;
  :blorb "blorb" ;
  :floop "floop" .
```

</td>
<td>false (for both)</td>
</tr>

<tr>
<td>

`alignObjects`

</td>
<td>

Boolean. Example:
```turtle
# alignObjects true
:test
  a           rdf:Resource ;
  :blorb      "blorb" ;
  :floopfloop "floopfloop" .

# alignObjects false
:test
  a rdf:Resource ;
  :blorb "blorb" ;
  :floopfloop "floopfloop" .
```

</td>
<td>
false
</td>
</tr>

<tr>
<td>

`charset`\*

</td>
<td>

One of `LATIN1`, `UTF_16_BE`, `UTF_16_LE`, `UTF_8`, `UTF_8_BOM`

</td>
<td>

`UTF_8`

</td>
</tr>
<tr>
<td>

`doubleFormat`

</td>
<td>

A [NumberFormat](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/text/NumberFormat.html) that describes how `xsd:double` literals are formatted

</td>
<td>

`0.####E0`

</td>
</tr>

<tr>
<td>

`endOfLine`\*

</td>
<td>

One of `LF`, `CR`, `CRLF`. If unsure, please see [Newline](https://en.wikipedia.org/wiki/Newline)

</td>
<td>

`LF`

</td>
</tr>
<tr>
<td>

`indentStyle`\*

</td>
<td>

`SPACE` or `TAB`. Note that when choosing `TAB`, `alignPredicates` and `alignObjects` are
automatically treated as `false`.

</td>
<td>

`SPACE`

</td>
</tr>

<tr>
<td>

`indentSize`\*

</td>
<td>

Integer. When using `indentStyle` `SPACE`, defines the indentation size.

</td>
<td>
2
</td>
</tr>

<tr>
<td>

`insertFinalNewLine`\*

</td>
<td>
Boolean. Determines whether there is a line break after the last line
</td>
<td>
true
</td>
</tr>

<tr>
<td>

`useAForRdfType`

</td>
<td>

Boolean. Determines whether `rdf:type` is written as `a` or as `rdf:type`.

</td>
<td>
true
</td>
</tr>

<tr>
<td>

`useCommaByDefault`

</td>
<td>

Boolean. Determines whether to use commas for identical predicates. Example:
```turtle
# useCommaByDefault false
:test a rdf:Resource ;
  :blorb "someBlorb" ;
  :blorb "anotherBlorb" .

# useCommaByDefault true
:test a rdf:Resource ;
  :blorb "someBlorb", "anotherBlorb" .
```

</td>
<td>
false
</td>
</tr>

<tr>
<td>

`commaForPredicate`

</td>
<td>

A set of predicates that, when used multiple times, are separated by commas, even when
`useCommaByDefault` is `false`. Example:

```turtle
# useCommaByDefault false, commaForPredicate contains 'rdf:type',
# firstPredicateInNewLine true
:test a ex:something, owl:NamedIndividual ;
  :blorb "someBlorb" ;
  :blorb "anotherBlorb" .

# useCommaByDefault false, commaForPredicate is empty,
# firstPredicateInNewLine false
:test
  a ex:something ;
  a owl:NamedIndividual ;
  :blorb "someBlorb" ;
  :blorb "anotherBlorb" .
```

</td>
<td>

Set.of(`rdf:type`)

</td>
</tr>

<tr>
<td>

`noCommaForPredicate`

</td>
<td>

Analogous to `commaForPredicate`: A set of predicates that, when used multiple times, are _not_
separated by commas, even when `useCommaByDefault` is `true`.

</td>
<td>
Empty
</td>
</tr>

<tr>
<td>

`prefixOrder`

</td>
<td>

A list of namespace prefixes that defines the order of `@prefix` directives. Namespaces from the
list always appear first (in this order), every other prefix will appear afterwards,
lexicographically sorted. Example:

```turtle
# prefixOrder contains "rdf" and "owl" (in this order), so they
# will appear in this order at the top (when the model contains
# them!), followed by all other namespaces
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix example: <http://example.com/> .
```

</td>
<td>

List.of(`rdf` `rdfs` `xsd` `owl`)

</td>
</tr>

</table>

\* Adapted from [EditorConfig](https://editorconfig.org/#file-format-details)

*The remaining missing documentation will be added soon*

## Contact

**turtle-formatter** is developed by Andreas Textor <<mail@atextor.de>>.

