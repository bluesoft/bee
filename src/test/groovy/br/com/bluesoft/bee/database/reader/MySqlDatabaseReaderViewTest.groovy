package br.com.bluesoft.bee.database.reader

import static org.junit.Assert.*
import static org.mockito.Mockito.*

import org.junit.Before
import org.junit.Test


class MySqlDatabaseReaderViewTest {

	def reader

	@Before
	void 'set up'(){
		def views = [
			[view_name:'VIEW_MENU', text:'aaa'],
			[view_name:'VIEW_USERS', text:'bbb']
		]
		def sql = [ rows: { query, schema -> views  } ]
		reader = new MySqlDatabaseReader(sql, 'test')
	}

	@Test
	void 'it should fill the views'() {
		def sequences = reader.getViews(null)
		assertEquals 2, sequences.size()
	}

	@Test
	void 'it should fill the view name'() {
		def sequences = reader.getViews(null)
		assertEquals 'VIEW_MENU', sequences['VIEW_MENU'].name
		assertEquals 'VIEW_USERS', sequences['VIEW_USERS'].name
	}
}
