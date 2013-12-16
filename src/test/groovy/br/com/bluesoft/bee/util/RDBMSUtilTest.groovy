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
		Options options = new Options()
		options.configFile = getProperties("/oracleTest.properties")
		options.arguments[0] = "test"

		RDBMS oracleDatabaseType = RDBMSUtil.getRDBMS(options)
		assertNotNull(oracleDatabaseType)
		assertEquals(RDBMS.ORACLE, oracleDatabaseType);
	}

	@Test
	public void 'deve retornar o tipo Mysql'() {
		Options options = new Options()
		options.configFile = getProperties("/mySqlTest.properties")
		options.arguments[0] = "test"

		RDBMS oracleDatabaseType = RDBMSUtil.getRDBMS(options)
		assertNotNull(oracleDatabaseType)
		assertEquals(RDBMS.MYSQL, oracleDatabaseType);
	}
	
	@Test
	public void 'deve retornar o tipo Postgres'() {
		Options options = new Options()
		options.configFile = getProperties("/postgresTest.properties")
		options.arguments[0] = "test"

		RDBMS oracleDatabaseType = RDBMSUtil.getRDBMS(options)
		assertNotNull(oracleDatabaseType)
		assertEquals(RDBMS.POSTGRES, oracleDatabaseType);
	}

	@Test
	public void 'deve Lançar Exception se o banco não for suportado'() {
		Options options = new Options()
		options.configFile = getProperties("/test.properties")
		options.arguments[0] = "test"

		try {
			RDBMS oracleDatabaseType = RDBMSUtil.getRDBMS(options)
			fail()
		} catch (Exception e) {
			def mensagemDeErroEsperada = MessageFormat.format(RDBMSUtil.MENSAGEM_DE_ERRO_BANCO_NAO_SUPORTADO, "river")
			assertEquals(mensagemDeErroEsperada, e.getMessage())
		}
	}

	private File getProperties(filePath) {
		def configUrl = RDBMSUtilTest.class.getResource(filePath)
		def configFile = new File(configUrl.toURI())
	}
}
