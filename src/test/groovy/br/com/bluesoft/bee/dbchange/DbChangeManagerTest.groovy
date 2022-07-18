package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.database.ConnectionInfo
import br.com.bluesoft.bee.service.BeeWriter
import br.com.bluesoft.bee.util.QueryDialectHelper
import br.com.bluesoft.bee.util.RDBMSUtilTest
import groovy.sql.Sql
import org.junit.After
import org.junit.Before
import spock.lang.Specification

import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.SQLException

class DbChangeManagerTest extends Specification {

    @Before
    void 'create temporary files'() {
        new File('/tmp/bee').mkdirs()
    }

    final static def RESULT = [
            [ARQUIVO_NOME: "65564564-test.dbchange"]
    ]

    def "deve retornar uma lista com os arquivos a serem executados e na ordem"() {
        given:
        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def sql = [execute: { instrucao -> [] }, rows: { instrucao -> RESULT }, close: {}]
        def directoryFile = [list: {
            [
                    "abc",
                    "65564564-test.dbchange",
                    "989899-test.dbchange",
                    "989898-test.dbchange"
            ]
        }]
        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger)

        when: "listar dbchanges"
        def lista = manager.listar()

        then: "deve conter somente as instrucoes que nao foram executadas"
        lista.size() == 2

        lista == [
                "989898-test.dbchange",
                "989899-test.dbchange"
        ]
    }

    def "deve retornar uma lista com os arquivos do grupo a serem executados e na ordem"() {
        given:
        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def sql = [execute: { instrucao -> [] }, rows: { instrucao -> [[ARQUIVO_NOME: '234234-test.dbchange.grupo']] }, close: {}]
        def directoryFile = [list: {
            [
                    "abc",
                    "65564564-test.dbchange",
                    "999999-test.dbchange.grupo",
                    "989899-test.dbchange",
                    "989898-test.dbchange",
                    "234234-test.dbchange.grupo",
                    "123123-test.dbchange.grupo"
            ]
        }]
        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger)
        def grupo = "grupo"

        when: "listar dbchanges"
        def lista = manager.listar(grupo)

        then: "deve conter somente as instrucoes que nao foram executadas"
        lista.size() == 2

        lista == [
                "123123-test.dbchange.grupo",
                "999999-test.dbchange.grupo"
        ]
    }

    def "deve retornar false quando a lista de arquivos ja executados for nula"() {
        given:
        def mensagens = []
        def directoryFile = [list: {
            [
                    "abc",
                    "65564564-test.dbchange",
                    "989898-test.dbchange"
            ]
        }]
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def manager = new DbChangeManager(directoryFile: directoryFile, logger: logger, configFile: Mock(File))
        ConnectionInfo.metaClass.static.createDatabaseConnection = { def a, def b -> return null }

        when: "listar dbchanges a lista do banco retorna null"
        def lista = manager.listar()

        then: "deve retornar null e conter a mensagem"
        lista == null
        mensagens.size() == 1
        mensagens[0] == DbChangeManager.MESSAGE_COULD_NOT_GET_CONNECTION
    }

    def "deve listar as instrucoes ja executadas no banco"() {
        given:
        def sql = [execute: { instrucao -> [] }, rows: { instrucao -> RESULT }, close: {}]
        def manager = new DbChangeManager(sql: sql)

        when: "listar instrucoes ja executadas"
        def listaBanco = manager.listarInstrucoesJaExecutadas()

        then:
        listaBanco == RESULT
    }

    def "deve listar os arquivos"() {
        given:
        def directoryFile = [list: {
            ["abc", "65564564-test.dbchange"
            ]
        }]
        def manager = new DbChangeManager(directoryFile: directoryFile)

        when: "listar arquivos de dbchanges"
        def listaArquivos = manager.listarArquivos()

        then:
        listaArquivos == RESULT
    }

    def "deve listar os arquivos do grupo"() {
        given:
        def directoryFile = [list: {
            [
                    "abc",
                    "65564564-test.dbchange",
                    "88726692-test.dbchange.grupo"
            ]
        }]
        def manager = new DbChangeManager(directoryFile: directoryFile)
        def grupo = "grupo"

        when: "listar arquivos de dbchanges"
        def listaArquivos = manager.listarArquivos(grupo)

        then:
        listaArquivos == [[ARQUIVO_NOME: "88726692-test.dbchange.grupo"]]
    }

    def "deve criar a tabela dbchanges caso nao exista em banco oracle"() {

        given:
        def manager = new DbChangeManager(directoryFile: mockDirectoryFile(), logger: mockLogger(), configFile: getProperties("/oracleTest.properties"), clientName: "test")
        def sql = Mock(Sql)
        1 * sql.execute(DbChangeManager.SELECT_TABLE) >> { throw new SQLException() }

        when: "criar tabela caso nao exista"
        def retorno = manager.criarTabelaDbchangesSeNaoExistir(sql)

        then: "e retorna true"
        retorno == true
    }

    def "deve criar a tabela dbchanges caso nao exista em banco mysql"() {

        given:
        def manager = new DbChangeManager(directoryFile: mockDirectoryFile(), logger: mockLogger(), configFile: getProperties("/mySqlTest.properties"), clientName: "test")
        def sql = Mock(Sql)
        def createTableQuery = QueryDialectHelper.getCreateTableDbchangesQuery(getProperties("/mySqlTest.properties"), "test")

        1 * sql.execute(DbChangeManager.SELECT_TABLE) >> { throw new SQLException() }

        when: "criar tabela caso nao exista"
        def retorno = manager.criarTabelaDbchangesSeNaoExistir(sql)

        then: "e retorna true"
        retorno == true
    }

    def "deve criar a tabela dbchanges caso nao exista em banco postgres"() {

        given:
        def manager = new DbChangeManager(directoryFile: mockDirectoryFile(), logger: mockLogger(), configFile: getProperties("/postgresTest.properties"), clientName: "test")
        def sql = Mock(Sql)
        def createTableQuery = QueryDialectHelper.getCreateTableDbchangesQuery(getProperties("/postgresTest.properties"), "test")

        1 * sql.execute(DbChangeManager.SELECT_TABLE) >> { throw new SQLException() }

        when: "criar tabela caso nao exista"
        def retorno = manager.criarTabelaDbchangesSeNaoExistir(sql)

        then: "e retorna true"
        retorno == true
    }


    def "deve retornar false caso nao consiga criar a tabela dbchanges"() {

        given:
        def sql = Mock(Sql)
        def logger = Mock(BeeWriter)
        def manager = new DbChangeManager(directoryFile: mockDirectoryFile(), logger: logger, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        def createTableQuery = QueryDialectHelper.getCreateTableDbchangesQuery(getProperties("/oracleTest.properties"), "test")

        1 * sql.execute(DbChangeManager.SELECT_TABLE) >> { throw new SQLException() }
        1 * sql.execute(createTableQuery) >> { throw new SQLException() }
        1 * logger.log(_)

        when: "Ocorrer um erro ao criar a tabela"
        def retorno = manager.criarTabelaDbchangesSeNaoExistir(sql)

        then: "Retorna false e loga o erro"
        !retorno
    }

    def "deve retornar false caso nao consiga obter a conexao com o banco"() {

        given:
        def mensagens = []
        def sql = Mock(Sql)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def manager = new DbChangeManager(logger: logger)

        when: "passar a sql null"
        def retorno = manager.criarTabelaDbchangesSeNaoExistir(null)

        then: "Retorna false e loga o erro"
        retorno == false
        mensagens.size() == 1
        mensagens[0] == DbChangeManager.MESSAGE_COULD_NOT_GET_CONNECTION
    }

    def "deve inserir uma execucao de dbchange quando o parametro UpDown for igual a UP"() {
        given:
        def sql = mockSql()
        def arquivo = "989898-test.dbchange"
        def logger = Mock(BeeWriter)
        def manager = new DbChangeManager(directoryFile: mockDirectoryFile(), logger: logger, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        def createTableQuery = QueryDialectHelper.getInsertIntoDbchangesQuery(getProperties("/oracleTest.properties"), "test")

        when: "salvar execucao de dbchange"
        manager.salvarExecucao(sql, arquivo, UpDown.UP)

        then: "deve inserir execucao e commitar"
        1 * sql.execute(createTableQuery, _)
        2 * sql.commit()
    }

    def "deve excluir uma execucao de dbchange quando o parametro UpDown for igual a DOWN"() {
        given:
        def sql = mockSql()
        def arquivo = "989898-test.dbchange"
        def logger = Mock(BeeWriter)
        def manager = new DbChangeManager(directoryFile: mockDirectoryFile(), logger: logger, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        def deleteQuery = QueryDialectHelper.getDeleteFromDbchangesQuery(getProperties("/oracleTest.properties"), "test")

        2 * sql.commit()
        1 * sql.execute(deleteQuery, _)

        when: "excluir execucao de dbchange"
        manager.salvarExecucao(sql, arquivo, UpDown.DOWN)

        then: "deve excluir execucao e commitar"
    }


    def "deve parsear, executar e gravar varias dbchanges"() {

        given:
        def mensagens = []
        def sql = mockSql()
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def lista = [
                "989898-test.dbchange",
                "65564564-test.dbchange"
        ]
        def dbchange = [
                [header: "carneiro", up: ["z", "x"], down: []],
                [header: "ismels", up: ["w", "e"], down: []]
        ]

        2 * parser.parseFile(_) >>> dbchange
        2 * sql.rows(_, _) >> []

        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarVariasDbChanges(lista, UpDown.UP)

        then:
        resultado == true
        mensagens.size() == 12
        mensagens[0] == "Executing dbchange: 989898-test.dbchange -- carneiro"
        mensagens[4].startsWith("Time elapsed")
        mensagens[5].startsWith("Execution time:")
        mensagens[6] == "Executing dbchange: 65564564-test.dbchange -- ismels"
    }

    def "deve parsear, executar e gravar uma dbchange"() {

        given:
        def mensagens = []
        def sql = mockSql()
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbchange"
        def dbchange = [header: "carneiro", up: ["z", "x"], down: []]

        1 * parser.parseFile(_) >> dbchange
        1 * sql.rows(_, _) >> []

        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbChange(arquivo, UpDown.UP)

        then:
        resultado == true
        mensagens.size() == 6
        mensagens[0] == "Executing dbchange: 989898-test.dbchange -- carneiro"
        mensagens[4].startsWith("Time elapsed")
        mensagens[5].startsWith("Execution time:")
    }

    def "deve parsear, executar e gravar uma dbchange quando o parametro UpDown = DOWN"() {

        given:
        def mensagens = []
        def sql = mockSql()
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbchange"
        def dbchange = [header: "carneiro", up: [], down: ["z", "x"]]

        1 * parser.parseFile(_) >> dbchange
        1 * sql.rows(_, _) >> ["select bla"]

        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbChange(arquivo, UpDown.DOWN)

        then:
        resultado == true
        mensagens.size() == 6
        mensagens[0] == "Executing dbchange: 989898-test.dbchange -- carneiro"
        mensagens[4].startsWith("Time elapsed")
        mensagens[5].startsWith("Execution time:")
    }

    def "deve retornar mensagem quando nao houver comandos no arquivo"() {

        given:
        def mensagens = []
        def sql = mockSql()
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbchange"
        def dbchange = [header: "carneiro", up: [], down: []]

        1 * parser.parseFile(_) >> dbchange

        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser)
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbChange(arquivo, UpDown.UP)

        then:
        resultado == true
        mensagens.size() == 2
        mensagens[0] == "Executing dbchange: 989898-test.dbchange -- carneiro"
        mensagens[1] == DbChangeManager.MESSAGE_NO_COMMANDS_IN_FILE
    }

    def "deve retornar false quando ocorrer um erro de sql"() {

        given:
        def mensagens = []
        def sql = mockSql()
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbchange"
        def dbchange = [header: "carneiro", up: ["x"], down: []]

        1 * parser.parseFile(_) >> dbchange
        1 * sql.rows(_, _) >> []
        1 * sql.executeUpdate(_) >> { throw new SQLException("Erro") }

        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        manager.metaClass.getFile = { def a, def b -> return [] }
        manager.metaClass.salvarExecucao = { def a, def b, def c -> assert false }

        when: "executar dbchange com erro"
        def resultado = manager.executarDbChange(arquivo, UpDown.UP)

        then: "retorna false e nao pode salvar a execucao da dbchange"
        resultado == false
        mensagens.size() == 4
        mensagens[0] == "Executing dbchange: 989898-test.dbchange -- carneiro"
        mensagens[2] == "!!!Error: Erro"
    }

    def "deve retornar false quando nao existir um execucao anterior e parametro UpDown = DOWN"() {

        given:
        def mensagens = []
        def sql = mockSql()
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbchange"
        def dbchange = [header: "carneiro", up: [], down: ["z", "x"]]

        1 * parser.parseFile(_) >> dbchange
        1 * sql.rows(_, _) >> []

        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, configFile: getProperties("/oracleTest.properties"), clientName: "test", force: true)
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbChange(arquivo, UpDown.DOWN)

        then:
        resultado == false
        mensagens.size() == 2
        mensagens[0] == "Executing dbchange: 989898-test.dbchange -- carneiro"
        mensagens[1] == DbChangeManager.MESSAGE_DBCHANGE_NOT_EXECUTED
    }

    def "deve retornar false quando ja existir um execucao anterior e parametro UpDown = UP"() {

        given:
        def mensagens = []
        def sql = mockSql()
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbchange"
        def dbchange = [header: "carneiro", up: ["z", "x"], down: ["z", "x"]]

        1 * parser.parseFile(_) >> dbchange
        1 * sql.rows(_, _) >> ["xxx"]

        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, configFile: getProperties("/oracleTest.properties"), clientName: "test", force: false)
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbChange(arquivo, UpDown.UP)

        then:
        resultado == false
        mensagens.size() == 2
        mensagens[0] == "Executing dbchange: 989898-test.dbchange -- carneiro"
        mensagens[1] == DbChangeManager.MESSAGE_DBCHANGE_ALREADY_EXECUTED
    }

    def "deve retornar true mesmo quando ja existir um execucao anterior caso seja forçado 'force=true' "() {

        given:
        def mensagens = []
        def sql = mockSql()
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbchange"
        def dbchange = [header: "carneiro", up: ["z", "x"], down: ["z", "x"]]

        1 * parser.parseFile(_) >> dbchange
        1 * sql.rows(_, _) >> ["xxx"]

        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, configFile: getProperties("/oracleTest.properties"), clientName: "test", force: true)
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbChange(arquivo, UpDown.UP)

        then:
        resultado == true
        mensagens.size() == 6
        mensagens[0] == "Executing dbchange: 989898-test.dbchange -- carneiro"
        mensagens[5].startsWith("Execution time:")
    }

    def "deve retornar false quando o nome do arquivo for inválido"() {

        given: "arquivo com nome errado"
        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def arquivo = "989898-test.dbchang"
        def manager = new DbChangeManager(logger: logger)

        when:
        def resultado = manager.executarDbChange(arquivo, UpDown.UP)

        then: "Retorna mensagem de nome do arquivo invalido"
        resultado == false
        mensagens.size() == 1
        mensagens[0] == DbChangeManager.MESSAGE_INVALID_FILE_NAME
    }

    def "deve retornar false quando nao conseguir obter a conexao com o banco"() {

        given: "arquivo com nome do banco errado"
        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def arquivo = "989898-test.dbchange"
        def manager = new DbChangeManager(configFile: Mock(File), logger: logger)
        ConnectionInfo.metaClass.static.createDatabaseConnection = { def a, def b -> return null }

        when: "o metodo createDatabaseConnection retornar null"
        def resultado = manager.executarDbChange(arquivo, UpDown.UP)

        then: "retorna false e a mensagem que nao conseguiu obter conexao"
        resultado == false
        mensagens.size() == 1
        mensagens[0] == DbChangeManager.MESSAGE_COULD_NOT_GET_CONNECTION
    }

    def "deve retornar o timestamp do nome de arquivo"() {

        given:
        def manager = new DbChangeManager()
        expect:
        manager.obterTimestamp(arquivo) == timestamp
        where:
        timestamp | arquivo
        "989898"  | "989898-test.dbchange"
        "132123"  | "132123-x.dbchange"
        "0"       | "789987-teste"
    }

    def "deve marcar todos os dbchanges"() {
        given:
        def sql = mockSql()
        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: {
            ["abc", "65564564-test.dbchange"
            ]
        }]
        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger, configFile: getProperties("/oracleTest.properties"), clientName: "test")

        when: "marcar todos os dbchanges"
        manager.markAll()

        then:
        mensagens.size() == 2
        mensagens[0] == "marking 1 file(s)"
        mensagens[1] == "65564564-test.dbchange marked as implemented"
    }

    def "deve marcar todos os dbchanges do grupo"() {
        given:
        def sql = mockSql()
        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: {
            [
                    "abc",
                    "65264543-test.dbchange.grupo",
                    "65564564-test.dbchange"
            ]
        }]
        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        def grupo = "grupo"

        when: "marcar todos os dbchanges do grupo"
        manager.markAll(grupo)

        then:
        mensagens.size() == 2
        mensagens[0] == "marking 1 file(s)"
        mensagens[1] == "65264543-test.dbchange.grupo marked as implemented"
    }

    def "se todos os arquivos já foram executados, não fazer nada quando rodar markAll"() {
        given:
        def sql = [execute: { instrucao -> [] }, rows: { instrucao -> RESULT }, close: {}]

        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["65564564-test.dbchange"] }]
        def manager = new DbChangeManager(sql: sql, directoryFile: directoryFile, logger: logger)

        when: "marcar todos os dbchanges"
        manager.markAll()

        then: "Informar que todos arquivos já estão marcados"
        mensagens.size() == 1
        mensagens[0] == "All files are already marked as implemented"
    }


    def "se não houver o diretorio 'dbchanges', deve criar"() {
        given:

        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter

        def manager = new DbChangeManager(logger: logger, path: '/tmp/bee')

        when: "criar um novo dbchange"
        manager.createDbChangeFile('new-file', null)

        then: "Deve gerar o novo arquivo"
        def dir = new File('/tmp/bee/dbchanges')
        true == dir.exists()
        true == dir.isDirectory()
        dir.list().size() == 1
    }

    @After
    void 'remove temporary files'() {
        new File('/tmp/bee').deleteDir()
    }


    private Sql mockSql() {
        def connection = Mock(Connection)
        connection.autoCommit() >> false
        def databaseMetaData = Mock(DatabaseMetaData)
        databaseMetaData.getDriverName >> "driver"
        connection.getMetaData() >> databaseMetaData
        def sql = Mock(Sql)
        sql.connection >> connection
        return sql
    }

    def mockDirectoryFile() {
        def directoryFile = [list: {
            [
                    "abc",
                    "65564564-test.dbchange",
                    "989898-test.dbchange"
            ]
        }]
        return directoryFile
    }

    def mockLogger() {
        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        return logger
    }

    private File getProperties(filePath) {
        def configUrl = RDBMSUtilTest.class.getResource(filePath)
        def configFile = new File(configUrl.toURI())
    }
}
