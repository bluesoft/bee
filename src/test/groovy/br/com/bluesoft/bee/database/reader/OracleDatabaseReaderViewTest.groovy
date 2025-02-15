package br.com.bluesoft.bee.database.reader

import groovy.sql.Sql
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

import static org.junit.Assert.assertEquals
import static org.mockito.Mockito.when

public class OracleDatabaseReaderViewTest {

    OracleDatabaseReader reader

    @Before
    void 'set up'() {
        def views = [
                [view_name: 'VIEW_MENU', text: 'aaa'],
                [view_name: 'VIEW_USERS', text: 'bbb']
        ]
        def dependencies = [
                [name: 'VIEW_MENU', referenced_name: 'VIEW_USERS']
        ]
        def sql = Mockito.mock(Sql)
        when(sql.rows(OracleDatabaseReader.VIEWS_QUERY)).thenReturn(views)
        when(sql.rows(OracleDatabaseReader.VIEW_DEPENDENCIES)).thenReturn(dependencies)
        reader = new OracleDatabaseReader(sql)
    }

    @Test
    void 'it should fill the views'() {
        def sequences = reader.getViews(null)
        assertEquals 2, sequences.size()
    }

    @Test
    void 'it should fill the view attributes'() {
        def views = reader.getViews(null)
        assertEquals 'view_menu', views['view_menu'].name
        assertEquals 'aaa', views['view_menu'].text_oracle
        assertEquals 'view_users', views['view_users'].name
        assertEquals 'bbb', views['view_users'].text_oracle
        assertEquals(['view_users'], views['view_menu'].dependencies)
    }
}
