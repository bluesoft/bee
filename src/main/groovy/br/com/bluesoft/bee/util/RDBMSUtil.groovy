package br.com.bluesoft.bee.util

import br.com.bluesoft.bee.model.Options

import java.text.MessageFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

public class RDBMSUtil {

    public static final String MENSAGEM_DE_ERRO_BANCO_NAO_SUPORTADO = "Banco de dados {0} não suportado"

    public static RDBMS getRDBMS(Options options) {
        return getRDBMS(options.configFile, options.arguments[0])
    }

    public static RDBMS getRDBMS(configFile, clientName) {
        def config = PropertiesUtil.readDatabaseConfig(configFile, clientName)

        def databaseType = getDataBaseType(config)

        if (databaseType.equals("mysql")) {
            return RDBMS.MYSQL
        } else if (databaseType.equals("oracle")) {
            return RDBMS.ORACLE
        } else if (databaseType.equals("postgresql")) {
            return RDBMS.POSTGRES
        } else if (databaseType.equals("redshift")) {
            return RDBMS.REDSHIFT
        } else {
            def mensagemDeErro = MessageFormat.format(MENSAGEM_DE_ERRO_BANCO_NAO_SUPORTADO, databaseType)
            throw new IllegalArgumentException(mensagemDeErro)
        }
    }

    private static String getDataBaseType(config) {
        def urlTirandoJdbc = config.url.substring(config.url.indexOf(":") + 1)
        def dataBaseType = urlTirandoJdbc.substring(0, urlTirandoJdbc.indexOf(":"))
    }

    public static String getMySqlDatabaseName(Options options) {
        def key = options.arguments[0]
        def config = PropertiesUtil.readDatabaseConfig(options.configFile, key)
        String urlConnection = config.url
        Pattern pattern = Pattern.compile('([^/]+$)')
        Matcher matcher = pattern.matcher(urlConnection)
        String databaseName
        if (matcher.find()) {
            databaseName = matcher.group();
        }
        databaseName
    }

}
