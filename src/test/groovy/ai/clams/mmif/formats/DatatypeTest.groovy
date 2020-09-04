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

import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.junit.Ignore
import org.junit.Test

/**
 *
 */
@Ignore
class DatatypeTest {

    @Test
    void print() {
        println XSDDatatype.XSDIDREF.getURI()
    }

    @Test
    void datatype() {
        File file = new File('src/test/resources/Datatype.schema')
        if (!file.exists()) {
            println "Working directory was not set correctly."
            return
        }

        GroovyShell shell = new GroovyShell()
        Script script = shell.parse(file)
        ExpandoMetaClass meta = new ExpandoMetaClass(script.class, false)
//        Map<String,Datatype> types
//        List<Node> nodes
        Node root
        meta.Datatypes { Closure cl ->
            cl.delegate = new DatatypeDelegate()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
//            nodes = cl.delegate.nodes
            root = cl.delegate.root
        }
        meta.Datatype = meta.Datatypes
        meta.initialize()
        script.metaClass = meta
        script.run()

//        Node root = new Node(null, 'xs:schema')
//        root.attributes().put("xmlns:xs", "http://example.org")
//        nodes.each { root.append(it) }

        println XmlUtil.serialize(root)
//        println root.toString()
//        StringWriter writer = new StringWriter()
//        MarkupBuilder builder = new MarkupBuilder(writer)
//        build(builder, types)
//        println writer.toString()
    }

    void build(MarkupBuilder builder, Map map) {
        builder.with {
            map.collect {k,v ->
                "$k" { build(builder, v) }
            }
        }
    }

    void build(MarkupBuilder builder, Object[] list) {
        builder.with {
            list.each { item ->
                li { build(builder, item) }
            }
        }
    }

    void build(MarkupBuilder builder, Object value) {
        if (value) {
            builder.with { mkp.yield(value) }
        }
    }

    @Test
    void fake() {
        String source = '''
            println "Hello $name" 
            greet(name)
            run {
                println "printing Hello $name"
                greet("greeting world")
                foo("bar")
                bar("root")
            }
         '''
        Binding binding = new Binding()
        binding.setVariable("name", "World")
        GroovyShell shell = new GroovyShell(binding)
        Script script = shell.parse(source)
        MetaClass meta = new ExpandoMetaClass(script.class, false)
        meta.greet = { String name ->
            println "Hello $name"
        }
        meta.run = { Closure cl ->
            cl.delegate = new Handler()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }
        meta.initialize()
        script.metaClass = meta
        script.run()
    }

}

class Handler {

    Handler() {

    }

    void foo(String value) {
        println "Foo: $value"
    }

    def propertyMissing(String name, value) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def propertyMissing(String name) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def methodMissing(String name, args) {
        println "Handling $name"
        true
    }
}
