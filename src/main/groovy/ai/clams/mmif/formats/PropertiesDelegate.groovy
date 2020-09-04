package ai.clams.mmif.formats

/**
 * @author Keith Suderman
 */
class PropertiesDelegate {
    //Map<String,PropertyDelegate> properties = [:]
    Map properties
    String type
    List<ElementDelegate> elements
    String annotationType

    public PropertiesDelegate(Map properties, List<ElementDelegate> elements, String type) {
        this.properties = properties
        this.elements = elements
        this.annotationType = type
    }

    def propertyMissing(String name, value) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def propertyMissing(String name) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def methodMissing(String name, args) {
        if (args.size() != 1) {
            throw new MissingMethodException("Invalid arguments for property $name")
        }
        if (!(args[0] instanceof Closure)) {
            throw new UnsupportedOperationException("Missing closure for properties definition")
        }

        Closure cl = (Closure) args[0]
        PropertyDelegate prop = new PropertyDelegate(elements, annotationType, name)
        cl.delegate = prop
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
        if (prop.type == null) {
            throw new VocabularyException("No type defined for property '${name}'.")
        }
        if (prop.description == null) {
            throw new VocabularyException("No description defined for property '${name}'.")
        }
        properties[name] = prop
    }
}
