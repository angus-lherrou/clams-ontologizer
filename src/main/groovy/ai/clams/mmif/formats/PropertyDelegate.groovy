package ai.clams.mmif.formats

/**
 * @author Keith Suderman
 */
class PropertyDelegate {
    /*
    static final String DATATYPE = "http://vocab.lappsgrid.org/Datatype"
    static final TYPE_MAP = [
            ID: "xsd:ID",
            "Integer": "xsd:long",
            "List of IDs": "xsd:IDREFS",
            "List of URI": "$DATATYPE#list_uri",
            "Set of IDs": "xsd:IDREFS",
            "String": "xsd:string",
            "String or URI": "xsd:string"
    ]
    */
    String type
    String description
    boolean required = false;
    boolean requiredSet = false;
    String annotationType
    String name
    List<ElementDelegate> elements

    PropertyDelegate(List<ElementDelegate> elements, String annotationType, String name) {
        this.elements = elements
        this.annotationType = annotationType
        this.name = name
    }

    void setProperty(String name, value) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    void type(String type) {
//        this.type = TYPE_MAP[type] ?: type
        this.type = type
    }

    void description(String description) {
        this.description = description
    }

    void required(boolean required) {
        if (requiredSet && this.required != required) {
            throw new VocabularyException("The required property has already been set.")
        }
        requiredSet = true
        this.required = required
    }

    void optional(boolean optional) {
        required(!optional)
    }

    void discriminator(boolean isOne) {
        if (isOne) {
            Map args = [
                    name: "${annotationType}#${name}",
                    discriminator:name,
                    definition:description,
                    elements:elements
//                    parent: annotationType
            ]
            elements << new ElementDelegate(args)
        }
    }
}
