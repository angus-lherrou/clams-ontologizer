package org.anc.lapps.vocab.dsl

import groovy.cli.commons.CliBuilder
// import org.apache.jena.ext.xerces.impl.Version
import org.apache.jena.ontology.*
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import org.apache.jena.vocabulary.XSD
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
/**
 * @author Keith Suderman
 */
class VocabDsl {
    static final String VOCAB = "NO"
    static final String VOCAB_PREFIX = "https://mmif.clams.ai"
    static final String VOCAB_DIR = "vocabulary"
    static final String EXTENSION = ".vocab"

    Map TYPE_MAP

    // The template used to generate the HTML page for a single Vocabulary element.
    String FILE_TEMPLATE = "src/test/resources/template.groovy"

    // The template used to generate the main vocabulary page.
    String INDEX_TEMPLATE = "src/test/resources/index.groovy"

    // Selects the templating engine to use.  Choices are the MarkupBuilderTemplateEngine
    // or HtmlTemplateEngine. The latter uses a template that looks like HTML while the
    // former uses the MarkupBuilder DSL as the template language.
//    static boolean USE_MARKUPBUILDER = true

    Set<String> included = new HashSet<String>()
    File parentDir
    File destination
    Binding bindings = new Binding()
    List<ElementDelegate> elements //= []
    Map<String, ElementDelegate> elementIndex //= [:]
    Node schema

    // Ontology used when generating RDF/OWL.  Instances are created and
    // destroyed as needed.
    OntModel ontology

    String version = "0.1.0"
    boolean release = false

    // List the script before running it.
    boolean printListing = false

    void run(File file, File destination) {
        parentDir = file.parentFile
        run(file.text, destination)
    }

    ClassLoader getLoader() {
        ClassLoader loader = Thread.currentThread().contextClassLoader;
        if (loader == null) {
            loader = this.class.classLoader
        }
        return loader
    }

    CompilerConfiguration getCompilerConfiguration() {
        ImportCustomizer customizer = new ImportCustomizer()
        /*
         * Custom imports can be defined in the ImportCustomizer.
         * For example:
         *   customizer.addImport("org.anc.xml.Parser")
         *   customizer.addStarImports("org.anc.util")
         *
         * The jar files for any packages imported this way must be
         * declared as Maven dependencies so they will be available
         * at runtime.
         */

        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.addCompilationCustomizers(customizer)
        return configuration
    }

    void initTypeMap() {
        String DATATYPE
        if (release) {
            DATATYPE = "$VOCAB_PREFIX/$VOCAB_DIR/Datatype"
        }
        else {
            DATATYPE = "$VOCAB_PREFIX/$version/$VOCAB_DIR/Datatype"
        }
        final Resource IDREFS = ResourceFactory.createResource("http://www.w3.org/2001/XMLSchema#IDREFS")
        //TODO: add "List of pairs" type
        TYPE_MAP = [
            ID: XSD.ID,
            "URI": XSD.anyURI,
            "Integer": XSD.xlong,
            "List of IDs": IDREFS,
            "List of URI": ResourceFactory.createResource("$DATATYPE#list_uri"),
            "List of Strings": ResourceFactory.createResource("$DATATYPE#string_list"),
            "Set of IDs": IDREFS,
            "String": XSD.xstring,
            "String or URI": XSD.xstring
        ]
    }

    void run(String scriptString, File destination) {
        this.destination = destination
        try {
            compile(scriptString)
            // Now generate the HTML.
            makeHtml()
            makeIndexHtml()
        }
        catch (Exception e) {
            println()
            println "Script execution threw an exception:"
            e.printStackTrace()
            println()
        }
    }

    void compile(String scriptString) {
        initTypeMap()
        if (printListing) {
            int n = 0
            scriptString.eachLine { String line ->
                printf("%03d: %s\n", ++n, line)
            }
        }
        elements = []
        elementIndex = [:]
        ClassLoader loader = getLoader()
        CompilerConfiguration configuration = getCompilerConfiguration()
        bindings.setVariable('version', version)
        GroovyShell shell = new GroovyShell(loader, bindings, configuration)

        Script script = shell.parse(scriptString)
        script.binding.setVariable("args", [:])
        script.metaClass = getMetaClass(script.class, shell)
        script.run()
        println "Compiled vocabulary version ${bindings.version}"
    }

    void dump(File file) {
        compile(file.text)
        elements.each { it.print() }
    }

    Resource type(String type) {
        Resource resource = TYPE_MAP[type]
        if (resource) {
            return resource
        }

        if (type.startsWith("Datatype#")) {
            if (release) {
                resource = ontology.createResource("$VOCAB_PREFIX/$VOCAB_DIR/${type}")
            }
            else {
                resource = ontology.createResource("$VOCAB_PREFIX/$version/$VOCAB_DIR/${type}")
            }
        }
        else {
            resource = ontology.createResource(type)
        }
//        println "Created resource for $type : ${resource.toString()}"
        TYPE_MAP[type] = resource
        return resource
    }

    void makeHtml() {
        // Create the template engine that will generate the HTML.
        TemplateEngine engine = new TemplateEngine(new File(FILE_TEMPLATE))
        String version = bindings.version ?: '99.0.0'
        elements.each { element ->
            if (!element.name.contains("#")) {
                // Walk up the hierarchy and record the names of
                // all ancestors.
                List parents = []
                String parent = element.parent
                while (parent) {
                    ElementDelegate delegate = elementIndex[parent]
                    parents.add(0, delegate)
                    parent = delegate.parent
                }
                // params is the data model to be passed to the template
                def params = [
                        element:element,
                        elements:elementIndex,
                        parents:parents,
                        version:bindings.getVariable('version'),
                        release:release
                ]
                // file is where the generated HTML will be written.
                File file = new File(destination, "${element.name}.html")
                // Call the template to generate the HTML from the model and
                // write it to the file.
                file.text = engine.generate(params)
                println "Wrote ${file.path} v${bindings.version}"
            }
        }
    }

    DatatypeProperty makeProperty(String annotation, String name) {
        return ontology.createDatatypeProperty("$VOCAB_PREFIX/$version/$VOCAB_DIR/${annotation}#${name}")
    }

    AnnotationProperty makeMetadata(String name) {
        return ontology.createAnnotationProperty("$VOCAB_PREFIX/$version/$VOCAB_DIR/metadata#${name}")
    }

    void makeOwl(File script, File destination, String ext) {
        makeOwl(script, RDFFormat.JSONLD_PRETTY, destination, ext)
    }

    void makeOwl(File script, RDFFormat format, File destination, String ext) {
        //initTypeMap()
        compile(script.text)

        Map<String,OntClass> classes = [:]
        Map<String,Resource> resources = [:]
        Map<String,Property> properties = [:]

//        def resource = { String name ->
//
//            Resource resource = resources[name]
//            if (resource == null) {
//                resource = ontology.createResource(type(name))
//                resources[name] = resource
//            }
//            return resource
//        }

        // Issue #10. Specify a model that does not do inferencing so only
        // relationships expressed in model are included in the output.
        ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
        OntResource ontResource = ontology.createOntology("$VOCAB_PREFIX/$version/$VOCAB_DIR")
        ontResource.addVersionInfo(ontology.createLiteral(bindings.version).toString())
        elements.each { ElementDelegate element ->
            println "Processing ${element.name}"
            OntClass theClass = classes[element.name]
            if (theClass) {
                throw new VocabularyException("Duplicate element : ${element.name}")
            }
            theClass = ontology.createClass("$VOCAB_PREFIX/$version/$VOCAB_DIR" + '/' + element.name)
            classes[element.name] = theClass
            if (element.definition) {
                theClass.addComment(ontology.createLiteral(element.definition))
            }
//            else {
//                throw new VocabularyException("Element ${element.name} is missing a definition field.")
//            }

            // Set the parent node.
            // TODO: It should likely be an error if the element does not have a
            // parent. This means 'Thing' would have to have something like
            // OWL.TOP as its parent.
            if (element.parent) {
                OntClass parent = classes[element.parent]
                if (!parent) {
                    throw new VocabularyException("Undefined parent class: ${element.parent}")
                }
                theClass.setSuperClass(parent)
            }
            element.sameAs.each { name ->
                theClass.addSameAs(type(name))
            }
            element.metadata.each { String key, PropertyDelegate value ->
                Property property = makeMetadata(key)
                property.addComment(ontology.createLiteral(value.description))
                property.addDomain(theClass)
                property.addRange(type(value.type))
//                theClass.addProperty(property, value.type)
            }
            element.properties.each { String name, PropertyDelegate value ->
                Property property = makeProperty(element.name, name)
                if (value.description) {
                    property.addComment(ontology.createLiteral(value.description))
                }
                property.addDomain(theClass)
                property.addRange(type(value.type))
//                theClass.addProperty(property, value.type)
            }

        }

        File file = new File(destination, "lapps-vocabulary.${ext}")
        OutputStream os = new FileOutputStream(file)
        RDFDataMgr.write(os, ontology, format)
        os.flush()
        os.close()
        println "Wrote ${file.path}"
    }

    MetaClass getMetaClass(Class<?> theClass, GroovyShell shell) {
        ExpandoMetaClass meta = new ExpandoMetaClass(theClass, false)
        meta.include = { String filename ->
            // Make sure we can find the file. The default behaviour is to
            // look in the same directory as the source script.
            // TODO Allow an absolute path to be specified.

            def filemaker
            if (parentDir != null) {
                filemaker = { String name ->
                    return new File(parentDir, name)
                }
            }
            else {
                filemaker = { String name ->
                    new File(name)
                }
            }

            File file = filemaker(filename)
            if (!file.exists() || file.isDirectory()) {
                file = filemaker(filename + EXTENSION)
                if (!file.exists()) {
                    throw new FileNotFoundException(filename)
                }
            }
            // Don't include the same file multiple times.
            if (included.contains(filename)) {
                return
            }
            included.add(filename)


            // Parse and run the script.
            Script included = shell.parse(file)
            included.metaClass = getMetaClass(included.class, shell)
            included.run()
        }
        meta.Datatypes = { Closure cl ->
            cl.delegate = new DatatypeDelegate(TYPE_MAP)
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            schema = cl.delegate.root
        }
        meta.Datatype = meta.Datatypes

//        meta.element = { Closure cl ->
//            ElementDelegate element = new ElementDelegate(elements)
//            cl.delegate = element
//            cl.resolveStrategy = Closure.DELEGATE_FIRST
//            cl()
//            elements << element
//            elementIndex[element.name] = element
//        }

        meta.methodMissing = { String name,  args ->
            if (args.size() != 1 || !(args[0] instanceof Closure) ) {
                throw new MissingMethodException(name, java.lang.Object.class, args)
            }
            Closure cl = (Closure) args[0]
            ElementDelegate element = new ElementDelegate(elements)
            element.name = name
            cl.delegate = element
            cl()
            elements << element
            elementIndex[name] = element
        }

        meta.initialize()
        return meta
    }

    void makeDiscriminators(File script, File destination) {
        List<String> order = ['annotation',
                              'chunk',
                              'paragraph',
                              'sentence',
                              'token',
                              'pos',
                              'coref',
                              'ne',
                              'person',
                              'location',
                              'date',
                              'organization',
                              'nchunk',
                              'vchunk',
                              'lemma',
                              'lookup',
                              'matches',
                              'markable',
                              'dependency-structure',
                              'phrase-structure',
                              'constituent',
                              'dependency']

        compile(script.text)

        elements.each {
            if (!it.discriminator) {
                it.discriminator = getDiscriminator(it)
            }
        }

        File outputFile = new File(destination, "vocabulary.config")
        outputFile.withWriter { writer ->
            boolean close = false
            if (bindings.version == null || bindings.version == '1.0.0') {
                writer.println("bank(2) {")
                close = true
            }
//            else {
//                writer.println "version='${bindings.version}'"
//            }

            order.each { String name ->
                ElementDelegate element = elements.find { it.discriminator == name }
                if (element) {
                    writeDiscriminator(writer, element)
                }
                else {
                    println "Missing element ${name}"
                }
            }
            elements.each { ElementDelegate element ->
//                String discriminator = getDiscriminator(element)
                if (!order.contains(element.discriminator)) {
                    writeDiscriminator(writer, element)
                }
//                else {
//                    println "Order list contains ${element.name}"
//                }
            }
            if (close) {
                writer.println("}")
            }
        }
        println "Wrote ${outputFile.path}"
    }

    String getDiscriminator(ElementDelegate element) {
        if (element.discriminator) {
            return element.discriminator
        }
        return element.name.replaceAll("([a-z])([A-Z])", '$1-$2').toLowerCase()
    }

    void writeSchema(File scriptFile, File destination) {
        File file = new File(destination, "Datatype.xsd")
//        initTypeMap()
        compile(scriptFile.text)
        StringWriter string = new StringWriter()
        PrintWriter writer = new PrintWriter(string)
        writer.write('<?xml version="1.0" encoding="UTF-8"?>\n')
        XmlNodePrinter printer = new XmlNodePrinter(writer)
        printer.setPreserveWhitespace(true)
        printer.print(schema)
//        new XmlNodePrinter(preserveWhitespace: true).print(schema)
//        String xml = XmlUtil.serialize(schema)
//        println XmlUtil.serialize(schema)
        file.text = string.toString()
        println "Wrote ${file.path}"
    }

    void writeDiscriminator(BufferedWriter writer, ElementDelegate element) {
//        if (!element.discriminator) {
//            println "Skipping ${element.name}"
//            return
//        }
        String discriminator = getDiscriminator(element)
        println "Generating discriminator info for ${element.name}"
        if (discriminator.contains('-')) {
            writer.println "\"${discriminator}\" {"
        }
        else {
            writer.println("${discriminator} {")
        }
//        if (element.parent) {
//            writer.println("\tparents ${element.parent}")
//        }
        //writer.println("\turi 'http://vocab.lappsgrid.org/${element.name}'")
        writer.println("\turi vocab('${element.name}')")
        writer.println("\tdescription \"${normalize(element.definition)}\"")
        if (element.deprecated) {
            writer.println("\tdeprecated \"${normalize(element.deprecated)}\"")
        }
        writer.println("}")
    }

    String normalize(String s) {
        return s.replaceAll(~/[\n\t]/, ' ').replaceAll(~/\s\s+/, ' ')
    }

    void makeIndexHtml() {
        File file = new File(INDEX_TEMPLATE)
        if (!file.exists()) {
            throw new FileNotFoundException("Unable to find the index template.")
        }
        TemplateEngine template = new TemplateEngine(file)
        String html = template.generate(roots: getTrees(), version:bindings.version, release:release)
        File destination = new File(destination, 'index.html')
        destination.text = html
        println "Wrote ${destination.path}"
    }

    List<TreeNode> getTrees() {
        List<TreeNode> roots = []
        elements.each { ElementDelegate element ->
            TreeNode elementNode = TreeNode.get(element)
            if (element.parent) {
                TreeNode parent = TreeNode.get(element.parent)
                parent.children << elementNode
            }
            else {
                roots << elementNode
            }
        }
        return roots
    }

    void makeJava(File scriptFile, String packageName, String className, File destination) {
        println "Generating ${className}.java"
        compile(scriptFile.text)

        File outFile = new File(destination, "${className}.java")
        outFile.withPrintWriter { PrintWriter out ->
            out.println """
/*
 * DO NOT EDIT THIS FILE.
 *
 * This file is machine generated and any edits will be lost the next
 * time the file is generated. Use the https://github.com/lapps/vocabulary-pages
 * project to make changes.
 *
 * Vocabulary v${version}
 * Copyright 2019 The Language Applications Grid. 
 */
package ${packageName};

public class ${className} {
    private ${className}() { }
"""
            def toSnakeCase = { String name ->
                name.replaceAll("([^_A-Z])([A-Z])", '$1_$2').toUpperCase()
            }
            elements.findAll{ !it.name.contains('#')}.sort { a,b -> a.name.compareTo(b.name) }.each { ElementDelegate e ->
                if (e.deprecated) {
                    String message = e.deprecated
                            .replaceAll(~/<\/?link>/, '')
                            .tokenize('.')[0]
                            .replaceAll('\n', ' ')
                            .replaceAll('\t', ' ')
                            .replaceAll(~/  +/, ' ')
                    out.println "\t/**"
                    out.println "\t * @deprecated $message"
                    out.println "\t */"
                    out.println "\t@Deprecated"
                }
                out.println "\t/**"
                out.println "\t * ${e.definition}"
                out.println "\t */"
                out.println "\tpublic static final String ${toSnakeCase(e.name) } = \"http://vocab.lappsgrid.org/${e.name}\";"
            }
            out.println "}"
        }
        println "Wrote ${outFile.path}"
    }

    void makeFeaturesJava(File scriptFile, String packageName, File destination)
    {
        println "Generating Features.java"
        compile(scriptFile.text)
        File outFile = new File(destination, "Features.java")
        outFile.withPrintWriter { PrintWriter out ->
            out.println """/*
 * DO NOT EDIT THIS FILE.
 *
 * This file is machine generated and any edits will be lost the next
 * time the file is generated. Use the https://github.com/lapps/vocabulary-pages
 * project to make changes.
 *
 * Vocabulary v${version}
 * Copyright 2019 The Language Applications Grid. 
*/

package ${packageName};

public class Features {
\tprivate Features() { }
"""
            elements.each { ElementDelegate e ->
                if (!e.name.contains('#')) {
                    String superClass = ""
                    if (e.parent) {
                        superClass = " extends ${e.parent}"
                    }
                    out.println "\tpublic static class ${e.name}${superClass} {"
                    e.properties.each { String name, ignored ->
                        String snakeCase = toSnakeCase(name)
                        if (e.definition) {
                            out.println("\t\t/**")
                            out.println("\t\t * ${e.definition}")
                            out.println("\t\t */")
                        }
                        if (name == "pos") {
                            // Hack-around since the POS tag name changed.
                            out.println "\t\tpublic static final String PART_OF_SPEECH = \"pos\";"
                        }
                        else {
                            out.println "\t\tpublic static final String ${snakeCase} = \"${name}\";"
                        }
                    }
                    out.println "\t}"
                    out.println()

                }
            }
            out.println("}")
        }
        println "Wrote ${outFile.path}"
    }

    void makeMetadataJava(File scriptFile, String packageName, File destination)
    {
        println "Generating Metadata.java"
        compile(scriptFile.text)
        File outFile = new File(destination, "Metadata.java")
        outFile.withPrintWriter { PrintWriter out ->
            out.println """/*
 * DO NOT EDIT THIS FILE.
 *
 * This file is machine generated and any edits will be lost the next
 * time the file is generated. Use the https://github.com/lapps/vocabulary-pages
 * project to make changes.
 *
 * Vocabulary v${version}
 * Copyright 2019 The Language Applications Grid. 
 */

package ${packageName};

/**
 * 
 */
public class Metadata {
\tprivate Metadata() { }
"""

            elements.findAll{ !it.name.contains('#') }.each { ElementDelegate e ->
                String superClass = ""
                if (e.parent) {
                    superClass = " extends ${e.parent}"
                }
                out.println "\tpublic static class ${e.name}${superClass} {"
                e.metadata.each { String name, ignored ->
                    String snakeCase = toSnakeCase(name)
                    if (e.definition) {
                        out.println("\t\t/**")
                        out.println("\t\t * ${e.definition}")
                        out.println("\t\t */")
                    }
                    out.println "\t\tpublic static final String ${snakeCase} = \"${name}\";"
                }
                out.println "\t}"
                out.println()
            }
            out.println("}")
        }
        println "Wrote ${outFile.path}"
    }

    protected String toSnakeCase(String name) {
        if (name.endsWith("Type")) {
            return "TYPE"
        }
        return name.replaceAll(/\B[A-Z]/){'_'+it}.toUpperCase()
    }

    static void main(args) {
        CliBuilder cli = new CliBuilder()
        cli.usage = "vocab [-?|-v] -d <dsl> -i <template> -h <template> -o <directory>"
        cli.header = "Generates LAPPS Vocabulary web site a LAPPS Vocab DSL file."
        cli.v(longOpt:'version', 'displays current application version number.')
        cli.h(longOpt:'html', args:1,'template used to generate html pages for vocabulary items.')
        cli.r(longOpt:'rdf', args:1, 'generates RDF/OWL ontology in the specifed format')
//        cli.R(longOpt:'release', 'generate a release version otherwise generates a SNAPSHOT version.')
        cli.i(longOpt:'index', args:1, 'template used to generate the index.html page.')
        cli.j(longOpt:'java', args:1, 'generates a Java class containing URI defintions.')
        cli.d(longOpt: 'discriminators', 'generated Discriminator DSL fragment.')
        cli.f(longOpt:'features', 'generates the Features.java with element property names.')
        cli.p(longOpt:'package', args:1, 'package name for the Java class.')
        cli.o(longOpt:'output', args:1, 'output directory.')
        cli.l(longOpt:'list', 'List compiled script with line numbers.')
        cli.b(longOpt:'debug', 'prints a data dump rather than generating anything.')
        cli.V(longOpt:'vocabVersion', args: 1, 'version of the vocabulary being generated.')
        cli.x(longOpt:'xsd', 'generates the XML Schema for any datatypes defined.')
        cli.'?'(longOpt:'help', 'displays this usage messages.')

        def params = cli.parse(args)
        if (!params) {
            return
        }

        if (params.'?') {
            cli.usage()
            return
        }
        if (params.v) {
            String versionString = "LAPPS Vocabulary DSL v" + Version.getVersion()
            InputStream stream = VocabDsl.getResourceAsStream('/build')
            if (stream) {
                versionString += ' Build ' + stream.text.trim()
            }
            println()
            println  versionString
            println "Copyright 2019 American National Corpus"
            println()
            return
        }

        File destination
        if (params.o) {
            destination = new File(params.o)
        }
        else {
            destination = new File('target')
        }

        if (!destination.exists()) {
            if (!destination.mkdirs()) {
                println "Unable to create output directory ${destination.path}"
                return
            }
        }

        List<String> files = params.arguments()
        if (files.size() == 0) {
            println "No vocabulary specified."
            return
        }
        VocabDsl dsl = new VocabDsl()
        if (params.V) {
            dsl.version = params.V
        }
        if (params.l) {
            dsl.printListing = true
        }

        if (dsl.version && (dsl.version.contains('-SNAPHOST') || dsl.version.contains('-RC'))) {
            dsl.release = false
        }
        else {
            dsl.release = true
        }

        File scriptFile = new File(files[0])
        if (params.j) {
            String packageName = "org.lappsgrid.discrimintor"
            if (params.p) {
                packageName = params.p
            }
            dsl.makeJava(scriptFile, packageName, params.j, destination)
            return
        }
        if (params.f) {
            String packageName = "org.lappsgrid.vocabulary"
            if (params.p) {
                    packageName = params.p
            }
            dsl.makeFeaturesJava(scriptFile, packageName, destination)
            dsl.makeMetadataJava(scriptFile, packageName, destination)
            return
        }
        if (params.x) {
            dsl.writeSchema(scriptFile, destination)
            return
        }
        if (params.d) {
            dsl.makeDiscriminators(scriptFile, destination)
            return
        }
        if (params.b) {
            dsl.dump(scriptFile)
            return
        }
        if (params.r) {
            String ext = params.r
            RDFFormat format = RDFFormat.RDFXML_PRETTY
            switch (params.r) {
                case 'owl':
                    format = RDFFormat.RDFXML_PRETTY
                    break
                case 'rdf':
                    format = RDFFormat.RDFXML_PLAIN
                    break
                case 'nq':
                    format = RDFFormat.NQ
                    break
                case 'n3':
                    format = RDFFormat.NTRIPLES_UTF8
                    break
                case 'jsonld':
                    format = RDFFormat.NQ.JSONLD_PRETTY
                    break
                case 'ttl':
                    format = RDFFormat.TTL
                    break
                default:
                    println "ERROR: Unknown output format ${params.r}"
                    println "Use one of 'owl', 'rdf', 'nq', 'n3', 'ttl', or 'jsonld'."
                    return
            }
            dsl.makeOwl(scriptFile, format, destination, ext)
            return
        }
        dsl.INDEX_TEMPLATE = params.i
        dsl.FILE_TEMPLATE = params.h
        dsl.run(scriptFile, destination)
    }
}
