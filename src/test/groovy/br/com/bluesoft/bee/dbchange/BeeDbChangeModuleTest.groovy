package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.exceptions.IncorrectUsageException
import br.com.bluesoft.bee.model.Options
import spock.lang.Specification
import spock.lang.Unroll

class BeeDbChangeModuleTest extends Specification {


    def "Deve exibir o uso correto"() {
        given: "um module"

        def module = new BeeDbChangeModule()
        def buffer = new ByteArrayOutputStream()
        System.out = new PrintStream(buffer)

        when: "chamar o usage"
        module.usage()
        then: "o uso deve ser do Data"
        buffer.toString() == """usage: bee <options> dbchange:action <parameters>
Actions:
         dbchange:create description <group> - creates a dbchange file
         dbchange:status connection <group> - lists dbchanges to run
         dbchange:up connection <file> - runs all pending dbchange files, or one if specified
         dbchange:down connection file - runs a dbchange rollback action
         dbchange:mark connection file - mark a file as executed
         dbchange:markall connection <group> - mark all files as executed
         dbchange:unmark connection file - unmark a file as executed
         dbchange:find name - search file
         dbchange:open name - open file with EDITOR
         dbchange:group_up connection group - runs all pending dbchange files of the group
"""
    }

    def "Deve exibir o erro quando não informar o uso correto"() {
        given:
        Options options = new Options()
        options.parse(
                ["dbchange:creat", "menu"])
        when:
        new BeeDbChangeModule().run(options)
        then:
        IncorrectUsageException ex = thrown(IncorrectUsageException)
        ex.class == IncorrectUsageException

    }

    @Unroll('Deve retornar o runner #runner para a ação #action')
    def "Deve retornar a Action"() {
        expect:
        new BeeDbChangeModule().getRunner(action, null, null).class == runner

        where:
        action     | runner
        "create"   | BeeDbChangeCreateAction
        "status"   | BeeDbChangeStatusAction
        "up"       | BeeDbChangeUpAction
        "down"     | BeeDbChangeDownAction
        "mark"     | BeeDbChangeMarkAction
        "markall"  | BeeDbChangeMarkAllAction
        "unmark"   | BeeDbChangeUnmarkAction
        "find"     | BeeDbChangeFindAction
        "open"     | BeeDbChangeOpenAction
        "group_up" | BeeDbChangeGroupUpAction

    }

}
