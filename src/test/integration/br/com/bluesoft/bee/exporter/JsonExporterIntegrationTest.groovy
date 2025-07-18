package br.com.bluesoft.bee.exporter

import br.com.bluesoft.bee.database.reader.DatabaseReader
import br.com.bluesoft.bee.database.reader.OracleDatabaseReader
import br.com.bluesoft.bee.exporter.Exporter
import br.com.bluesoft.bee.exporter.BeeExporter
import groovy.sql.Sql
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue;

class JsonExporterIntegrationTest {

    Exporter exporter
    DatabaseReader databaseReader


    @Before
    void 'set up'() {
        def sql = Sql.newInstance("jdbc:oracle:thin:@server:1521:serverdb", "alfa", "alfa", "oracle.jdbc.driver.OracleDriver")
        this.databaseReader = new OracleDatabaseReader(sql)
    }

    @Test
    void 'it should set the path of the folder to create the files'() {
        exporter = new BeeExporter(databaseReader, '/tmp/carneiro')
        assertEquals '/tmp/carneiro', exporter.path
    }

    @Test
    void 'it should set the default path if no path is set'() {
        exporter = new BeeExporter(databaseReader)
        assertEquals '/tmp/bee', exporter.path
    }

    @Test
    void 'it should create the folder and subfolders if they do not exist yet'() {
        File folder = new File('/tmp/bee')
        exporter = new BeeExporter(databaseReader, folder.getPath())

        folder.deleteDir()
        assertFalse folder.exists()

        exporter.createPath()
        assertTrue folder.exists()
        assertTrue new File(folder, 'tables').exists()
        assertTrue new File(folder, 'views').exists()
    }

    @Test
    void itShouldCreateOneFileForEachTable() {
        exporter = new BeeExporter(databaseReader, '/tmp/bee')
        exporter.export()

        def files = new File('/tmp/bee/tables').list().toList()
        def tables = exporter.getSchema().getTables()

        tables.each {
            assertTrue files.contains(it.value.name + '.bee')
        }
    }

    @Test
    void itShouldCreateAFileWithAllTheSequences() {
        exporter = new BeeExporter(databaseReader, '/tmp/bee')
        exporter.export()

        def file = new File('/tmp/bee/sequences.bee')
        assertTrue file.exists()

        def linhas = file.readLines()
        def sequences = exporter.getSchema().getSequences()
        sequences.each { sequenceName, sequence ->
            assertNotNull linhas.find { String linha ->
                linha.contains(sequenceName)
            }
        }
    }

    @Test
    void itShouldCreateOneFileForEachView() {
        exporter = new BeeExporter(databaseReader, '/tmp/bee')
        exporter.export()

        def files = new File('/tmp/bee/views').list().toList()
        def views = exporter.getSchema().getViews()

        views.each {
            assertTrue files.contains(it.value.name + '.bee')
        }
    }

    @After
    void 'remove temporary files'() {
        new File('/tmp/bee').deleteDir()
    }
}
