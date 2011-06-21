package br.com.bluesoft.bee.util;

import org.junit.Test


public class PropertiesUtilTest {
	
	@Test
	void 'deve ler um arquivo de properties'() {
		def configUrl = PropertiesUtilTest.class.getResource("/test.properties")
		def configFile = new File(configUrl.toURI())
		def propertiesUtil = new PropertiesUtil()
		def dbConfig = propertiesUtil.readDatabaseConfig(configFile, "test")
		
		assert dbConfig != null
		assert ["url", "driver", "user", "password"]  == dbConfig.keySet() as List
		assert dbConfig.url == "jdbc:river:port:database" 
		assert dbConfig.driver == "com.test.Driver"
		assert dbConfig.user == "test"
		assert dbConfig.password == "test-pass"
	}
	
	@Test
	void 'retorna null quando nao houver a cofiguracao'() {
		def configUrl = PropertiesUtilTest.class.getResource("/test.properties")
		def configFile = new File(configUrl.toURI())
		def propertiesUtil = new PropertiesUtil()
		def dbConfig = propertiesUtil.readDatabaseConfig(configFile, "test2")
		
		assert dbConfig == null
	}
	
	@Test
	void 'retorna null quando faltar algum atributo da cofiguracao'() {
		def configUrl = PropertiesUtilTest.class.getResource("/test.properties")
		def configFile = new File(configUrl.toURI())
		def propertiesUtil = new PropertiesUtil()
		def dbConfig = propertiesUtil.readDatabaseConfig(configFile, "test3")
		
		assert dbConfig == null
	}

}
