package br.com.bluesoft.bee.util

import groovy.sql.Sql;
import java.io.File;
import java.text.MessageFormat;

import org.junit.Test;
import org.mockito.Mock;

import br.com.bluesoft.bee.model.Options;
import static org.junit.Assert.*;

class RDBMSUtilTest {

	@Mock
	public Sql sql

	@Test
	public void 'deve retornar o tipo Oracle'() {
		def configFile = getProperties("/oracleTest.properties")
		def clientName = "test"

		RDBMS oracleDatabaseType = RDBMSUtil.getRDBMS(configFile, clientName)
		assertNotNull(oracleDatabaseType)
		assertEquals(RDBMS.ORACLE, oracleDatabaseType)
	}

	@Test
	public void 'deve retornar o tipo Mysql'() {
		def configFile = getProperties("/mySqlTest.properties")
		def clientName = "test"

		RDBMS oracleDatabaseType = RDBMSUtil.getRDBMS(configFile, clientName)
		assertNotNull(oracleDatabaseType)
		assertEquals(RDBMS.MYSQL, oracleDatabaseType)
	}
	
	@Test
	public void 'deve retornar o tipo Postgres'() {
		def configFile = getProperties("/postgresTest.properties")
		def clientName = "test"

		RDBMS oracleDatabaseType = RDBMSUtil.getRDBMS(configFile, clientName)
		assertNotNull(oracleDatabaseType)
		assertEquals(RDBMS.POSTGRES, oracleDatabaseType)
	}

	@Test
	public void 'deve retornar o tipo Redshift'() {
		def configFile = getProperties("/redshiftTest.properties")
		def clientName = "test"

		RDBMS oracleDatabaseType = RDBMSUtil.getRDBMS(configFile, clientName)
		assertNotNull(oracleDatabaseType)
		assertEquals(RDBMS.REDSHIFT, oracleDatabaseType)
	}

	@Test
	public void 'deve Lançar Exception se o banco não for suportado'() {
		def configFile = getProperties("/test.properties")
		def clientName = "test"

		try {
			RDBMS oracleDatabaseType = RDBMSUtil.getRDBMS(configFile, clientName)
			fail()
		} catch (Exception e) {
			def mensagemDeErroEsperada = MessageFormat.format(RDBMSUtil.MENSAGEM_DE_ERRO_BANCO_NAO_SUPORTADO, "river")
			assertEquals(mensagemDeErroEsperada, e.getMessage())
		}
	}
	
	@Test
	public void 'deve retornar o tipo do banco passando o objeto Options'() {
		Options options = new Options()
		options.configFile = getProperties("/mySqlTest.properties")
		options.arguments[0] = "test"
		
		RDBMS oracleDatabaseType = RDBMSUtil.getRDBMS(options)
		
		assertEquals(RDBMS.MYSQL, oracleDatabaseType)
	}

	
	@Test
	public void 'deve retornar o selected database do mysql'() {
		Options options = new Options()
		options.configFile = getProperties("/mySqlTest.properties")
		options.arguments[0] = "test"
		
		def database = RDBMSUtil.getMySqlDatabaseName(options)
		
		assertEquals("test", database)
	}
	
	private File getProperties(filePath) {
		def configUrl = RDBMSUtilTest.class.getResource(filePath)
		def configFile = new File(configUrl.toURI())
	}
}
