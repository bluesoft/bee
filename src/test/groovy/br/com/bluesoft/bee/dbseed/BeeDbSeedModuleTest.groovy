package br.com.bluesoft.bee.dbseed

import spock.lang.Specification
import spock.lang.Unroll

class BeeDbSeedModuleTest extends Specification {


    def "Deve exibir o uso correto"() {
        given: "um module"

        def module = new BeeDbSeedModule()
        def buffer = new ByteArrayOutputStream()
        System.out = new PrintStream(buffer)

        when: "chamar o usage"
        module.usage()
        then: "o uso deve ser do Data"
        buffer.toString()  == """usage: bee <options> dbseed:action <parameters>
Actions:
         dbseed:create description - creates a dbseed file
         dbseed:status connection - lists dbseeds to run
         dbseed:up connection <file> - runs all pending dbseed files, or one if specified
         dbseed:down connection file - runs a dbseed rollback action
         dbseed:mark connection file - mark a file as executed
         dbseed:markall connection - mark All files as executed
         dbseed:unmark connection file - unmark a file as executed
"""
    }

    @Unroll('Deve retornar o runner #runner para a ação #action')
    def "Deve retornar a Action"() {
        expect:
        new BeeDbSeedModule().getRunner(action, null, null).class == runner

        where:
        action      | runner
        "create"    | BeeDbSeedCreateAction
        "status"    | BeeDbSeedStatusAction
        "up"        | BeeDbSeedUpAction
        "down"      | BeeDbSeedDownAction
        "mark"      | BeeDbSeedMarkAction
        "markall"   | BeeDbSeedMarkAllAction
        "unmark"    | BeeDbSeedUnmarkAction

    }
}
