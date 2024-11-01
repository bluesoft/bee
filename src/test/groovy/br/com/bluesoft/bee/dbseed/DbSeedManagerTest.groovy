package br.com.bluesoft.bee.dbseed

import br.com.bluesoft.bee.database.ConnectionInfo
import br.com.bluesoft.bee.dbchange.SQLFileParser
import br.com.bluesoft.bee.dbchange.UpDown
import br.com.bluesoft.bee.service.BeeWriter
import br.com.bluesoft.bee.util.QueryDialectHelper
import br.com.bluesoft.bee.util.RDBMSUtilTest
import groovy.sql.Sql
import spock.lang.Specification

import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.SQLException

class DbSeedManagerTest extends Specification {

    final static def RESULT = [
            [ARQUIVO_NOME: "65564564-test.dbseed"]
    ]

    def "deve retornar uma lista com os arquivos a serem executados e na ordem"() {
        given:
        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def sql = mockSql()
        sql.rows(_ as String) >> { instrucao -> RESULT }
        def directoryFile = [list: {
            [
                    "abc",
                    "65564564-test.dbseed",
                    "989899-test.dbseed",
                    "989898-test.dbseed"
            ]
        }]
        def manager = new DbSeedManager(sql: sql, directoryFile: directoryFile, logger: logger, configFile: getProperties("/oracleTest.properties"), clientName: "test")

        when: "listar dbseeds"
        def lista = manager.listar()

        then: "deve conter somente as instrucoes que nao foram executadas"
        lista.size() == 2

        lista == ["989898-test.dbseed", "989899-test.dbseed"]
    }

    def "deve retornar false quando a lista de arquivos ja executados for nula"() {
        given:
        def mensagens = []
        def directoryFile = [list: {
            [
                    "abc",
                    "65564564-test.dbseed",
                    "989898-test.dbseed"
            ]
        }]
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def manager = new DbSeedManager(directoryFile: directoryFile, logger: logger, configFile: Mock(File))
        ConnectionInfo.metaClass.static.createDatabaseConnection = { def a, def b -> return null }

        when: "listar dbseeds a lista do banco retorna null"
        def lista = manager.listar()

        then: "deve retornar null e conter a mensagem"
        lista == null
        mensagens.size() == 2
        mensagens[1] == DbSeedManager.MESSAGE_COULD_NOT_GET_CONNECTION
    }

    def "deve listar as instrucoes ja executadas no banco"() {
        given:
        def sql = mockSql()
        sql.rows(_ as String) >> { instrucao -> RESULT }
        def manager = new DbSeedManager(sql: sql, directoryFile: mockDirectoryFile(), logger: mockLogger(), configFile: getProperties("/oracleTest.properties"), clientName: "test")

        when: "listar instrucoes ja executadas"
        def listaBanco = manager.listarInstrucoesJaExecutadas()

        then:
        listaBanco == RESULT
    }

    def "deve listar os arquivos"() {
        given:
        def directoryFile = [list: { ["abc", "65564564-test.dbseed"] }]
        def manager = new DbSeedManager(directoryFile: directoryFile)

        when: "listar arquivos de dbseeds"
        def listaArquivos = manager.listarArquivos()

        then:
        listaArquivos == RESULT
    }

    def "deve criar a tabela dbseeds caso nao exista em bancos oracle"() {

        given:
        def manager = new DbSeedManager(directoryFile: mockDirectoryFile(), logger: mockLogger(), configFile: getProperties("/oracleTest.properties"), clientName: "test")
        def sql = mockSql()
        1 * sql.execute(DbSeedManager.SELECT_TABLE) >> { throw new SQLException() }

        when: "criar tabela caso nao exista"
        def retorno = manager.criarTabelaDbseedsSeNaoExistir(sql)

        then: "e retorna true"
        retorno == true
    }

    def "deve criar a tabela dbseeds caso nao exista em bancos mysql"() {

        given:
        def manager = new DbSeedManager(directoryFile: mockDirectoryFile(), logger: mockLogger(), configFile: getProperties("/mySqlTest.properties"), clientName: "test")
        def sql = mockSql()
        def createTableQuery = QueryDialectHelper.getCreateTableDbseedsQuery(getProperties("/mySqlTest.properties"), "test")

        1 * sql.execute(DbSeedManager.SELECT_TABLE) >> { throw new SQLException() }

        when: "criar tabela caso nao exista"
        def retorno = manager.criarTabelaDbseedsSeNaoExistir(sql)

        then: "e retorna true"
        retorno == true
    }

    def "deve criar a tabela dbseeds caso nao exista em bancos postgres"() {

        given:
        def sql = mockSql()
        def manager = new DbSeedManager(directoryFile: mockDirectoryFile(), logger: mockLogger(), configFile: getProperties("/postgresTest.properties"), clientName: "test")
        def createTableQuery = QueryDialectHelper.getCreateTableDbseedsQuery(getProperties("/postgresTest.properties"), "test")

        1 * sql.execute(DbSeedManager.SELECT_TABLE) >> { throw new SQLException() }

        when: "criar tabela caso nao exista"
        def retorno = manager.criarTabelaDbseedsSeNaoExistir(sql)

        then: "e retorna true"
        retorno == true
    }


    def "deve retornar false caso nao consiga criar a tabela dbseeds"() {

        given:
        def sql = mockSql()
        def logger = Mock(BeeWriter)
        def manager = new DbSeedManager(directoryFile: mockDirectoryFile(), logger: logger, configFile: getProperties("/postgresTest.properties"), clientName: "test")
        def createTableQuery = QueryDialectHelper.getCreateTableDbseedsQuery(getProperties("/postgresTest.properties"), "test")

        1 * sql.execute(DbSeedManager.SELECT_TABLE) >> { throw new SQLException() }
        1 * sql.execute(QueryDialectHelper.CREATE_TABLE_DBSEEDS_POSTGRES) >> { throw new SQLException() }

        when: "Ocorrer um erro ao criar a tabela"
        def retorno = manager.criarTabelaDbseedsSeNaoExistir(sql)

        then: "Retorna false e loga o erro"
        !retorno
    }

    def "deve retornar false caso nao consiga obter a conexao com o banco"() {

        given:
        def mensagens = []
        def sql = Mock(Sql)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def manager = new DbSeedManager(logger: logger)

        when: "passar a sql null"
        def retorno = manager.criarTabelaDbseedsSeNaoExistir(null)

        then: "Retorna false e loga o erro"
        retorno == false
        mensagens.size() == 1
        mensagens[0] == DbSeedManager.MESSAGE_COULD_NOT_GET_CONNECTION
    }

    def "deve inserir uma execucao de dbseed quando o parametro UpDown for igual a UP"() {
        given:
        def sql = mockSql()
        def arquivo = "989898-test.dbseed"
        def logger = Mock(BeeWriter)
        def manager = new DbSeedManager(directoryFile: mockDirectoryFile(), logger: logger, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        def createTableQuery = QueryDialectHelper.getInsertIntoDbseedsQuery(getProperties("/oracleTest.properties"), "test")

        when: "salvar execucao de dbseed"
        manager.salvarExecucao(sql, arquivo, UpDown.UP)

        then: "deve inserir execucao e commitar"
        1 * sql.execute(createTableQuery, _)
        2 * sql.commit()
    }

    def "deve excluir uma execucao de dbseed quando o parametro UpDown for igual a DOWN"() {
        given:
        def sql = mockSql()
        def arquivo = "989898-test.dbseed"
        def logger = Mock(BeeWriter)
        def manager = new DbSeedManager(directoryFile: mockDirectoryFile(), logger: logger, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        def deleteQuery = QueryDialectHelper.getDeleteFromDbseedsQuery(getProperties("/oracleTest.properties"), "test")

        2 * sql.commit()
        1 * sql.execute(deleteQuery, _)

        when: "excluir execucao de dbseed"
        manager.salvarExecucao(sql, arquivo, UpDown.DOWN)

        then: "deve excluir execucao e commitar"
    }


    def "deve parsear, executar e gravar varias dbseeds"() {

        given:
        def mensagens = []
        def sql = mockSql()
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def lista = [
                "989898-test.dbseed",
                "65564564-test.dbseed"
        ]
        def dbseed = [
                [header: "carneiro", up: ["z", "x"], down: []],
                [header: "ismels", up: ["w", "e"], down: []]
        ]

        2 * parser.parseFile(_) >>> dbseed
        2 * sql.rows(_, _) >> []

        def manager = new DbSeedManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarVariasDbSeeds(lista, UpDown.UP)

        then:
        resultado == true
        mensagens.size() == 12
        mensagens[0] == "Executing dbseed: 989898-test.dbseed -- carneiro"
        mensagens[4].startsWith("Time elapsed")
        mensagens[5].startsWith("Execution time:")
        mensagens[6] == "Executing dbseed: 65564564-test.dbseed -- ismels"
    }

    def "deve parsear, executar e gravar uma dbseed"() {

        given:
        def mensagens = []
        def sql = mockSql()
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbseed"
        def dbseed = [header: "carneiro", up: ["z", "x"], down: []]

        1 * parser.parseFile(_) >> dbseed
        1 * sql.rows(_, _) >> []

        def manager = new DbSeedManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbSeed(arquivo, UpDown.UP)

        then:
        resultado == true
        mensagens.size() == 6
        mensagens[0] == "Executing dbseed: 989898-test.dbseed -- carneiro"
        mensagens[4].startsWith("Time elapsed")
        mensagens[5].startsWith("Execution time:")
    }

    def "deve parsear, executar e gravar uma dbseed quando o parametro UpDown = DOWN"() {

        given:
        def mensagens = []
        def sql = mockSql()
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbseed"
        def dbseed = [header: "carneiro", up: [], down: ["z", "x"]]

        1 * parser.parseFile(_) >> dbseed
        1 * sql.rows(_, _) >> ["select bla"]

        def manager = new DbSeedManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbSeed(arquivo, UpDown.DOWN)

        then:
        resultado == true
        mensagens.size() == 6
        mensagens[0] == "Executing dbseed: 989898-test.dbseed -- carneiro"
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
        def arquivo = "989898-test.dbseed"
        def dbseed = [header: "carneiro", up: [], down: []]

        1 * parser.parseFile(_) >> dbseed

        def manager = new DbSeedManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbSeed(arquivo, UpDown.UP)

        then:
        resultado == true
        mensagens.size() == 2
        mensagens[0] == "Executing dbseed: 989898-test.dbseed -- carneiro"
        mensagens[1] == DbSeedManager.MESSAGE_NO_COMMANDS_IN_FILE
    }

    def "deve retornar false quando ocorrer um erro de sql"() {

        given:
        def mensagens = []
        def sql = Mock(Sql)
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbseed"
        def dbseed = [header: "carneiro", up: ["x"], down: []]

        1 * parser.parseFile(_) >> dbseed
        1 * sql.rows(_, _) >> []
        1 * sql.executeUpdate(_) >> { throw new SQLException("Erro") }

        def manager = new DbSeedManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        manager.metaClass.getFile = { def a, def b -> return [] }
        manager.metaClass.salvarExecucao = { def a, def b, def c -> assert false }

        when: "executar dbseed com erro"
        def resultado = manager.executarDbSeed(arquivo, UpDown.UP)

        then: "retorna false e nao pode salvar a execucao da dbseed"
        resultado == false
        mensagens.size() == 4
        mensagens[0] == "Executing dbseed: 989898-test.dbseed -- carneiro"
        mensagens[2] == "!!!Error: Erro"
    }

    def "deve retornar mensagem de erro quando a lista de instrucoes nao existir"() {

        given:
        def mensagens = []
        def sql = Mock(Sql)
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbseed"
        def dbseed = [header: "carneiro", up: null, down: []]

        1 * parser.parseFile(_) >> dbseed

        def manager = new DbSeedManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser)
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbSeed(arquivo, UpDown.UP)

        then:
        resultado == false
        mensagens.size() == 2
        mensagens[0] == "Executing dbseed: 989898-test.dbseed -- carneiro"
        mensagens[1] == DbSeedManager.MESSAGE_THERE_IS_NO_INSTRUCTIONS
    }

    def "deve retornar false quando nao existir um execucao anterior e parametro UpDown = DOWN"() {

        given:
        def mensagens = []
        def sql = Mock(Sql)
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbseed"
        def dbseed = [header: "carneiro", up: [], down: ["z", "x"]]

        1 * parser.parseFile(_) >> dbseed
        1 * sql.rows(_, _) >> []

        def manager = new DbSeedManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, force: true, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbSeed(arquivo, UpDown.DOWN)

        then:
        resultado == false
        mensagens.size() == 2
        mensagens[0] == "Executing dbseed: 989898-test.dbseed -- carneiro"
        mensagens[1] == DbSeedManager.MESSAGE_DBSEED_NOT_EXECUTED
    }

    def "deve retornar false quando ja existir um execucao anterior e parametro UpDown = UP"() {

        given:
        def mensagens = []
        def sql = Mock(Sql)
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbseed"
        def dbseed = [header: "carneiro", up: ["z", "x"], down: ["z", "x"]]

        1 * parser.parseFile(_) >> dbseed
        1 * sql.rows(_, _) >> ["xxx"]

        def manager = new DbSeedManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, force: false, configFile: getProperties("/oracleTest.properties"), clientName: "test")

        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbSeed(arquivo, UpDown.UP)

        then:
        resultado == false
        mensagens.size() == 2
        mensagens[0] == "Executing dbseed: 989898-test.dbseed -- carneiro"
        mensagens[1] == DbSeedManager.MESSAGE_DBSEED_ALREADY_EXECUTED
    }

    def "deve retornar true mesmo quando ja existir um execucao anterior caso seja forçado 'force=true' "() {

        given:
        def mensagens = []
        def sql = mockSql()
        def parser = Mock(SQLFileParser)
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "xyz"] }]
        def arquivo = "989898-test.dbseed"
        def dbseed = [header: "carneiro", up: ["z", "x"], down: ["z", "x"]]

        1 * parser.parseFile(_) >> dbseed
        1 * sql.rows(_, _) >> ["xxx"]

        def manager = new DbSeedManager(sql: sql, directoryFile: directoryFile, logger: logger, parser: parser, force: true, configFile: getProperties("/oracleTest.properties"), clientName: "test")
        manager.metaClass.getFile = { def a, def b -> return [] }

        when:
        def resultado = manager.executarDbSeed(arquivo, UpDown.UP)

        then:
        resultado == true
        mensagens.size() == 6
        mensagens[0] == "Executing dbseed: 989898-test.dbseed -- carneiro"
        mensagens[5].startsWith("Execution time:")
    }

    def "deve retornar false quando o nome do arquivo for inválido"() {

        given: "arquivo com nome errado"
        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def arquivo = "989898-test.dbchang"
        def manager = new DbSeedManager(logger: logger)

        when:
        def resultado = manager.executarDbSeed(arquivo, UpDown.UP)

        then: "Retorna mensagem de nome do arquivo invalido"
        resultado == false
        mensagens.size() == 1
        mensagens[0] == DbSeedManager.MESSAGE_INVALID_FILE_NAME
    }

    def "deve retornar false quando nao conseguir obter a conexao com o banco"() {

        given: "arquivo com nome do banco errado"
        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def arquivo = "989898-test.dbseed"
        def manager = new DbSeedManager(configFile: Mock(File), logger: logger)
        ConnectionInfo.metaClass.static.createDatabaseConnection = { def a, def b -> return null }

        when: "o metodo createDatabaseConnection retornar null"
        def resultado = manager.executarDbSeed(arquivo, UpDown.UP)

        then: "retorna false e a mensagem que nao conseguiu obter conexao"
        resultado == false
        mensagens.size() == 2
        mensagens[1] == DbSeedManager.MESSAGE_COULD_NOT_GET_CONNECTION
    }

    def "deve retornar o timestamp do nome de arquivo"() {

        given:
        def manager = new DbSeedManager()
        expect:
        manager.obterTimestamp(arquivo) == timestamp
        where:
        timestamp | arquivo
        "989898"  | "989898-test.dbseed"
        "132123"  | "132123-x.dbseed"
        "0"       | "789987-teste"
    }

    def "deve marcar todos os dbseeds"() {
        given:
        def sql = mockSql()
        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["abc", "65564564-test.dbseed"] }]
        def manager = new DbSeedManager(sql: sql, directoryFile: directoryFile, logger: logger, configFile: getProperties("/oracleTest.properties"), clientName: "test")

        when: "marcar todos os dbseeds"
        manager.markAll()

        then:
        mensagens.size() == 2
        mensagens[0] == "marking 1 file(s)"
        mensagens[1] == "65564564-test.dbseed marked as implemented"
    }

    def "se todos os arquivos já foram executados, não fazer nada quando rodar markAll"() {
        given:
        def sql = mockSql()
        sql.rows(_ as String) >> { instrucao -> RESULT }

        def mensagens = []
        def logger = ["log": { msg -> mensagens << msg }] as BeeWriter
        def directoryFile = [list: { ["65564564-test.dbseed"] }]
        def manager = new DbSeedManager(sql: sql, directoryFile: directoryFile, logger: logger, configFile: getProperties("/oracleTest.properties"), clientName: "test")

        when: "marcar todos os dbseeds"
        manager.markAll()

        then: "Informar que todos arquivos já estão marcados"
        mensagens.size() == 1
        mensagens[0] == "All files are already marked as implemented"
    }

    private Sql mockSql() {
        def connection = Mock(Connection)
        connection.autoCommit() >> false
        def databaseMetaData = Mock(DatabaseMetaData)
        databaseMetaData.getDriverName >> "driver"
        connection.getMetaData() >> databaseMetaData
        def sql = Mock(Sql)
        sql.connection >> connection
        sql.withTransaction(_) >> { args -> args[0].call(connection) }
        return sql
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

    def mockDirectoryFile() {
        def directoryFile = [list: {
            [
                    "abc",
                    "65564564-test.dbseed",
                    "989898-test.dbseed"
            ]
        }]
        return directoryFile
    }


}
