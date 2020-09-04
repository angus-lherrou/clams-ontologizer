/*
 * Copyright (c) 2019 The American National Corpus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.clams.mmif.formats

import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory

/**
 *
 */
class DatatypeDelegate {

    Node root
    Map types

    DatatypeDelegate(Map types) {
        this.types = types
        root = new Node(null, "xs:schema")
        root.attributes().with {
            put("xmlns:xs", "http://www.w3.org/2001/XMLSchema")
            put("attributeFormDefault", "unqualified")
            put("elementFormDefault", "qualified")
        }
    }

//    void simpleType(String name, Closure body) {
//        simpleType(name, name, body)
//    }
//
//    void simpleType(String name, String longName, Closure body) {
//        if (types[longName] != null) {
//            throw new VocabularyException("Redefintion of type $longName")
//        }
//        //TODO: magic version number
//        Resource type = ResourceFactory.createResource("$Formats.VOCAB_PREFIX/0.1.0/$Formats.VOCAB_DIR/Datatype#${name}")
//        types.put(longName, type)
//        XmlDelegate delegate = new XmlDelegate(root,"xs:simpleType", types, type)
//        body.delegate = delegate
//        body.resolveStrategy = Closure.DELEGATE_FIRST
//        body()
//        Node node = delegate.node
//        node.attributes().with {
//            put("name", name)
//            put("id", name)
//        }
//    }
//
//    void complexType(String name, Closure body) {
//        complexType(name, name, body)
//    }
//
//    void complexType(String name, String longName, Closure body) {
//        if (types[longName] != null) {
//            throw new VocabularyException("Redefintion of type $longName")
//        }
//        //TODO: magic version number
//        Resource type = ResourceFactory.createResource("$Formats.VOCAB_PREFIX/0.1.0/$Formats.VOCAB_DIR/Datatype#${name}")
//        types.put(longName, type)
//        XmlDelegate delegate = new XmlDelegate(root, "xs:complexType", types, type)
//        body.delegate = delegate
//        body.resolveStrategy = Closure.DELEGATE_FIRST
//        body()
//        Node node = delegate.node
//        node.attributes().with {
//            put("name", name)
//            put("id", name)
//        }
//    }
}

class XmlDelegate {
    Node node
    Resource type
    Map types

    XmlDelegate(Node parent, String name, Map types, Resource type) {
        node = new Node(parent, name)
        this.type = type
        this.types = types
    }

    void alias(String name) {
        if (types[name] != null) {
            throw new VocabularyException("There is already a type with the name $name")
        }
        println "Aliasing $name to $type"
        types[name] = type
    }

    void methodMissing(String name, args) {
        String qName = "xs:$name"
        if (args[0] instanceof Closure) {
            Closure cl = (Closure) args[0]
            cl.delegate = new XmlDelegate(node, qName, types, type)
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }
        else if (args[0] instanceof Map) {
            Node child = new Node(node, qName)
            Map atts = child.attributes()
            ((Map)args[0]).each { k,v ->
                atts[k] = v
            }
        }
        else {
            println "XmlDelegate method : $name args: ${args}"

        }
    }
}
