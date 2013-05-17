package br.com.bluesoft.bee.dbseed

import java.io.File
import br.com.bluesoft.bee.service.BeeWriter
import groovy.lang.Grab
import groovy.lang.GrabConfig
import groovy.mock.interceptor.MockFor
import groovy.sql.Sql;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DbSeedManagerTest {

	def sql

	@Before
	public void setUp() {
		def data = [
			[ tipo_operacao_nota_fiscal_key: "1", descricao: "Entrada", sinal: "1"],
			[ tipo_operacao_nota_fiscal_key: "2", descricao: "Saída", sinal: "-1"]
		]

		sql = [eachRow: { query, closure ->
				data.each(closure)
			} ]
	}

	@Test
	def void "deve ler criar dados das tabelas core"(){
		def mensagens = []
		def logger = [ "log": { msg -> mensagens << msg } ] as BeeWriter

		def clientName = 'pedreira'

		File directory = new File("beeFolderTest")
		directory.mkdirs()
		def mockDirectory = new MockFor(File)

		def manager = new DbSeedManager(sql: sql, clientName: clientName, path: directory, directoryFile: directory, logger: logger)
		manager.createCoreData()
	}
}
