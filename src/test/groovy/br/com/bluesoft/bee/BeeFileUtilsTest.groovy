package br.com.bluesoft.bee;

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
}
