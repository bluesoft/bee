package br.com.bluesoft.bee.dbseed

import br.com.bluesoft.bee.database.ConnectionInfo
import br.com.bluesoft.bee.database.reader.TableDataReader
import br.com.bluesoft.bee.dbchange.SQLExecutor
import br.com.bluesoft.bee.dbchange.SQLFileParser
import br.com.bluesoft.bee.dbchange.UpDown
import br.com.bluesoft.bee.importer.JsonImporter
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.service.BeeWriter
import br.com.bluesoft.bee.util.CsvUtil
import br.com.bluesoft.bee.util.QueryDialectHelper

import java.sql.SQLException

class DbSeedManager {

    final static def SELECT_TABLE = "select * from dbseeds"
    final static def CREATE_TABLE_ORACLE = "create table dbseeds(arquivo_timestamp number(14), arquivo_nome varchar(60) not null, data_execucao date, constraint pk_dbseeds primary key (arquivo_timestamp))"
    final static def MESSAGE_THERE_IS_NO_INSTRUCTIONS = "!!!Error: There is no ::up/::down statement in dbseed file"
    final static def MESSAGE_NO_COMMANDS_IN_FILE = "There is no commands in the dbseed file"
    final static def MESSAGE_DBSEED_ALREADY_EXECUTED = "It was not possible to execute the instructions ::up, because this dbseed was already executed"
    final static def MESSAGE_DBSEED_NOT_EXECUTED = "It was not possible to execute the instructions ::down, because this dbseed was not executed"
    final static def MESSAGE_INVALID_FILE_NAME = "Invalid dbseed file name"
    final static def MESSAGE_COULD_NOT_GET_CONNECTION = "It was not possible to get the database connection"

    def sql
    def directoryFile
    def parser

    File configFile
    String path
    String clientName
    boolean force
    private BeeWriter logger
    private Schema schema

    boolean executarVariasDbSeeds(List<String> lista, UpDown upDown) {
        def result = true;

        for (def execucao : lista) {
            if (!executarDbSeed(execucao, upDown)) {
                result = false;
                break
            }
        }
        return result
    }

    boolean executarDbSeed(String arquivo, UpDown upDown) {

        if (obterTimestamp(arquivo) == "0") {
            logger.log(MESSAGE_INVALID_FILE_NAME)
            return false
        }

        def sql = getDatabaseConnection()
        if (sql == null) {
            logger.log(MESSAGE_COULD_NOT_GET_CONNECTION)
            return false
        }

        def directoryFile = getDirectoryFile()
        def file = getFile(directoryFile, arquivo)
        def parser = getParser()
        def dbseed = parser.parseFile(file)

        def msg = "Executing dbseed: ${arquivo}"
        if (dbseed.header != null) {
            msg += " -- ${dbseed.header}"
        }
        logger.log(msg)

        def listaDeInstrucoes = null
        if (upDown == UpDown.UP) {
            listaDeInstrucoes = dbseed.up
        } else {
            listaDeInstrucoes = dbseed.down
        }

        if (listaDeInstrucoes == null) {
            logger.log MESSAGE_THERE_IS_NO_INSTRUCTIONS
            return false
        }

        if (listaDeInstrucoes.size() == 0) {
            logger.log MESSAGE_NO_COMMANDS_IN_FILE
            salvarExecucao(sql, arquivo, upDown)
            return true
        }

        def resultado
        if (podeExecutar(arquivo, sql, upDown)) {

            def executor = new SQLExecutor(sql: sql, logger: logger)
            resultado = executor.execute(listaDeInstrucoes)

            def start = System.currentTimeMillis()
            if (resultado) {
                salvarExecucao(sql, arquivo, upDown)
            }

            def end = System.currentTimeMillis()
            logger.log("Execution time: ${(end - start) / 1000} seconds")
        } else {
            if (upDown == UpDown.UP) {
                logger.log(MESSAGE_DBSEED_ALREADY_EXECUTED)
            } else {
                logger.log(MESSAGE_DBSEED_NOT_EXECUTED)
            }
            resultado = false
        }

        return resultado
    }

    def salvarExecucao(def sql, def arquivo, def upDown) {
        boolean autocommit = sql.connection.getAutoCommit()

        if (!autocommit) {
            sql.commit()
        }

        if (!criarTabelaDbseedsSeNaoExistir(sql)) {
            return false
        }

        def timestamp = obterTimestamp(arquivo)

        if (upDown == UpDown.UP) {
            def insertQuery = QueryDialectHelper.getInsertIntoDbseedsQuery(configFile, clientName)
            sql.execute(insertQuery, [arquivo, timestamp])
        } else {
            def deleteQuery = QueryDialectHelper.getDeleteFromDbseedsQuery(configFile, clientName)
            sql.execute(deleteQuery, [timestamp])
        }

        if (!autocommit) {
            sql.commit()
        }
    }

    def podeExecutar(def arquivo, def sql, def upDown) {

        def timestamp = obterTimestamp(arquivo)
        def selectQuery = QueryDialectHelper.getSelectFromDbseedsQuery(configFile, clientName)
        def rows = sql.rows(selectQuery, [timestamp])

        if (upDown == UpDown.UP) {
            if (force) {
                return true
            }
            return rows.size() == 0
        } else {
            return rows.size() > 0
        }
    }

    List<String> listar() {
        def listaBanco = listarInstrucoesJaExecutadas()
        def listaArquivo = listarArquivos()

        if (listaArquivo == null || listaBanco == null) {
            return null
        }

        def listaParaExecutar = (listaArquivo - listaBanco)
        listaParaExecutar = listaParaExecutar.sort({ it.ARQUIVO_NOME })

        logger.log "Found ${listaParaExecutar.size()} file(s)"
        listaParaExecutar.each { logger.log "${it.ARQUIVO_NOME}" }
        return listaParaExecutar*.ARQUIVO_NOME
    }

    def listarArquivos() {
        def directoryFile = getDirectoryFile()
        def filenames = directoryFile.list().findAll { it ==~ /^[0-9]+\-.+\.dbseed$/ }

        def result = []
        filenames.each {
            result << [ARQUIVO_NOME: it]
        }

        return result
    }

    def listarInstrucoesJaExecutadas() {
        def sql = getDatabaseConnection()

        def result = null
        if (criarTabelaDbseedsSeNaoExistir(sql)) {
            def listQuery = QueryDialectHelper.listDbseedsQuery(configFile, clientName)
            result = sql.rows(listQuery)
            sql.close()
        }
        return result
    }

    def criarTabelaDbseedsSeNaoExistir(def sql) {
        def tabelaDbseedsFoiCriada = false
        if (sql == null) {
            logger.log(MESSAGE_COULD_NOT_GET_CONNECTION)
            return false
        }

        try {
            sql.withTransaction() {
                sql.execute(SELECT_TABLE)
                tabelaDbseedsFoiCriada = true
            }
        } catch (SQLException ex) {
            def createTableQuery = QueryDialectHelper.getCreateTableDbseedsQuery(configFile, clientName)
            try {
                sql.withTransaction() {
                    sql.execute(createTableQuery)
                    tabelaDbseedsFoiCriada = true
                }
            } catch (SQLException e) {
                logger.log("!!!Erro: Nao foi possivel criar a tabela dbseeds")
                return false
            }
        }
        return tabelaDbseedsFoiCriada
    }

    def createDbSeedFile(description) {
        def filename = String.valueOf(System.currentTimeMillis()) + "-" + description + ".dbseed";
        def file = new File(getDirectoryFile(), filename)

        def str = """-- ${description}

::up

::down

"""

        file << str
        logger.log("Criado " + file.path)

        def env = System.getenv()
        if (env['EDITOR']) {
            println "Opening editor ${env['EDITOR']}"
            def cmd = [env['EDITOR'], file.path]
            new ProcessBuilder(env['EDITOR'], file.path).start()
        }
    }

    def mark(def arquivo) {
        def sql = getDatabaseConnection()
        if (podeExecutar(arquivo, sql, UpDown.UP)) {
            salvarExecucao(sql, arquivo, UpDown.UP)
        }
    }

    def markAll() {
        def sql = getDatabaseConnection()
        def listaBanco = listarInstrucoesJaExecutadas()
        def listaArquivo = listarArquivos()

        def listaParaExecutar = (listaArquivo - listaBanco)
        listaParaExecutar = listaParaExecutar.sort({ it.ARQUIVO_NOME })

        if (listaParaExecutar.size() > 0) {
            logger.log "marking ${listaParaExecutar.size()} file(s)"
            listaParaExecutar.each {
                def timestamp = obterTimestamp("${it.ARQUIVO_NOME}")
                String arquivo_nome = "${it.ARQUIVO_NOME}"
                def insertQuery = QueryDialectHelper.getInsertIntoDbseedsQuery(configFile, clientName)
                sql.execute(insertQuery, [arquivo_nome, timestamp])
                logger.log "${it.ARQUIVO_NOME} marked as implemented"
            }
        } else {
            logger.log "All files are already marked as implemented"
        }
    }

    def unmark(def arquivo) {
        def sql = getDatabaseConnection()
        salvarExecucao(sql, arquivo, UpDown.DOWN)
    }

    def generate(def tableName) {
        def sql = getDatabaseConnection()

        try {
            logger.log "Extracting the table data from ${tableName} ... "
            def schema = getSchema(path)
            def table = schema.tables[tableName]
            def data = new TableDataReader(sql).getData(table)

            def dir = getDirectoryFile()
            def filename = tableName.toLowerCase() + ".csv"

            def file = new File(dir, filename)
            file.delete()

            CsvUtil.write file, data
            return true
        } catch (e) {
            logger.log e.toString()
            throw new Exception("Error importing database metadata.", e)
        }
    }

    def obterTimestamp(def arquivo) {
        if (!(arquivo ==~ /^([0-9]+)\-(.+)\.dbseed$/)) {
            return "0"
        }

        def matcher = (arquivo =~ /^([0-9]+)\-(.+)\.dbseed$/)
        def timestamp = matcher[0][1]
        return timestamp
    }

    def getFile(def directoryFile, def arquivo) {
        return new File(directoryFile, arquivo)
    }

    private Schema getSchema(path) {
        if (schema == null) {
            schema = new JsonImporter(path).importMetaData()
        }
        schema
    }

    def getParser() {
        if (parser) {
            return parser
        }
        return new SQLFileParser()
    }

    def getDirectoryFile() {
        if (this.directoryFile != null) {
            return this.directoryFile
        }
        return new File(path, "dbseeds")
    }

    def getDatabaseConnection() {
        if (sql != null) {
            return sql
        }

        try {
            logger.log "Connecting to the database..."
            sql = ConnectionInfo.createDatabaseConnection(configFile.absoluteFile, clientName)
            return sql
        } catch (e) {
            throw new Exception("It was not possible to connect to the database.", e)
        }
    }
}
