package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.util.RDBMSUtilTest
import spock.lang.Specification;

class DbchangeQueryDialectHelperTest extends Specification {
	
	def "deve retornar query de criacao da tabela dbchanges especifica para MySQL" () {
		given:
		def configFile = getProperties("/mySqlTest.properties")

		when: "obter comando de criacao da tabela dbchanges"
		def query = getCreateQuery(configFile, "test")

		then: "deve retornar query especifica para o dialeto do mysql"
		query == DbchangeQueryDialectHelper.CREATE_TABLE_DBCHANGES_MYSQL
	}


	def "deve retornar query de criacao da tabela dbchanges especifica para Oracle" () {
		given:
		def configFile = getProperties("/oracleTest.properties")

		when: "obter comando de criacao da tabela dbchanges"
		def query = getCreateQuery(configFile, "test")

		then: "deve retornar query especifica para o dialeto do oracle"
		query == DbchangeQueryDialectHelper.CREATE_TABLE_DBCHANGES_ORACLE
	}


	def "deve retornar query de criacao da tabela dbchanges especifica para PostgreSQL" () {
		given:
		def configFile = getProperties("/postgresTest.properties")

		when: "obter comando de criacao da tabela dbchanges"
		def query = getCreateQuery(configFile, "test")

		then: "deve retornar query especifica para o dialeto do postgres"
		query == DbchangeQueryDialectHelper.CREATE_TABLE_DBCHANGES_POSTGRES
	}
	
	def "deve retornar query de insert na tabela dbchanges especifica para MySQL" () {
		given:
		def configFile = getProperties("/mySqlTest.properties")

		when: "obter comando de criacao da tabela dbchanges"
		def query = getInsertQuery(configFile, "test")

		then: "deve retornar query especifica para o dialeto do mysql"
		query == DbchangeQueryDialectHelper.INSERT_INTO_DBCHANGES_MYSQL
	}
	
	def "deve retornar query de insert na tabela dbchanges especifica para Oracle" () {
		given:
		def configFile = getProperties("/oracleTest.properties")

		when: "obter comando de criacao da tabela dbchanges"
		def query = getInsertQuery(configFile, "test")

		then: "deve retornar query especifica para o dialeto do oracle"
		query == DbchangeQueryDialectHelper.INSERT_INTO_DBCHANGES_ORACLE
	}
	
	def "deve retornar query de insert na tabela dbchanges especifica para PostgreSQL" () {
		given:
		def configFile = getProperties("/postgresTest.properties")

		when: "obter comando de criacao da tabela dbchanges"
		def query = getInsertQuery(configFile, "test")

		then: "deve retornar query especifica para o dialeto do postgres"
		query == DbchangeQueryDialectHelper.INSERT_INTO_DBCHANGES_POSTGRES
	}
	
	def "deve retornar query de delete na tabela dbchanges especifica para MySQL" () {
		given:
		def configFile = getProperties("/mySqlTest.properties")

		when: "obter comando de exclusao de registros  da tabela dbchanges"
		def query = getDeleteQuery(configFile, "test")

		then: "deve retornar query especifica para o dialeto do mysql"
		query == DbchangeQueryDialectHelper.DELETE_FROM_DBCHANGES_MYSQL
	}
	
	def "deve retornar query de delete na tabela dbchanges especifica para Oracle" () {
		given:
		def configFile = getProperties("/oracleTest.properties")

		when: "obter comando de exclusao de registros da tabela dbchanges"
		def query = getDeleteQuery(configFile, "test")

		then: "deve retornar query especifica para o dialeto do oracle"
		query == DbchangeQueryDialectHelper.DELETE_FROM_DBCHANGES_ORACLE
	}
	
	def "deve retornar query de delete na tabela dbchanges especifica para PostgreSQL" () {
		given:
		def configFile = getProperties("/postgresTest.properties")

		when: "obter comando de exclusao de registros  da tabela dbchanges"
		def query = getDeleteQuery(configFile, "test")

		then: "deve retornar query especifica para o dialeto do postgres"
		query == DbchangeQueryDialectHelper.DELETE_FROM_DBCHANGES_POSTGRES
	}

	private getCreateQuery(File configFile, String clientName) {
		def query = new DbchangeQueryDialectHelper().getCreateTableDbchangesQuery(configFile, clientName)
		return query
	}
	
	private getInsertQuery(File configFile, String clientName) {
		def query = new DbchangeQueryDialectHelper().getInsertIntoDbchangesQuery(configFile, clientName)
		return query
	}
	
	private getDeleteQuery(File configFile, String clientName) {
		def query = new DbchangeQueryDialectHelper().getDeleteFromDbchangesQuery(configFile, clientName)
		return query
	}




	private File getProperties(filePath) {
		def configUrl = RDBMSUtilTest.class.getResource(filePath)
		def configFile = new File(configUrl.toURI())
	}
}
