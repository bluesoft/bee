package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.util.QueryDialectHelper
import br.com.bluesoft.bee.util.RDBMSUtilTest
import spock.lang.Specification

class DbchangeQueryDialectHelperTest extends Specification {

    def "deve retornar query de criacao da tabela dbchanges especifica para MySQL"() {
        given:
        def configFile = getProperties("/mySqlTest.properties")

        when: "obter comando de criacao da tabela dbchanges"
        def query = getCreateQuery(configFile, "test")

        then: "deve retornar query especifica para o dialeto do mysql"
        query == "create table dbchanges(arquivo_timestamp bigint, arquivo_nome varchar(200) not null, data_execucao date, constraint pk_dbchanges primary key (arquivo_timestamp))"
    }


    def "deve retornar query de criacao da tabela dbchanges especifica para Oracle"() {
        given:
        def configFile = getProperties("/oracleTest.properties")

        when: "obter comando de criacao da tabela dbchanges"
        def query = getCreateQuery(configFile, "test")

        then: "deve retornar query especifica para o dialeto do oracle"
        query == "create table dbchanges(arquivo_timestamp number(14), arquivo_nome varchar(200) not null, data_execucao date, constraint pk_dbchanges primary key (arquivo_timestamp))"
    }


    def "deve retornar query de criacao da tabela dbchanges especifica para PostgreSQL"() {
        given:
        def configFile = getProperties("/postgresTest.properties")

        when: "obter comando de criacao da tabela dbchanges"
        def query = getCreateQuery(configFile, "test")

        then: "deve retornar query especifica para o dialeto do postgres"
        query == "create table dbchanges(arquivo_timestamp bigint, arquivo_nome varchar(200) not null, data_execucao date, constraint pk_dbchanges primary key (arquivo_timestamp))"
    }

    def "deve retornar query de insert na tabela dbchanges especifica para MySQL"() {
        given:
        def configFile = getProperties("/mySqlTest.properties")

        when: "obter comando de criacao da tabela dbchanges"
        def query = getInsertQuery(configFile, "test")

        then: "deve retornar query especifica para o dialeto do mysql"
        query == "insert into dbchanges(arquivo_nome, arquivo_timestamp, data_execucao) values (?, ?, current_timestamp)"
    }

    def "deve retornar query de insert na tabela dbchanges especifica para Oracle"() {
        given:
        def configFile = getProperties("/oracleTest.properties")

        when: "obter comando de criacao da tabela dbchanges"
        def query = getInsertQuery(configFile, "test")

        then: "deve retornar query especifica para o dialeto do oracle"
        query == "insert into dbchanges(arquivo_nome, arquivo_timestamp, data_execucao) values (?, ?, current_timestamp)"
    }

    def "deve retornar query de insert na tabela dbchanges especifica para PostgreSQL"() {
        given:
        def configFile = getProperties("/postgresTest.properties")

        when: "obter comando de criacao da tabela dbchanges"
        def query = getInsertQuery(configFile, "test")

        then: "deve retornar query especifica para o dialeto do postgres"
        query == "insert into dbchanges(arquivo_nome, arquivo_timestamp, data_execucao) values (?, ?::bigint, current_timestamp)"
    }

    def "deve retornar query de delete na tabela dbchanges especifica para MySQL"() {
        given:
        def configFile = getProperties("/mySqlTest.properties")

        when: "obter comando de exclusao de registros  da tabela dbchanges"
        def query = getDeleteQuery(configFile, "test")

        then: "deve retornar query especifica para o dialeto do mysql"
        query == "delete from dbchanges where arquivo_timestamp = ?"
    }

    def "deve retornar query de delete na tabela dbchanges especifica para Oracle"() {
        given:
        def configFile = getProperties("/oracleTest.properties")

        when: "obter comando de exclusao de registros da tabela dbchanges"
        def query = getDeleteQuery(configFile, "test")

        then: "deve retornar query especifica para o dialeto do oracle"
        query == "delete from dbchanges where arquivo_timestamp = ?"
    }

    def "deve retornar query de delete na tabela dbchanges especifica para PostgreSQL"() {
        given:
        def configFile = getProperties("/postgresTest.properties")

        when: "obter comando de exclusao de registros  da tabela dbchanges"
        def query = getDeleteQuery(configFile, "test")

        then: "deve retornar query especifica para o dialeto do postgres"
        query == "delete from dbchanges where arquivo_timestamp = ?::bigint"
    }

    def "deve retornar query de select na tabela dbchanges especifica para MySQL"() {
        given:
        def configFile = getProperties("/mySqlTest.properties")

        when: "obter comando de selecao de registros  da tabela dbchanges"
        def query = getSelectQuery(configFile, "test")

        then: "deve retornar query especifica para o dialeto do mysql"
        query == "select * from dbchanges where arquivo_timestamp = ?"
    }

    def "deve retornar query de select na tabela dbchanges especifica para Oracle"() {
        given:
        def configFile = getProperties("/oracleTest.properties")

        when: "obter comando de selecao de registros da tabela dbchanges"
        def query = getSelectQuery(configFile, "test")

        then: "deve retornar query especifica para o dialeto do oracle"
        query == "select * from dbchanges where arquivo_timestamp = ?"
    }

    def "deve retornar query de select na tabela dbchanges especifica para PostgreSQL"() {
        given:
        def configFile = getProperties("/postgresTest.properties")

        when: "obter comando de selecao de registros  da tabela dbchanges"
        def query = getSelectQuery(configFile, "test")

        then: "deve retornar query especifica para o dialeto do postgres"
        query == "select * from dbchanges where arquivo_timestamp = ?::bigint"
    }

    private getCreateQuery(File configFile, String clientName) {
        def query = new QueryDialectHelper().getCreateTableDbchangesQuery(configFile, clientName)
        return query
    }

    private getInsertQuery(File configFile, String clientName) {
        def query = new QueryDialectHelper().getInsertIntoDbchangesQuery(configFile, clientName)
        return query
    }

    private getDeleteQuery(File configFile, String clientName) {
        def query = new QueryDialectHelper().getDeleteFromDbchangesQuery(configFile, clientName)
        return query
    }

    private getSelectQuery(File configFile, String clientName) {
        def query = new QueryDialectHelper().getSelectFromDbchangesQuery(configFile, clientName)
        return query
    }


    private File getProperties(filePath) {
        def configUrl = RDBMSUtilTest.class.getResource(filePath)
        def configFile = new File(configUrl.toURI())
    }
}
