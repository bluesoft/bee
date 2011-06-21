package br.com.bluesoft.bee.dbchange

import org.junit.*

import spock.lang.Specification


class SQLFileParserTest extends Specification {

	def parser = new SQLFileParser()

	def "deve fazer o parse de um arquivo" () {

		expect:
		resultado == parser.parseFile(arquivo)

		where:
		arquivo << [
			"::up\nselect * from a;",
			"::up\nselect * from a;\n::down\n",
			"::up\nselect * from a\nwhere b = 1;",
			"::up\nselect * from a; \n select * from b;",
			"::up\n  select * from a ; \n  select * from b; ",
			"::up\n--!code\nbegin\na:= 1;\n end;\n/",
			"::up\n--!code\nbegin\na:= 1;\n end;\n/\nselect * from x;",
			"::up\n--comentario\n--!code\nbegin\na:= 1;\n end;\n/\nselect * from x;",
			"::up\n--!code\nbegin\na:= 1;\n end;\n/\n--comentario\nselect * from x;",
			"-- test header \n\n-- teste2\n::up\n--!code\nbegin\na:= 1;\n end;\n/\n--comentario\nselect * from x;",
			"::up\nselect * from a;\n::down\n\nselect * from b;\n",
			"::up\n::down\n\nselect * from b;\n",
			"::down\n\nselect * from b;\n",
			"::up\n--!code\naa\nbb\n/\n"
		]
		resultado << [
			[up: ["select * from a\n"], header: null, down: null],
			[up: ["select * from a\n"], header: null, down: []],
			[up: ["select * from a\nwhere b = 1\n"], header: null, down: null],
			[up: ["select * from a\n", " select * from b\n"], header: null, down: null],
			[up: ["  select * from a \n","  select * from b\n"], header: null, down: null],
			[up: ["begin\na:= 1;\n end;\n"], header: null, down: null],
			[up: ["begin\na:= 1;\n end;\n", "select * from x\n"], header: null, down: null],
			[up: ["begin\na:= 1;\n end;\n", "select * from x\n"], header: null, down: null],
			[up: ["begin\na:= 1;\n end;\n", "select * from x\n"], header: null, down: null],
			[up: ["begin\na:= 1;\n end;\n", "select * from x\n"], header: "test header", down: null],
			[up: ["select * from a\n"], header: null, down: ["select * from b\n"]],
			[up: [], header: null, down: ["select * from b\n"]],
			[up: null, header: null, down: ["select * from b\n"]],
			[up: ["aa\nbb\n"], header: null, down: null],
		]

	}
}