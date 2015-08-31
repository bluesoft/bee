package br.com.bluesoft.bee;

import org.junit.Test;

import spock.lang.Specification;

public class BeeVersionModuleTest extends Specification {

	@Test
	def "verifica se a função que retorna a URL com a última versão está retornando a URL com a versão correta"() {
		given:""
		def beeVersionLatest = BeeVersionModule.getLatestVersion()

		when:""
		String urlLatest = 'https://github.com/bluesoft/bee/releases/tag/' + beeVersionLatest

		then:""
		BeeVersionModule.getLatestVersionURL() == urlLatest
	}
	
	@Test
	def "verifica se a função que retorna a URL de download está retornando a URL com a versão correta"() {
		given:""
		def beeVersionLatest = BeeVersionModule.getLatestVersion()

		when:""
		String urlLatest = 'https://github.com/bluesoft/bee/releases/download/' + beeVersionLatest + '/bee-' + beeVersionLatest + '.zip'

		then:""
		BeeVersionModule.getLatestDownloadURL() == urlLatest
	}
}
