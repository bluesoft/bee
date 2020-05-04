package br.com.bluesoft.bee.data

import spock.lang.Specification
import spock.lang.Unroll

class BeeDataModuleTest extends Specification {


    def "Deve exibir o uso correto"() {
        given: "um module"

        def module = new BeeDataModule()
        def buffer = new ByteArrayOutputStream()
        System.out = new PrintStream(buffer)

        when: "chamar o usage"
        module.usage()
        then: "o uso deve ser do Data"
        buffer.toString() == """usage: bee <options> data:action <options>
Actions:
         data:generate connection object - generates an entire schema data or single object, if specified
         data:validate connection [object] - validates an entire schema data or single object, if specified
"""
    }

    @Unroll('Deve retornar o runner #runner para a ação #action')
    def "Deve retornar a Action"() {
        expect:
        new BeeDataModule().getRunner(action, null, null).class == runner

        where:
        action     | runner
        "generate" | BeeDataGeneratorAction
        "validate" | BeeDataValidatorAction

    }
}
