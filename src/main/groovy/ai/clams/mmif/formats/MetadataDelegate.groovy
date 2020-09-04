package ai.clams.mmif.formats

/**
 * @author Keith Suderman
 */
class MetadataDelegate {
    Map properties
    List<ElementDelegate> elements
    String annotationType
    public MetadataDelegate(Map properties, List<ElementDelegate> elements, String annotationType) {
        this.properties = properties
        this.elements = elements
        this.annotationType = annotationType
    }

    def propertyMissing(String name, value) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def propertyMissing(String name) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def methodMissing(String name, args) {
        if (args.size() != 1) {
            throw MissingMethodException("Invalid arguments for property $name")
        }
        if (!(args[0] instanceof Closure)) {
            throw UnsupportedOperationException("Missing closure for properties definition")
        }

        Closure cl = (Closure) args[0]
        cl.delegate = new PropertyDelegate(elements, annotationType, name)
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
        properties[name] = cl.delegate
    }
}
