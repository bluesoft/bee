package br.com.bluesoft.bee.database.reader

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.bluesoft.bee.model.Options;
import br.com.bluesoft.bee.util.PropertiesUtil;
import br.com.bluesoft.bee.util.RDBMS
import br.com.bluesoft.bee.util.RDBMSUtil;
import br.com.bluesoft.bee.util.StringUtil;
import groovy.sql.Sql


class DatabaseReaderChanger {

	public static final String MENSAGEM_DE_ERRO_BANCO_NAO_SUPORTADO = "Banco de dados {0} não suportado"

	public static DatabaseReader getDatabaseReader(Options options, sql) {
		def databaseReader
		RDBMS banco = RDBMSUtil.getRDBMS(options)

		if (banco == RDBMS.MYSQL) {
			String databaseName = RDBMSUtil.getMySqlDatabaseName(options)
			databaseReader = new MySqlDatabaseReader(sql, databaseName)
		} else if(banco == RDBMS.ORACLE) {
			databaseReader = new OracleDatabaseReader(sql)
		} else {
			def mensagemDeErro = MessageFormat.format(MENSAGEM_DE_ERRO_BANCO_NAO_SUPORTADO, banco)
			throw new IllegalArgumentException(mensagemDeErro)
		}
		return databaseReader;
	}

}
