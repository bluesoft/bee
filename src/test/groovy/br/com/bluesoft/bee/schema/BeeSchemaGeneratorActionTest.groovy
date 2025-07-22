package br.com.bluesoft.bee.schema;

import br.com.bluesoft.bee.database.reader.DatabaseReader
import br.com.bluesoft.bee.importer.Importer;
import br.com.bluesoft.bee.model.Options;
import br.com.bluesoft.bee.model.Schema;
import br.com.bluesoft.bee.model.View
import br.com.bluesoft.bee.service.BeeWriter
import groovy.sql.Sql
import org.junit.After;
import org.junit.Before;
import org.junit.Test

import java.nio.file.Files

import static org.junit.Assert.assertEquals
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when;

public class BeeSchemaGeneratorActionTest {

    private BeeSchemaGeneratorAction action;
    private Schema schemaOld;
    private Schema schemaNew;

    @Before
    public void setUp() {
        schemaOld = new Schema()
        schemaNew = new Schema()

        def importer = mock(Importer)
        when(importer.importMetaData()).thenReturn(schemaOld)

        action = new BeeSchemaGeneratorAction() {
            @Override
            Sql getDatabaseConnection(Object clientName) {
                return mock(Sql)
            }

            @Override
            DatabaseReader getDatabaseReader(Sql sql) {
                def reader = mock(DatabaseReader)
                when(reader.getSchema(any())).thenReturn(schemaNew)
                return reader
            }
        }
        action.importer = importer
        action.out = mock(BeeWriter)

        Options.instance.configFile = new File(getClass().getResource("/oracleTest.properties").toURI())
        Options.instance.dataDir = Files.createTempDirectory("test").toFile()
        Options.instance.arguments = ["test"]
        action.options = Options.instance
    }

    @After
    void after() {
        Options.instance.dataDir.delete()
    }

    @Test
    public void applyListWithViews() {
        // Create views for schemaOld
        View oldView1 = new View();
        oldView1.setName("view1");
        oldView1.setText_oracle("old oracle text 1");
        List<String> oldDeps1 = Arrays.asList("old_dep1", "old_dep2");
        oldView1.setDependencies_oracle(oldDeps1);
        oldView1.setText_postgres("old postgres text 1");
        List<String> oldPgDeps1 = Arrays.asList("old_pg_dep1", "old_pg_dep2");
        oldView1.setDependencies_postgres(oldPgDeps1);
        oldView1.setText_mysql("old mysql text 1");
        oldView1.setText_redshift("old redshift text 1");

        View oldView2 = new View();
        oldView2.setName("view2");
        oldView2.setText_oracle("old oracle text 2");
        List<String> oldDeps2 = Arrays.asList("old_dep3", "old_dep4");
        oldView2.setDependencies_oracle(oldDeps2);
        oldView2.setText_postgres("old postgres text 2");
        List<String> oldPgDeps2 = Arrays.asList("old_pg_dep3", "old_pg_dep4");
        oldView2.setDependencies_postgres(oldPgDeps2);
        oldView2.setText_mysql("old mysql text 2");
        oldView2.setText_redshift("old redshift text 2");

        // Create views for schemaNew
        View newView1 = new View();
        newView1.setName("view1");
        newView1.setText_oracle("new oracle text 1");
        List<String> newDeps1 = Arrays.asList("new_dep1", "new_dep2");
        newView1.setDependencies_oracle(newDeps1);
        newView1.setText_postgres("new postgres text 1");
        List<String> newPgDeps1 = Arrays.asList("new_pg_dep1", "new_pg_dep2");
        newView1.setDependencies_postgres(newPgDeps1);
        newView1.setText_mysql("new mysql text 1");
        newView1.setText_redshift("new redshift text 1");

        View newView2 = new View();
        newView2.setName("view2");
        // Leave some properties null to test the fallback logic
        newView2.setText_oracle(null);
        newView2.setDependencies_oracle(null);
        newView2.setText_postgres("new postgres text 2");
        newView2.setDependencies_postgres(null);
        newView2.setText_mysql(null);
        newView2.setText_redshift("new redshift text 2");

        // Add views to schemas
        schemaOld.getViews().put("view1", oldView1);
        schemaOld.getViews().put("view2", oldView2);
        schemaNew.getViews().put("view1", newView1);
        schemaNew.getViews().put("view2", newView2);

        // When
        action.run();

        // Verify that properties were correctly copied
        // For view1, all properties should be from newView1
        assertEquals("new oracle text 1", schemaNew.getViews().get("view1").getText_oracle());
        assertEquals(newDeps1, schemaNew.getViews().get("view1").getDependencies_oracle());
        assertEquals("new postgres text 1", schemaNew.getViews().get("view1").getText_postgres());
        assertEquals(newPgDeps1, schemaNew.getViews().get("view1").getDependencies_postgres());
        assertEquals("new mysql text 1", schemaNew.getViews().get("view1").getText_mysql());
        assertEquals("new redshift text 1", schemaNew.getViews().get("view1").getText_redshift());

        // For view2, null properties in newView2 should be replaced with values from oldView2
        assertEquals("old oracle text 2", schemaNew.getViews().get("view2").getText_oracle());
        assertEquals(oldDeps2, schemaNew.getViews().get("view2").getDependencies_oracle());
        assertEquals("new postgres text 2", schemaNew.getViews().get("view2").getText_postgres());
        assertEquals(oldPgDeps2, schemaNew.getViews().get("view2").getDependencies_postgres());
        assertEquals("old mysql text 2", schemaNew.getViews().get("view2").getText_mysql());
        assertEquals("new redshift text 2", schemaNew.getViews().get("view2").getText_redshift());
    }
}
