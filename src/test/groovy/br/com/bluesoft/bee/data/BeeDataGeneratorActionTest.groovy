package br.com.bluesoft.bee.data

import br.com.bluesoft.bee.database.ConnectionInfo
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.service.BeeWriter
import br.com.bluesoft.bee.testutils.BeeTesterSpecification
import groovy.sql.Sql
import org.junit.After
import org.junit.Before

class BeeDataGeneratorActionTest extends BeeTesterSpecification {

    private static final List<LinkedHashMap<String, String>> PERMISSAO_DATA = [
            [permissao_key: "1 ", descricao: "permissao 1"],
            [permissao_key: "2", descricao: "permissao 2 "]
    ]

    private static final List<LinkedHashMap<String, String>> MENU_DATA = [
            [menu_key: "1 ", descricao: "menu 1"],
            [menu_key: "2", descricao: "menu 2 "]
    ]


    Sql sql

    @Before
    void beforeClass() {
        createBeeFolders()

        sql = getSqlMock()
        GroovyMock(ConnectionInfo, global: true)
        ConnectionInfo.createDatabaseConnection(_, _) >> sql
    }


    def "Deve gerar o data para a tabela"() {
        given: "Dada uma tabela"
        def mensagens = []
        Options options = new Options()
        options.parse(
                ["data:validate", "-d", "/tmp/bee", "-c", "src/test/resources/test.properties", "docker", "permissao"])
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        mockSqlEachRow(sql, PERMISSAO_DATA)

        when: "Quando executar o gerador"
        def result = new BeeDataGeneratorAction(options: options, out: logger).run()

        then: "e retorna true"
        result == true
        and: 'e deve gerar para permissão'

        File file = new File("${BeeTesterSpecification.BEE_TEMP_DIRECTORY}/data/permissao.csv")
        file.getText() == "1,permissao 1\n2,permissao 2\n"

        and: 'Deve ter gerado log'
        mensagens[1] == 'Extracting the table data to permissao ... '
    }

    def "Deve gerar o data para mais de uma tabela"() {
        given: "Dada uma tabela"
        def mensagens = []
        Options options = new Options()
        options.parse(
                ["data:validate", "-d", "/tmp/bee", "-c", "src/test/resources/test.properties", "docker", "permissao", "menu"])
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter

        mockSqlEachRowConditional(sql,
                                  "select permissao_key,descricao from permissao order by permissao_key",
                                  PERMISSAO_DATA)
        mockSqlEachRowConditional(sql, "select menu_key,descricao from menu order by menu_key",
                                  MENU_DATA)

        when: "Quando executar o gerador"
        def result = new BeeDataGeneratorAction(options: options, out: logger).run()


        then: "Deve retorna true"
        result == true

        and: 'e deve gerar para permissão'

        File file = new File("${BeeTesterSpecification.BEE_TEMP_DIRECTORY}/data/permissao.csv")
        file.getText() == "1,permissao 1\n2,permissao 2\n"

        and: 'e deve gerar para permissão'

        File menuFile = new File("${BeeTesterSpecification.BEE_TEMP_DIRECTORY}/data/menu.csv")
        menuFile.getText() == "1,menu 1\n2,menu 2\n"

        and: 'Deve ter gerado log'
        mensagens[1] == 'Extracting the table data to permissao ... '
        mensagens[3] == 'Extracting the table data to menu ... '
    }

    @After
    void 'remove temporary files'() {
        new File('/tmp/bee').deleteDir()
    }
}
