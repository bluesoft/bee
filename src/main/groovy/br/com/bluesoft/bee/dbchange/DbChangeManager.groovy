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

import br.com.bluesoft.bee.service.BeeWriter
import java.sql.SQLException
import java.text.MessageFormat;
import java.util.List

import br.com.bluesoft.bee.database.ConnectionInfo
import br.com.bluesoft.bee.service.BeeWriter

class DbChangeManager {

	final static def SELECT_TABLE = "select * from dbchanges"
	final static def CREATE_TABLE_ORACLE = "create table dbchanges(arquivo_timestamp number(14), arquivo_nome varchar(60) not null, data_execucao date, constraint pk_dbchanges primary key (arquivo_timestamp))"
	final static def CREATE_TABLE = "create table dbchanges(arquivo_timestamp bigint, arquivo_nome varchar(60) not null, data_execucao date, constraint pk_dbchanges primary key (arquivo_timestamp))"
	final static def INSERT_INTO_DBCHANGES = "insert into dbchanges(arquivo_nome, arquivo_timestamp, data_execucao) values (?, ?, current_timestamp)"
	final static def DELETE_FROM_DBCHANGES = "delete from dbchanges where arquivo_timestamp = ?"
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
	boolean force
	BeeWriter logger

	boolean executarVariasDbChanges(List<String> lista, UpDown upDown) {
		def result = true;

		for(def execucao: lista) {
			if(!executarDbChange(execucao, upDown)) {
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

		def sql = getDatabaseConnection()
		if (sql == null) {
			logger.log(MESSAGE_COULD_NOT_GET_CONNECTION)
			return false
		}

		def directoryFile = getDirectoryFile()
		def file = getFile(directoryFile, arquivo)
		def parser = getParser()
		def dbchange = parser.parseFile(file)

		def msg = "Executing dbchange: ${arquivo}"
		if(dbchange.header != null) {
			msg += " -- ${dbchange.header}"
		}
		logger.log(msg)

		def listaDeInstrucoes = null
		if(upDown == UpDown.UP) {
			listaDeInstrucoes = dbchange.up
		} else {
			listaDeInstrucoes = dbchange.down
		}

		if(listaDeInstrucoes == null) {
			logger.log MESSAGE_THERE_IS_NO_INSTRUCTIONS
			return false
		}

		if(listaDeInstrucoes.size == 0) {
			logger.log MESSAGE_NO_COMMANDS_IN_FILE
			salvarExecucao(sql, arquivo, upDown)
			return true
		}

		def resultado
		if (podeExecutar(arquivo, sql, upDown)) {

			def executor = new SQLExecutor(sql: sql, logger: logger)
			resultado = executor.execute(listaDeInstrucoes)

			def start = System.currentTimeMillis()
			if(resultado) {
				salvarExecucao(sql, arquivo, upDown)
			}

			def end = System.currentTimeMillis()
			logger.log("Execution time: ${(end - start) / 1000} seconds")
		} else {
			if(upDown == UpDown.UP) {
				logger.log(MESSAGE_DBCHANGE_ALREADY_EXECUTED)
			} else {
				logger.log(MESSAGE_DBCHANGE_NOT_EXECUTED)
			}
			resultado = false
		}

		return resultado
	}

	def salvarExecucao(def sql, def arquivo, def upDown) {
		boolean autocommit = sql.connection.getAutoCommit()
		
		if (!autocommit)
			sql.commit()

		if (!criarTabelaDbchangesSeNaoExistir(sql)) {
			return false
		}

		def timestamp = obterTimestamp(arquivo)

		if(upDown == UpDown.UP) {
			sql.execute(INSERT_INTO_DBCHANGES, [arquivo, timestamp])
		} else {
			sql.execute(DELETE_FROM_DBCHANGES, [timestamp])
		}
		
		if (!autocommit)
			sql.commit()
	}

	def podeExecutar(def arquivo, def sql, def upDown) {

		def timestamp = obterTimestamp(arquivo)
		def rows = sql.rows("select * from dbchanges where arquivo_timestamp = ?", [timestamp])

		if (upDown == UpDown.UP) {
			if(force) {
				return true
			}
			return rows.size == 0
		} else {
			return rows.size > 0
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

		logger.log "Found ${listaParaExecutar.size} file(s)"
		listaParaExecutar.each { logger.log "${it.ARQUIVO_NOME}" }
		return listaParaExecutar*.ARQUIVO_NOME
	}

	def listarArquivos() {
		def directoryFile = getDirectoryFile()
		def filenames = directoryFile.list().findAll { it ==~ /^[0-9]+\-.+\.dbchange$/ }

		def result = []
		filenames.each {
			result << [ ARQUIVO_NOME: it ]
		}

		return result
	}

	def listarInstrucoesJaExecutadas() {
		def sql = getDatabaseConnection()

		def result = null
		if (criarTabelaDbchangesSeNaoExistir(sql)) {
			result = sql.rows("select arquivo_nome as ARQUIVO_NOME from dbchanges order by arquivo_timestamp desc")
			sql.close()
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
			sql.execute(SELECT_TABLE)
			tabelaDbchangesFoiCriada = true
		} catch (SQLException ex) {
			try {
				sql.execute(CREATE_TABLE_ORACLE)
				tabelaDbchangesFoiCriada = true
			} catch (SQLException e) {
				try {
					sql.execute(CREATE_TABLE)
					tabelaDbchangesFoiCriada = true
				} catch (Exception e2) {
					logger.log("!!!Erro: Nao foi possivel criar a tabela dbchanges")
					return false
				}
			}
		}
		return tabelaDbchangesFoiCriada
	}

	def createDbChangeFile(description) {
		def filename = String.valueOf(System.currentTimeMillis()) + "-" + description +".dbchange";
		def file = new File(getDirectoryFile(), filename)

		def str = """-- ${description}

::up

::down

"""

		file << str
		logger.log("Criado " + file.path)

		def env = System.getenv()
		if(env['EDITOR']) {
			println "Opening editor ${env['EDITOR']}"
			def cmd = [env['EDITOR'], file.path]
			new ProcessBuilder(env['EDITOR'], file.path).start()
		}
	}

	def mark(def arquivo) {
		def sql = getDatabaseConnection()
		if(podeExecutar(arquivo, sql, UpDown.UP))
			salvarExecucao(sql, arquivo, UpDown.UP)
	}
	
	def markAll() {
		def sql = getDatabaseConnection()
		def listaBanco = listarInstrucoesJaExecutadas()
		def listaArquivo = listarArquivos()
		
		def listaParaExecutar = (listaArquivo - listaBanco)
		listaParaExecutar = listaParaExecutar.sort({ it.ARQUIVO_NOME })

		if (listaParaExecutar.size > 0) {
			logger.log "marking ${listaParaExecutar.size} file(s)"
			listaParaExecutar.each {
			def timestamp = obterTimestamp("${it.ARQUIVO_NOME}")
			String arquivo_nome = "${it.ARQUIVO_NOME}"
			sql.execute(INSERT_INTO_DBCHANGES, [arquivo_nome, timestamp])
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

	def obterTimestamp(def arquivo) {
		if(!(arquivo ==~ /^([0-9]+)\-(.+)\.dbchange$/)) {
			return "0"
		}

		def matcher = (arquivo =~ /^([0-9]+)\-(.+)\.dbchange$/)
		def timestamp = matcher[0][1]
		return timestamp
	}

	def getFile(def directoryFile, def arquivo) {
		return new File(directoryFile, arquivo)
	}

	def getParser() {
		if(parser) {
			return parser
		}
		return new SQLFileParser()
	}

	def getDirectoryFile() {
		if(this.directoryFile != null) {
			return this.directoryFile
		}
		return new File(path, "dbchanges")
	}

	def getDatabaseConnection() {
		if(sql != null) {
			return sql
		}
		return ConnectionInfo.createDatabaseConnection(configFile.absoluteFile, clientName)
	}
}