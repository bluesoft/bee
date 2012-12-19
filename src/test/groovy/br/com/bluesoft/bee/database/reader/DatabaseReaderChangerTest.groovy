package br.com.bluesoft.bee.database.reader


import static org.junit.Assert.*;

import java.io.File;
import java.text.MessageFormat;

import groovy.mock.interceptor.MockFor;
import groovy.sql.Sql;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import br.com.bluesoft.bee.model.Options;


class DatabaseReaderChangerTest {

	@Mock
	public Sql sql

	@Test
	public void 'deve retornar um OracleDatabaseReader'() {
		Options options = new Options()
		options.configFile = getProperties("/oracleTest.properties")
		options.arguments[0] = "test"

		def oracleDatabaseReader = DatabaseReaderChanger.getDatabaseReader(options, sql)
		assertNotNull(oracleDatabaseReader)
		assertTrue(oracleDatabaseReader instanceof OracleDatabaseReader)
	}

	@Test
	public void 'deve retornar um MysqlDatabaseReader'() {
		Options options = new Options()
		options.configFile = getProperties("/mySqlTest.properties")
		options.arguments[0] = "test"

		def mySqlDatabaseReader = DatabaseReaderChanger.getDatabaseReader(options, sql)
		assertNotNull(mySqlDatabaseReader)
		assertTrue(mySqlDatabaseReader instanceof MySqlDatabaseReader)
	}
	
	@Test
	public void 'deve Lançar Exception se o banco não for suportado'() {
		Options options = new Options()
		options.configFile = getProperties("/test.properties")
		options.arguments[0] = "test"

		try {
			DatabaseReaderChanger.getDatabaseReader(options, sql)
			fail()
		} catch (Exception e) {
			def mensagemDeErroEsperada = MessageFormat.format(DatabaseReaderChanger.MENSAGEM_DE_ERRO_BANCO_NAO_SUPORTADO, "river")
			assertEquals(mensagemDeErroEsperada, e.getMessage())
		}
		
	}


	private File getProperties(filePath) {
		def configUrl = DatabaseReaderChangerTest.class.getResource(filePath)
		def configFile = new File(configUrl.toURI())
	}
}
