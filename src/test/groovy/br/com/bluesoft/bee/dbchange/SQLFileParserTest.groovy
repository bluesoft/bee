package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.util.RDBMS
import spock.lang.Specification

class SQLFileParserTest extends Specification {

    def "deve fazer o parse de um arquivo"() {

        expect:
        def parser = new SQLFileParser()
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
                [up: ["  select * from a \n", "  select * from b\n"], header: null, down: null],
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

    def 'should find corret database type'() {
        expect:
        def parser = new SQLFileParser(rdbms: type)
        parser.getRDBMS(line) == type

        where:
        line << [
                "nothing",
                "::up",
                "::up::oracle",
                "::up::postgres",
                "::up::mysql"
        ]
        type << [
                null,
                null,
                RDBMS.ORACLE,
                RDBMS.POSTGRES,
                RDBMS.MYSQL
        ]
    }

    def 'should parse a speficic rdsms type'() {
        expect:
        def parser = new SQLFileParser(rdbms: RDBMS.ORACLE)
        resultado == parser.parseFile(arquivo)

        where:
        arquivo << [
                "::up\nselect * from a;\n",
                "::up\nselect * from a;\n::up::oracle\nselect * from b;\n",
                "::up\nselect * from a;\n::up::oracle\nselect * from b;\n::up::postgres\nselect * from c;\n",
                "::down\nselect * from a;\n",
                "::down\nselect * from a;\n::down::oracle\nselect * from b;\n",
                "::down\nselect * from a;\n::down::oracle\nselect * from b;\n::up::postgres\nselect * from c;\n",
        ]
        resultado << [
                [up: ["select * from a\n"], header: null, down: null],
                [up: ["select * from b\n"], header: null, down: null],
                [up: ["select * from b\n"], header: null, down: null],
                [up: null, header: null, down: ["select * from a\n"]],
                [up: null, header: null, down: ["select * from b\n"]],
                [up: null, header: null, down: ["select * from b\n"]],
        ]
    }

}
