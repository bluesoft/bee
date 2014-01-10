package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.util.RDBMS;
import br.com.bluesoft.bee.util.RDBMSUtil;

class DbchangeQueryDialectHelper {
	static final def String CREATE_TABLE_DBCHANGES_MYSQL = "create table dbchanges(arquivo_timestamp bigint, arquivo_nome varchar(200) not null, data_execucao date, constraint pk_dbchanges primary key (arquivo_timestamp))"
	static final def String CREATE_TABLE_DBCHANGES_ORACLE = "create table dbchanges(arquivo_timestamp number(14), arquivo_nome varchar(200) not null, data_execucao date, constraint pk_dbchanges primary key (arquivo_timestamp))"
	static final def String CREATE_TABLE_DBCHANGES_POSTGRES = 'create table dbchanges(arquivo_timestamp bigint, arquivo_nome varchar(200) not null, data_execucao date, constraint pk_dbchanges primary key (arquivo_timestamp))'

	static final def String INSERT_INTO_DBCHANGES_MYSQL = "insert into dbchanges(arquivo_nome, arquivo_timestamp, data_execucao) values (?, ?, current_timestamp)"
	static final def String INSERT_INTO_DBCHANGES_ORACLE = INSERT_INTO_DBCHANGES_ORACLE
	static final def String INSERT_INTO_DBCHANGES_POSTGRES = "insert into dbchanges(arquivo_nome, arquivo_timestamp, data_execucao) values (?, ?::bigint, current_timestamp)"

	static final def String DELETE_FROM_DBCHANGES_MYSQL = "delete from dbchanges where arquivo_timestamp = ?" 
	static final def String DELETE_FROM_DBCHANGES_ORACLE = DELETE_FROM_DBCHANGES_POSTGRES
	static final def String DELETE_FROM_DBCHANGES_POSTGRES = "delete from dbchanges where arquivo_timestamp = ?::bigint"


	def static getCreateTableDbchangesQuery(configFile, clientName) {
		def rdbms = RDBMSUtil.getRDBMS(configFile, clientName)
		switch (rdbms) {
			case RDBMS.MYSQL:
				return CREATE_TABLE_DBCHANGES_MYSQL
				break;
			case RDBMS.ORACLE:
				return CREATE_TABLE_DBCHANGES_ORACLE
				break;
			case RDBMS.POSTGRES:
				return CREATE_TABLE_DBCHANGES_POSTGRES
				break;
		}
	}

	def static getInsertIntoDbchangesQuery(configFile, clientName) {
		def rdbms = RDBMSUtil.getRDBMS(configFile, clientName)
		switch (rdbms) {
			case RDBMS.MYSQL:
				return INSERT_INTO_DBCHANGES_MYSQL
				break;
			case RDBMS.ORACLE:
				return INSERT_INTO_DBCHANGES_ORACLE
				break;
			case RDBMS.POSTGRES:
				return INSERT_INTO_DBCHANGES_POSTGRES
				break;
		}
	}

	def static getDeleteFromDbchangesQuery(configFile, clientName) {
		def rdbms = RDBMSUtil.getRDBMS(configFile, clientName)
		switch (rdbms) {
			case RDBMS.MYSQL:
				return DELETE_FROM_DBCHANGES_MYSQL
				break;
			case RDBMS.ORACLE:
				return DELETE_FROM_DBCHANGES_ORACLE
				break;
			case RDBMS.POSTGRES:
				return DELETE_FROM_DBCHANGES_POSTGRES
				break;
		}
	}
}
