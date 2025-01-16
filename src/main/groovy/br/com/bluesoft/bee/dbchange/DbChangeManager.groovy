/*
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is
 * Bluesoft Consultoria em Informatica Ltda.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 */
package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.database.ConnectionInfo
import br.com.bluesoft.bee.service.BeeWriter
import br.com.bluesoft.bee.util.QueryDialectHelper
import br.com.bluesoft.bee.util.RDBMSUtil
import groovy.sql.Sql

import java.sql.SQLException

class DbChangeManager {

    private final static def FILENAME_REGEX = /^([0-9]+)\-(.+)\.dbchange((\.)(.+))?$/
    final static def SELECT_TABLE = "select * from dbchanges"
    final static def MESSAGE_THERE_IS_NO_INSTRUCTIONS = "!!!Error: There is no ::up/::down statement in dbchange file"
    final static def MESSAGE_NO_COMMANDS_IN_FILE = "There is no commands in the dbchange file"
    final static def MESSAGE_DBCHANGE_ALREADY_EXECUTED = "It was not possible to execute the instructions ::up, because this dbchange was already executed"
    final static def MESSAGE_DBCHANGE_NOT_EXECUTED = "It was not possible to execute the instructions ::down, because this dbchange was not executed"
    final static def MESSAGE_INVALID_FILE_NAME = "Invalid dbchange file name"
    final static def MESSAGE_COULD_NOT_GET_CONNECTION = "It was not possible to get the database connection"

    def sql
    def directoryFile
    def parser

    File configFile
    String path
    String clientName
    String fileName
    boolean force
    BeeWriter logger

    boolean executarVariasDbChanges(List<String> lista, UpDown upDown) {
        def result = true;

        for (def execucao : lista) {
            if (!executarDbChange(execucao, upDown)) {
                result = false;
                break
            }
        }
        return result
    }

    boolean executarDbChange(String arquivo, UpDown upDown) {

        if (obterTimestamp(arquivo) == "0") {
            logger.log(MESSAGE_INVALID_FILE_NAME)
            return false
        }

        Sql sql = getDatabaseConnection()
        if (sql == null) {
            logger.log(MESSAGE_COULD_NOT_GET_CONNECTION)
            return false
        }

        def directoryFile = getDirectoryFile()
        def file = getFile(directoryFile, arquivo)
        def parser = getParser()
        def dbchange = parser.parseFile(file)

        def msg = "Executing dbchange: ${arquivo}"
        if (dbchange.header != null) {
            msg += " -- ${dbchange.header}"
        }
        logger.log(msg)

        def listaDeInstrucoes = null
        if (upDown == UpDown.UP) {
            listaDeInstrucoes = dbchange.up
        } else {
            listaDeInstrucoes = dbchange.down
        }

        def resultado = true
        sql.withTransaction {
            criarTabelaDbchangesSeNaoExistir(sql)

            if (podeExecutar(arquivo, sql, upDown)) {

                def executor = new SQLExecutor(sql: sql, logger: logger)
                if (listaDeInstrucoes) {
                    resultado = executor.execute(listaDeInstrucoes)
                }

                def start = System.currentTimeMillis()
                if (resultado) {
                    salvarExecucao(sql, arquivo, upDown)
                }

                def end = System.currentTimeMillis()
                logger.log("Execution time: ${(end - start) / 1000} seconds")
            } else {
                if (upDown == UpDown.UP) {
                    logger.log(MESSAGE_DBCHANGE_ALREADY_EXECUTED)
                } else {
                    logger.log(MESSAGE_DBCHANGE_NOT_EXECUTED)
                }
                resultado = false
            }
        }

        return resultado
    }

    def salvarExecucao(def sql, def arquivo, def upDown) {
        boolean autocommit = sql.connection.getAutoCommit()

        if (!autocommit) {
            sql.commit()
        }

        if (!criarTabelaDbchangesSeNaoExistir(sql)) {
            return false
        }

        def timestamp = obterTimestamp(arquivo)

        if (upDown == UpDown.UP) {
            def insertQuery = QueryDialectHelper.getInsertIntoDbchangesQuery(configFile, clientName)
            sql.execute(insertQuery, [arquivo, timestamp])
        } else {
            def deleteQuery = QueryDialectHelper.getDeleteFromDbchangesQuery(configFile, clientName)
            sql.execute(deleteQuery, [timestamp])
        }

        if (!autocommit) {
            sql.commit()
        }
    }

    def podeExecutar(def arquivo, def sql, def upDown) {
        def timestamp = obterTimestamp(arquivo)
        def selectQuery = QueryDialectHelper.getSelectFromDbchangesQuery(configFile, clientName)
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

    List<String> listar(String group) {
        def listaBanco = listarInstrucoesJaExecutadas()
        def listaArquivo = listarArquivos(group)

        if (listaArquivo == null || listaBanco == null) {
            return null
        }

        def listaParaExecutar = listaArquivo
        listaParaExecutar.removeAll(listaBanco)
        listaParaExecutar = listaParaExecutar.sort({ it.ARQUIVO_NOME })

        logger.log "Found ${listaParaExecutar.size()} file(s)"
        listaParaExecutar.each { logger.log "${it.ARQUIVO_NOME}" }
        return listaParaExecutar*.ARQUIVO_NOME
    }

    List<String> search() {
        def listFiles = listarArquivos()

        if (listFiles == null) {
            return null
        }

        listFiles = listFiles.findAll({ it.ARQUIVO_NOME.contains(fileName) ? it.ARQUIVO_NOME : null })

        logger.log "Found ${listFiles.size()} file(s)"
        listFiles.each { logger.log "${it.ARQUIVO_NOME}" }
        return listFiles*.ARQUIVO_NOME
    }

    def open() {
        def listFiles = listarArquivos()
        def env = System.getenv()
        def files = ""

        if (listFiles == null) {
            return null
        }

        listFiles = listFiles.findAll({ it.ARQUIVO_NOME.contains(fileName) ? it.ARQUIVO_NOME : null })
        def file = new File(getDirectoryFile(), listFiles[0].ARQUIVO_NOME)
        println file
        if (env['EDITOR']) {
            println "Opening with editor ${env['EDITOR']}"
            def cmd = [env['EDITOR'], file.path]
            new ProcessBuilder(env['EDITOR'], file.path).start()
        }

    }

    def listarArquivos(def group) {
        def directoryFile = getDirectoryFile()
        def filenames = directoryFile.list().findAll {
            return it ==~ FILENAME_REGEX && (!group || it.endsWith(".${group}"))
        }

        def result = []
        filenames.each {
            result << [ARQUIVO_NOME: it]
        }
        return result
    }

    def listarInstrucoesJaExecutadas() {
        def sql = getDatabaseConnection()

        def result = null
        if (criarTabelaDbchangesSeNaoExistir(sql)) {
            result = sql.rows('select arquivo_nome as "ARQUIVO_NOME" from dbchanges order by arquivo_timestamp desc')
        }
        return result
    }

    def criarTabelaDbchangesSeNaoExistir(def sql) {
        def tabelaDbchangesFoiCriada = false
        if (sql == null) {
            logger.log(MESSAGE_COULD_NOT_GET_CONNECTION)
            return false
        }

        try {
            sql.withTransaction {
                sql.execute(SELECT_TABLE)
                tabelaDbchangesFoiCriada = true
            }
        } catch (SQLException ex) {
            def createTableQuery = QueryDialectHelper.getCreateTableDbchangesQuery(configFile, clientName)
            try {
                sql.execute(createTableQuery)
                tabelaDbchangesFoiCriada = true
            } catch (SQLException e) {
                logger.log("!!!Erro: Nao foi possivel criar a tabela dbchanges")
                return false
            }
        }
        return tabelaDbchangesFoiCriada
    }

    def createDbChangeFile(description, group) {
        def filename = "${String.valueOf(System.currentTimeMillis())}-${description}.dbchange"
        if (group) {
            filename += ".${group}"
        }
        def file = new File(getDirectoryFile(), filename)

        def str = """-- ${description}
${group ? "-- group: ${group}" : ""}

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

    def markAll(def group) {
        def sql = getDatabaseConnection()
        def listaBanco = listarInstrucoesJaExecutadas()
        def listaArquivo = listarArquivos(group)

        def listaParaExecutar = (listaArquivo - listaBanco)
        listaParaExecutar = listaParaExecutar.sort({ it.ARQUIVO_NOME })

        if (listaParaExecutar.size() > 0) {
            logger.log "marking ${listaParaExecutar.size()} file(s)"
            String insertQuery = QueryDialectHelper.getInsertIntoDbchangesQuery(configFile, clientName)
            sql.withTransaction {
                listaParaExecutar.each {
                    String arquivo_nome = "${it.ARQUIVO_NOME}"
                    def timestamp = obterTimestamp("${it.ARQUIVO_NOME}")
                    sql.executeUpdate(insertQuery, [arquivo_nome, timestamp])
                    logger.log "${it.ARQUIVO_NOME} marked as implemented"
                }
            }
        } else {
            logger.log "All files are already marked as implemented"
        }
    }

    def unmark(def arquivo) {
        def sql = getDatabaseConnection()
        salvarExecucao(sql, arquivo, UpDown.DOWN)
    }

    def obterTimestamp(def arquivo) {
        if (!(arquivo ==~ FILENAME_REGEX)) {
            return "0"
        }

        def matcher = (arquivo =~ FILENAME_REGEX)
        def timestamp = matcher[0][1]
        return timestamp
    }

    def getFile(def directoryFile, def arquivo) {
        return new File(directoryFile, arquivo)
    }

    def getParser() {
        if (parser) {
            return parser
        }
        return new SQLFileParser(rdbms: RDBMSUtil.getRDBMS(configFile, clientName))
    }

    def getDirectoryFile() {
        def dir = this.directoryFile
        if (this.directoryFile == null) {
            dir = new File(path, "dbchanges")

            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdirs()
            }
        }

        return dir
    }

    def getDatabaseConnection() {
        if (sql == null) {
            sql = ConnectionInfo.createDatabaseConnection(configFile.absoluteFile, clientName)
        }
        return sql
    }
}
