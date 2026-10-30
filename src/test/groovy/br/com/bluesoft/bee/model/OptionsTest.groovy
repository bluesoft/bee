package br.com.bluesoft.bee.model;

import spock.lang.Specification

public class OptionsTest extends Specification {

    def "it shoud parse and validate the options"() {
        given:
        def options = new Options()
        def result = options.parse(args)
        expect:
        result == good
        options.arguments == arguments
        where:
        args                                                                                | good  | arguments
        []                                                                                  | false | []
        ["bee:generate"]                                                                    | false | []
        ["-d", ".", "-c", "src/test/resources/test.properties", "bee:generate"]             | true  | []
        ["-d", ".", "-c", "src/test/resources/test.properties", "bee:generate", "xx", "yy"] | true  | ["xx", "yy"]
        ["bee:generate", "-d", ".", "-c", "src/test/resources/test.properties", "xx", "yy"] | true  | ["xx", "yy"]
    }

    def "it should parse the quiet option"() {
        given:
        def options = new Options()
        options.parse(["-d", ".", "-c", "src/test/resources/test.properties"] + args)
        expect:
        options.quiet == quiet
        options.arguments == arguments
        where:
        args                                     | quiet | arguments
        ["dbchange:up", "conn"]                  | false | ["conn"]
        ["-q", "dbchange:up", "conn"]            | true  | ["conn"]
        ["dbchange:up", "conn", "-q"]            | true  | ["conn"]
        ["dbchange:down", "--quiet", "conn", "x"] | true  | ["conn", "x"]
    }
}
