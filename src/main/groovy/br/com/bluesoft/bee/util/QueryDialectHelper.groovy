package br.com.bluesoft.bee.util


class QueryDialectHelper {
	static final def String CREATE_TABLE_DBCHANGES_MYSQL = "create table dbchanges(arquivo_timestamp bigint, arquivo_nome varchar(200) not null, data_execucao date, constraint pk_dbchanges primary key (arquivo_timestamp))"
	static final def String CREATE_TABLE_DBCHANGES_ORACLE = "create table dbchanges(arquivo_timestamp number(14), arquivo_nome varchar(200) not null, data_execucao date, constraint pk_dbchanges primary key (arquivo_timestamp))"
	static final def String CREATE_TABLE_DBCHANGES_POSTGRES = "create table dbchanges(arquivo_timestamp bigint, arquivo_nome varchar(200) not null, data_execucao date, constraint pk_dbchanges primary key (arquivo_timestamp))"

	static final def String INSERT_INTO_DBCHANGES_MYSQL = "insert into dbchanges(arquivo_nome, arquivo_timestamp, data_execucao) values (?, ?, current_timestamp)"
	static final def String INSERT_INTO_DBCHANGES_ORACLE = "insert into dbchanges(arquivo_nome, arquivo_timestamp, data_execucao) values (?, ?, current_timestamp)"
	static final def String INSERT_INTO_DBCHANGES_POSTGRES = "insert into dbchanges(arquivo_nome, arquivo_timestamp, data_execucao) values (?, ?::bigint, current_timestamp)"

	static final def String DELETE_FROM_DBCHANGES_MYSQL = "delete from dbchanges where arquivo_timestamp = ?"
	static final def String DELETE_FROM_DBCHANGES_ORACLE = "delete from dbchanges where arquivo_timestamp = ?"
	static final def String DELETE_FROM_DBCHANGES_POSTGRES = "delete from dbchanges where arquivo_timestamp = ?::bigint"
	
	static final def String SELECT_FROM_DBCHANGES_MYSQL = "select * from dbchanges where arquivo_timestamp = ?"
	static final def String SELECT_FROM_DBCHANGES_ORACLE = "select * from dbchanges where arquivo_timestamp = ?"
	static final def String SELECT_FROM_DBCHANGES_POSTGRES = "select * from dbchanges where arquivo_timestamp = ?::bigint"
	
	static final def String CREATE_TABLE_DBSEEDS_MYSQL = "create table dbseeds(arquivo_timestamp bigint, arquivo_nome varchar(200) not null, data_execucao date, constraint pk_dbseeds primary key (arquivo_timestamp))"
	static final def String CREATE_TABLE_DBSEEDS_ORACLE = "create table dbseeds(arquivo_timestamp number(14), arquivo_nome varchar(200) not null, data_execucao date, constraint pk_dbseeds primary key (arquivo_timestamp))"
	static final def String CREATE_TABLE_DBSEEDS_POSTGRES = "create table dbseeds(arquivo_timestamp bigint, arquivo_nome varchar(200) not null, data_execucao date, constraint pk_dbseeds primary key (arquivo_timestamp))"

	static final def String INSERT_INTO_DBSEEDS_MYSQL = "insert into dbseeds(arquivo_nome, arquivo_timestamp, data_execucao) values (?, ?, current_timestamp)"
	static final def String INSERT_INTO_DBSEEDS_ORACLE = "insert into dbseeds(arquivo_nome, arquivo_timestamp, data_execucao) values (?, ?, current_timestamp)"
	static final def String INSERT_INTO_DBSEEDS_POSTGRES = "insert into dbseeds(arquivo_nome, arquivo_timestamp, data_execucao) values (?, ?::bigint, current_timestamp)"

	static final def String DELETE_FROM_DBSEEDS_MYSQL = "delete from dbseeds where arquivo_timestamp = ?"
	static final def String DELETE_FROM_DBSEEDS_ORACLE = "delete from dbseeds where arquivo_timestamp = ?"
	static final def String DELETE_FROM_DBSEEDS_POSTGRES = "delete from dbseeds where arquivo_timestamp = ?::bigint"

	static final def String SELECT_FROM_DBSEEDS_MYSQL = "select * from dbseeds where arquivo_timestamp = ?"
	static final def String SELECT_FROM_DBSEEDS_ORACLE = "select * from dbseeds where arquivo_timestamp = ?"
	static final def String SELECT_FROM_DBSEEDS_POSTGRES = "select * from dbseeds where arquivo_timestamp = ?::bigint"
	
	static final def String LIST_DBSEEDS_MYSQL = "select arquivo_nome as ARQUIVO_NOME from dbseeds order by arquivo_timestamp desc"
	static final def String LIST_DBSEEDS_ORACLE = "select arquivo_nome as ARQUIVO_NOME from dbseeds order by arquivo_timestamp desc"
	static final def String LIST_DBSEEDS_POSTGRES = "select arquivo_nome as \"ARQUIVO_NOME\" from dbseeds order by arquivo_timestamp desc"


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
	
	def static getSelectFromDbchangesQuery(configFile, clientName) {
		def rdbms = RDBMSUtil.getRDBMS(configFile, clientName)
		switch (rdbms) {
			case RDBMS.MYSQL:
				return SELECT_FROM_DBCHANGES_MYSQL
				break;
			case RDBMS.ORACLE:
				return SELECT_FROM_DBCHANGES_ORACLE
				break;
			case RDBMS.POSTGRES:
				return SELECT_FROM_DBCHANGES_POSTGRES
				break;
		}
	}
	
	def static getCreateTableDbseedsQuery(configFile, clientName) {
		def rdbms = RDBMSUtil.getRDBMS(configFile, clientName)
		switch (rdbms) {
			case RDBMS.MYSQL:
				return CREATE_TABLE_DBSEEDS_MYSQL
				break;
			case RDBMS.ORACLE:
				return CREATE_TABLE_DBSEEDS_ORACLE
				break;
			case RDBMS.POSTGRES:
				return CREATE_TABLE_DBSEEDS_POSTGRES
				break;
		}
	}

	def static getInsertIntoDbseedsQuery(configFile, clientName) {
		def rdbms = RDBMSUtil.getRDBMS(configFile, clientName)
		switch (rdbms) {
			case RDBMS.MYSQL:
				return INSERT_INTO_DBSEEDS_MYSQL
				break;
			case RDBMS.ORACLE:
				return INSERT_INTO_DBSEEDS_ORACLE
				break;
			case RDBMS.POSTGRES:
				return INSERT_INTO_DBSEEDS_POSTGRES
				break;
		}
	}

	def static getDeleteFromDbseedsQuery(configFile, clientName) {
		def rdbms = RDBMSUtil.getRDBMS(configFile, clientName)
		switch (rdbms) {
			case RDBMS.MYSQL:
				return DELETE_FROM_DBSEEDS_MYSQL
				break;
			case RDBMS.ORACLE:
				return DELETE_FROM_DBSEEDS_ORACLE
				break;
			case RDBMS.POSTGRES:
				return DELETE_FROM_DBSEEDS_POSTGRES
				break;
		}
	}

	def static getSelectFromDbseedsQuery(configFile, clientName) {
		def rdbms = RDBMSUtil.getRDBMS(configFile, clientName)
		switch (rdbms) {
			case RDBMS.MYSQL:
				return SELECT_FROM_DBSEEDS_MYSQL
				break;
			case RDBMS.ORACLE:
				return SELECT_FROM_DBSEEDS_ORACLE
				break;
			case RDBMS.POSTGRES:
				return SELECT_FROM_DBSEEDS_POSTGRES
				break;
		}
	}
	
	def static listDbseedsQuery(configFile, clientName) {
		def rdbms = RDBMSUtil.getRDBMS(configFile, clientName)
		switch (rdbms) {
			case RDBMS.MYSQL:
				return LIST_DBSEEDS_MYSQL
				break;
			case RDBMS.ORACLE:
				return LIST_DBSEEDS_ORACLE
				break;
			case RDBMS.POSTGRES:
				return LIST_DBSEEDS_POSTGRES
				break;
		}
	}
}
