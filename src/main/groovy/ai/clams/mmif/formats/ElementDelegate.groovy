package ai.clams.mmif.formats

import groovy.transform.MapConstructor

/**
 * @author Keith Suderman
 */
@MapConstructor
class ElementDelegate {
    String name
    String definition
    String discriminator
    String parent

    // If this String is non-null then the element has been deprecated.  The
    // value of the string is the deprecation message to be displayed if
    // needed.
    String deprecated

    List<String> sameAs = []
    List<String> similarTo = []
    String uri
    Map<String, PropertyDelegate> properties = [:]
    Map<String, PropertyDelegate> metadata = [:]

    List<ElementDelegate> elements

    ElementDelegate(List<ElementDelegate> elements) {
        this.elements = elements
    }

    def propertyMissing(String name, value) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def propertyMissing(String name) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def name(String name) {
        this.name = name
    }

    void parent(String parent) {
        this.parent = parent
    }

    void definition(String definition) {
        this.definition = definition
    }

//    void discriminator(String discriminator) {
//        this.discriminator = discriminator
//    }

    void deprecated(String message) {
        this.deprecated = message
    }
    void sameAs(String... args) {
        args.each { sameAs << it }
    }

    void similarTo(String... args) {
        args.each { similarTo << it }
    }

    void uri(String uri) {
        this.uri = uri
    }

    void metadata(Closure cl) {
        cl.delegate = new MetadataDelegate(metadata, elements, name)
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    void properties(Closure cl) {
        cl.delegate = new PropertiesDelegate(properties, elements, name)
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    void print() {
        print(System.out)
    }

    public String getUri() {
        if (uri) {
            return uri
        }
        return "http://vocab.lappsgrid.org/${name}"
    }

    void print(def writer) {
        writer.println "Element   : $name"
        if (parent) {
            writer.println "Parent    : $parent"
        }
        writer.println "Definition: $definition"
        writer.println "Same as   : ${sameAs.join(", ")}"
        writer.println "URI       : $uri"
        writer.println "Properties"
        properties.each { name, PropertyDelegate value ->
            writer.println "\t$name {"
            writer.println "\t\ttype: ${value.type}"
            writer.println "\t\tdescription: ${value.description}"
            writer.println "\t\trequired: ${value.required}"
            writer.println "\t}"
        }
        writer.println()
    }
}
