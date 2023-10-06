package br.com.bluesoft.bee.importer

import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals;

class JsonImporterIntegrationTest {

    JsonImporter importer

    @Before
    public void beforeClass() {

        File destTablesFolder = new File('/tmp/bee/tables')
        destTablesFolder.mkdirs()

        File destViewsFolder = new File('/tmp/bee/views')
        destViewsFolder.mkdirs()

        File destProceduressFolder = new File('/tmp/bee/procedures')
        destProceduressFolder.mkdirs()

        File destPackagesFolder = new File('/tmp/bee/packages')
        destPackagesFolder.mkdirs()

        File destTriggersFolder = new File('/tmp/bee/triggers')
        destTriggersFolder.mkdirs()

        File destUserTypesFolder = new File('/tmp/bee/usertypes')
        destUserTypesFolder.mkdirs()

        File srcTablesFolder = new File(this.getClass().getResource("/tables").getFile())
        srcTablesFolder.eachFile {
            new File(destTablesFolder, it.getName()) << it.getText()
        }

        File srcViewsFolder = new File(this.getClass().getResource('/views').getFile())
        srcViewsFolder.eachFile {
            new File(destViewsFolder, it.getName()) << it.getText()
        }

        File srcProceduresFolder = new File(this.getClass().getResource('/procedures').getFile())
        srcProceduresFolder.eachFile {
            new File(destProceduressFolder, it.getName()) << it.getText()
        }

        File srcPackagesFolder = new File(this.getClass().getResource('/packages').getFile())
        srcPackagesFolder.eachFile {
            new File(destPackagesFolder, it.getName()) << it.getText()
        }

        File srcTriggersFolder = new File(this.getClass().getResource('/triggers').getFile())
        srcTriggersFolder.eachFile {
            new File(destTriggersFolder, it.getName()) << it.getText()
        }

        File srcUserTypesFolder = new File(this.getClass().getResource('/usertypes').getFile())
        srcUserTypesFolder.eachFile {
            new File(destUserTypesFolder, it.getName()) << it.getText()
        }

        new File('/tmp/bee/sequences.bee') << new File(this.getClass().getResource('/sequences.bee').getFile()).getText()
        new File('/tmp/bee/rules.json') << new File(this.getClass().getResource('/rules.json').getFile()).getText()
        importer = new JsonImporter()
    }


    @Test
    public void "it should use set the default path if none was set"() {
        assertEquals importer.getPath(), '/tmp/bee'
    }

    @Test
    public void "it should use set the path"() {
        importer = new JsonImporter('/tmp/carneiro')
        assertEquals importer.getPath(), '/tmp/carneiro'
    }

    @Test
    public void "it should create a table object for each table metadata file"() {
        assertEquals 4, importer.importMetaData().getTables().size()
    }

    @Test
    public void "it should create a view object for each view metadata file"() {
        def views = importer.importMetaData().getViews()
        assertEquals 1, views.size()

        def view = views.get("menus_view")
        assertEquals "SELECT test\n" +
                     "FROM test\n" +
                     "WHERE test.abc = 1", view.text_oracle
        assertEquals 1, view.dependencies.size()
        assertEquals "menus_view_dependency", view.dependencies.get(0)
    }

    @Test
    public void "it should create a sequence for each element in the sequence metadata file"() {
        assertEquals 9, importer.importMetaData().getSequences().size()
    }

    @Test
    public void "it should create a procedure object for each procedure metadata file"() {
        assertEquals 1, importer.importMetaData().getProcedures().size()
    }

    @Test
    public void "it should create a package object for each package metadata file"() {
        assertEquals 1, importer.importMetaData().getPackages().size()
    }

    @Test
    public void "it should create a triiger object for each trigger metadata file"() {
        assertEquals 1, importer.importMetaData().getTriggers().size()
    }

    @After
    void 'remove temporary files'() {
        new File('/tmp/bee').deleteDir()
    }
}
