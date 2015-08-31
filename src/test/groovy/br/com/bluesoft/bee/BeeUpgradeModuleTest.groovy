package br.com.bluesoft.bee;

import spock.lang.Specification;
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito;

public class BeeUpgradeModuleTest extends Specification {

	@Test
	def "deve verificar que o bee está atualizado e retornar true"() {
		given:""
		GroovyMock(BeeVersionModule, global: true)
		BeeVersionModule.getCurrentVersion() >> "1.1"
		BeeVersionModule.getLatestVersion()  >> "1.1"

		when:""
		BeeUpgradeModule upgrade = new BeeUpgradeModule()
		def isLatest = upgrade.isLatestVersion()

		then:""
		isLatest == true
	}
	
	@Test
	def "deve verificar que o bee está desatualizado e retornar false"() {
		given:""
		GroovyMock(BeeVersionModule, global: true)
		BeeVersionModule.getCurrentVersion() >> "1.0"
		BeeVersionModule.getLatestVersion()  >> "1.1"

		when:""
		BeeUpgradeModule upgrade = new BeeUpgradeModule()
		def isLatest = upgrade.isLatestVersion()

		then:""
		isLatest == false
	}
	
	@Test
	def "deve pegar a última versão e ela deve ser igual à versão retornada no módulo BeeVersionModule"() {
		given:""
		GroovyMock(BeeVersionModule, global: true)
		def beeVersionLatest = BeeVersionModule.getLatestVersion()

		when:""
		BeeUpgradeModule upgrade = new BeeUpgradeModule()
		def beeUpgradeLatest = upgrade.getLatestVersion()

		then:""
		 beeUpgradeLatest == beeVersionLatest
	}
}
