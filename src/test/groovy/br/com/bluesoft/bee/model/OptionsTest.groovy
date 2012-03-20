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
			args 																						| good 	| arguments
			[]   																						| false | []
			[ "bee:generate" ] 																			| false | []
			[ "-d", ".", "-c", "src/test/resources/test.properties", "bee:generate"  ]					| true 	| []
			[ "-d", ".", "-c", "src/test/resources/test.properties", "bee:generate", "xx", "yy"  ]		| true 	| [ "xx", "yy" ]
			[ "bee:generate", "-d", ".", "-c", "src/test/resources/test.properties", "xx", "yy"  ]		| true 	| [ "xx", "yy" ]
	}
}
