package br.com.bluesoft.bee.database.reader

import java.text.MessageFormat;

import br.com.bluesoft.bee.model.Options;
import br.com.bluesoft.bee.util.RDBMS
import br.com.bluesoft.bee.util.RDBMSUtil;


class DatabaseReaderChanger {

	public static final String MENSAGEM_DE_ERRO_BANCO_NAO_SUPORTADO = "Banco de dados {0} não suportado"

	public static DatabaseReader getDatabaseReader(Options options, sql) {
		def databaseReader
		RDBMS banco = RDBMSUtil.getRDBMS(options)

		switch (banco) {
			case RDBMS.MYSQL:
				String databaseName = RDBMSUtil.getMySqlDatabaseName(options)
				databaseReader = new MySqlDatabaseReader(sql, databaseName)
				break;
			case RDBMS.ORACLE:
				databaseReader = new OracleDatabaseReader(sql);
				break;
			case RDBMS.POSTGRES:
				databaseReader = new PostgresDatabaseReader(sql);
				break;
			case RDBMS.REDSHIFT:
				databaseReader = new RedshiftDatabaseReader(sql);
				break;
			default:
				def mensagemDeErro = MessageFormat.format(MENSAGEM_DE_ERRO_BANCO_NAO_SUPORTADO, banco);
				throw new IllegalArgumentException(mensagemDeErro);
				break;
		}

		return databaseReader;
	}

}
