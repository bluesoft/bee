package br.com.bluesoft.bee.database.reader

import groovy.sql.Sql
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito

import static org.junit.Assert.assertEquals
import static org.mockito.Mockito.when

class PostgresDatabaseReaderTableTest {

    PostgresDatabaseReader reader

    @Before
    void 'set up'() {
        def tableRows = [
                [table_name: 'resumo_crescimento_loja', temporary: 'N']
        ]
        def views = [
                [view_name: 'VIEW_MENU', text: 'aaa'],
                [view_name: 'VIEW_USERS', text: 'bbb']
        ]
        def dependencies = [
                [name: 'VIEW_MENU', referenced_name: 'VIEW_USERS']
        ]

        def columns = createColumns()
        def indexes = createIndexes()
        def indexesColumns = createIndexesColumns()


        def sql = Mockito.mock(Sql)
        when(sql.rows(PostgresDatabaseReader.TABLES_QUERY)).thenReturn(tableRows)
        when(sql.rows(PostgresDatabaseReader.TABLES_COLUMNS_QUERY)).thenReturn(columns)
        when(sql.rows(PostgresDatabaseReader.INDEXES_QUERY)).thenReturn(indexes)
        when(sql.rows(PostgresDatabaseReader.VIEWS_QUERY)).thenReturn(views)
        when(sql.rows(PostgresDatabaseReader.VIEW_DEPENDENCIES)).thenReturn(dependencies)

        reader = new PostgresDatabaseReader(sql)
    }

    @Test
    @Ignore
    void 'it should fill the tables'() {
        def tables = reader.getTables(null)
        assertEquals(2, tables.size())
    }

    @Test
    void 'it should fill the view attributes'() {
        def views = reader.getViews(null)
        assertEquals 'view_menu', views['view_menu'].name
        assertEquals 'aaa', views['view_menu'].text_postgres
        assertEquals 'view_users', views['view_users'].name
        assertEquals 'bbb', views['view_users'].text_postgres
        assertEquals(['view_users'], views['view_menu'].dependencies)
    }


    def createColumns() {
        def data = [table_name  : 'resumo_crescimento_loja',
                    column_name : 'data',
                    data_type   : "date",
                    nullable    : "YES",
                    data_size   : null,
                    data_scale  : null,
                    data_default: null
        ]

        def loja_key = [table_name  : 'resumo_crescimento_loja',
                        column_name : 'loja_key',
                        data_type   : "character",
                        nullable    : "YES",
                        data_size   : null,
                        data_scale  : null,
                        data_default: null
        ]
        return [data, loja_key]
    }

    def createIndexes() {
        def indexes = [
                [table_name: 'resumo_crescimento_loja', index_name: 'idx_resumo_crescimento_loja_data', uniqueness: 'f', index_type: 'n', indkey: '1'],
                [table_name: 'resumo_crescimento_loja', index_name: 'idx_resumo_crescimento_loja_loja', uniqueness: 'f', index_type: 'n', indkey: '2']
        ]
    }

    def createIndexesColumns() {

        def idx_resumo_crescimento_loja_data = [
                table_name : 'resumo_crescimento_loja',
                index_name : 'idx_resumo_crescimento_loja_data',
                column_name: 'data',
                attnum     : '1'
        ]

        def idx_resumo_crescimento_loja_loja = [
                table_name : 'resumo_crescimento_loja',
                index_name : 'idx_resumo_crescimento_loja_loja',
                column_name: 'loja_key',
                attnum     : '2'
        ]

        return [
                idx_resumo_crescimento_loja_data,
                idx_resumo_crescimento_loja_loja
        ]
    }
}
