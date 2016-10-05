package br.com.bluesoft.bee.util

import br.com.bluesoft.bee.upgrade.BeeVersionModule
import br.com.bluesoft.bee.util.BeeFileUtils;
import org.junit.Rule;
import org.junit.Test
import org.junit.rules.TemporaryFolder

import spock.lang.Specification;


public class BeeFileUtilsTest extends Specification {
	
	@Rule
	public TemporaryFolder temporaryFolder

	@Test
	def "deve criar a pasta enviada como parâmetro"() {
		given:""
		String tempFile = System.getProperty("java.io.tmpdir") + "/bee-tests"
		File dir = new File(tempFile)

		when:""
		BeeFileUtils.createDir(dir)

		then:""
		dir.exists() ==  true
		dir.deleteOnExit()
	}

	@Test
	def "deve remover a pasta enviada como parâmetro"() {
		setup:""
        def tempFile = temporaryFolder.newFile('test.txt')

		when:""
		BeeFileUtils.removeDir(tempFile)

		then:""
		tempFile.exists() ==  false
	}

	@Test
	def "deve remover todos os arquivos cuja versão seja diferente de getLatestVersion"() {
		setup:""
		GroovyMock(BeeVersionModule, global: true)
		BeeVersionModule.getLatestVersion()  >> "1.3"

		def tempFolder    = temporaryFolder.newFolder()
		def tempLibFolder = new File(tempFolder.getPath(), "lib")
		tempLibFolder.mkdirs()

		def bee1 = new File(tempLibFolder.getPath(), "bee-1.1.jar")
		def bee2 = new File(tempLibFolder.getPath(), "bee-1.2.jar")
		def bee3 = new File(tempLibFolder.getPath(), "bee-1.3.jar")

		bee1.createNewFile()
		bee2.createNewFile()
		bee3.createNewFile()

		when:""
		BeeFileUtils.removeOldBees(tempFolder)

		then:""
		bee1.exists() == false
		bee2.exists() == false
		bee3.exists() == true
	}
}
