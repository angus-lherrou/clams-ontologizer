# LAPPS Vocabulary DSL

**Build status**

[![master status](http://grid.anc.org:9080/travis/svg/lappsgrid-incubator/vocabulary-dsl?branch=master)](https://travis-ci.org/lappsgrid-incubator/vocabulary-dsl)
[![develop status](http://grid.anc.org:9080/travis/svg/lappsgrid-incubator/vocabulary-dsl?branch=develop)](https://travis-ci.org/lappsgrid-incubator/vocabulary-dsl)

Templating system to generate LAPPS vocabulary web pages.

The LAPPS Vocabulary is expressed as a Groovy DSL that is then processed to generate the http://vocab.lappsgrid.org site as well as Java classes and input to the [Discriminator DSL](https://github.com/lappsgrid-incubator/org.lappsgrid.discriminator.dsl).

This repository creates the executables (shell startup script and jar file) needed to process the LAPPS Vocabulary file. To create the executables do

```
$ make release
```

This creates the shell script `target/vocab/vocab` and the jar file `target/vocab/vocab-1.3.0-SNAPSHOT.jar`, where `VERSION` is determined by the content of the `VERSION` file. It also creates the archive `target/vocab-VERSION.zip` which contains the shell script and the jar. Those two executables should be added to the `bin` directory of https://github.com/lapps/vocabulary-pages, where they are used to create HTML pages and Java files.


## Using the jar file

```
java -jar vocab.jar [options] vocabulary-file
```
where `vocabulary-file` is the path to the dsl file to be processed.

### Options

* **-h --html** &lt;path&gt;<br/>
specifies the template used to generate the HTML page for each term in the vocabulary
* **-i --index** &lt;path&gt;<br/>
specifies the template used to generate the vocabulary index.html page
* **-d --discriminators**<br/>
generate the discriminator dsl fragement
* **-r --rdf** &lt;string&gt;<br/>
generates one of the RDF formats owl, rdf, ttl, jsonld, or n3
* **-f --features**<br/>
generates the Features.java source file
* **-j --java** &lt;string&gt;<br/>
generates the Java source file with annotation definitons. The parameter is the name of the class/file that will be generated.
* **-p --package** &lt;string&gt;<br/>
specify the package name to use for the Java source files.
* **-o --output** &lt;path&gt;<br/>
output directory

### Examples

These examples are for the startup script.

```
vocab -h pages.template -i index.template -o /tmp lapps.vocab
vocab -j Annotations -p org.lappsgrid.vocabulary -o /tmp lapps.vocab
```

## Makefile

While the build and dependency management is done with Maven, a Makefile is also included to simplify common tasks related to creating an ddeploying the jar file.

### Common Goals

* **jar**<br/>
Creates an executable jar file. Runs `mvn package`.
* **clean**<br/>
Removes artifacts from previous builds. Runs `mvn clean`.
* **release**<br/>
Creates two zip files with the main jar file and a startup script. The two zips are identical, one is named `vocab-latest.zip` and one `vocab-VERSION.zip` where `VERSION` is taken from the `VERSION` file, both zip files are in the `target` directory.
* **install**<br/>
Copies the versioned jar and startup script to `$HOME/bin` as well as to a machine specific directory if it exists.
* **upload**<br/>
Uploads the zip file to the downloads area on the ANC web server. This goal will need to be modified before it can be used by other users and also assumes the user has a valid SSH key installed on the ANC server.
