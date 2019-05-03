package br.com.bluesoft.bee.schema

import spock.lang.Specification
import spock.lang.Unroll

class BeeSchemaModuleTest extends Specification {


    def "Deve exibir o uso correto"() {
        given: "um module"

        def module = new BeeSchemaModule()
        def buffer = new ByteArrayOutputStream()
        System.out = new PrintStream(buffer)

        when: "chamar o usage"
        module.usage()
        then: "o uso deve ser do Data"
        buffer.toString()  == """usage: bee <options> schema:action <parameters>
Actions:
         schema:generate connection [object] - generates an entire schema or single object, if specified
         schema:validate connection [object] - validates an entire schema or single object, if specified
         schema:recreate_mysql [object] - build a MySql DDL script
         schema:recreate_oracle [object] - build a Oracle DDL script
         schema:recreate_postgres [object] - build a Postgres DDL script
         schema:check - validate structure correctness
"""
    }

    @Unroll('Deve retornar o runner #runner para a ação #action')
    def "Deve retornar a Action"() {
        expect:
        new BeeSchemaModule().getRunner(action, null, null).class == runner

        where:
        action              | runner
        "generate"          | BeeSchemaGeneratorAction
        "validate"          | BeeSchemaValidatorAction
        "recreate_mysql"    | BeeMySqlSchemaCreatorAction
        "recreate_oracle"   | BeeOracleSchemaCreatorAction
        "recreate_postgres" | BeePostgresSchemaCreatorAction
        "check"             | BeeSchemaCheckerAction

    }
}
