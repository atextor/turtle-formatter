# turtle-formatter

[![build](https://github.com/atextor/turtle-formatter/actions/workflows/build.yml/badge.svg)](https://github.com/atextor/turtle-formatter/actions/workflows/build.yml) [![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)

**turtle-formatter** is a Java library for pretty printing
[RDF/Turtle](https://www.w3.org/TR/turtle/) documents in a _configurable_ and _reproducible_ way.

It takes as input a formatting style and an [Apache Jena](https://jena.apache.org) Model and
produces as output a pretty-printed RDF/Turtle document.

## Current Status

The library is feature-complete.

## Usage

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

## Contact

**turtle-formatter** is developed by Andreas Textor <<mail@atextor.de>>.

