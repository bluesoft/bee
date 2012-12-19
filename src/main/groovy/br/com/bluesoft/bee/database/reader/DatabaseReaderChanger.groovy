package br.com.bluesoft.bee.database.reader

import java.text.MessageFormat;

import br.com.bluesoft.bee.model.Options;
import br.com.bluesoft.bee.util.PropertiesUtil;
import br.com.bluesoft.bee.util.StringUtil;
import groovy.sql.Sql


class DatabaseReaderChanger {

	public static final String MENSAGEM_DE_ERRO_BANCO_NAO_SUPORTADO = "Banco de dados {0} não suportado"

	public static DatabaseReader getDatabaseReader(Options options, Sql sql){
		def databaseReader
		def key = options.arguments[0]
		def config = PropertiesUtil.readDatabaseConfig(options.configFile, key)
		def databaseType = getDataBaseType(config)

		if (databaseType.equals("mysql")) {
			databaseReader = new MySqlDatabaseReader(sql)
		} else if(databaseType.equals("oracle")) {
			databaseReader = new OracleDatabaseReader(sql)
		} else {
			def mensagemDeErro = MessageFormat.format(MENSAGEM_DE_ERRO_BANCO_NAO_SUPORTADO, databaseType)
			throw new IllegalArgumentException(mensagemDeErro)
		}
		return databaseReader;
	}

	public static getDataBaseType(config) {
		def urlTirandoJdbc = config.url.substring(config.url.indexOf(":") + 1)
		def dataBaseType = urlTirandoJdbc.substring(0, urlTirandoJdbc.indexOf(":"))
	}
}
