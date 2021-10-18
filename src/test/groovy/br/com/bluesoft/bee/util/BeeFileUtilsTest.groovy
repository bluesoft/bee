package br.com.bluesoft.bee.util

import br.com.bluesoft.bee.util.BeeFileUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

public class BeeFileUtilsTest extends Specification {

    @Rule
    public TemporaryFolder temporaryFolder

    def "deve criar a pasta enviada como parâmetro"() {
        given: ""
        String tempFile = System.getProperty("java.io.tmpdir") + "/bee-tests"
        File dir = new File(tempFile)

        when: ""
        BeeFileUtils.createDir(dir)

        then: ""
        dir.exists() == true
        dir.deleteOnExit()
    }

    def "deve remover a pasta enviada como parâmetro"() {
        setup: ""
        def tempFile = temporaryFolder.newFile('test.txt')

        when: ""
        BeeFileUtils.removeDir(tempFile)

        then: ""
        tempFile.exists() == false
    }

    def testListBaseJars() {
        given: "Lista de arquivos para testar"
        def files = [
                [name: 'bee-1.75.jar'],
                [name: 'commons-cli-1.4.jar'],
                [name: 'groovy-2.5.8.jar'],
                [name: 'groovy-cli-commons-2.5.8.jar'],
                [name: 'groovy-sql-2.5.8.jar'],
                [name: 'jackson-core-lgpl-1.9.13.jar'],
                [name: 'jackson-mapper-lgpl-1.9.13.jar']
        ]
        def source = [
                files      : files,
                isDirectory: { true },
                eachFile   : { def c -> files.each(c) }
        ]

        when: "listing"
        def result = BeeFileUtils.listBaseJars(source)

        then: ""
        result == ['bee', 'commons-cli', 'groovy', 'groovy-cli-commons', 'groovy-sql', 'jackson-core-lgpl', 'jackson-mapper-lgpl']
    }

    def testRemoveOldJars() {
        given: ""
        Set files = [
                'bee-1.75.jar',
                'commons-cli-1.4.jar',
                'groovy-2.5.8.jar',
                'groovy-cli-commons-2.5.8.jar',
                'groovy-sql-2.5.8.jar',
                'jackson-core-lgpl-1.9.13.jar',
                'jackson-mapper-lgpl-1.9.13.jar',
                'postgresql-9.2-1003.jdbc4.jar'
        ]
        def list = ['bee', 'commons-cli', 'groovy', 'groovy-cli-commons', 'groovy-sql', 'jackson-core-lgpl', 'jackson-mapper-lgpl']
        files.each { temporaryFolder.newFile(it) }

        when: ""
        def result = BeeFileUtils.removeOldJars(temporaryFolder.getRoot(), list)

        then: ""
        temporaryFolder.getRoot().list() == ['postgresql-9.2-1003.jdbc4.jar']
    }
}
