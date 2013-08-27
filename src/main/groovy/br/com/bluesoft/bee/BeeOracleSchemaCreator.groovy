package br.com.bluesoft.bee

import br.com.bluesoft.bee.model.Options;
import br.com.bluesoft.bee.service.BeeWriter;
import br.com.bluesoft.bee.importer.JsonImporter

class BeeOracleSchemaCreator extends BeeSchemaCreator {

	void createCoreData(def file, def schema, def dataFolderPath) {
		file.append("alter session set nls_date_format = 'yyyy-mm-dd';\n\n", 'utf-8')
		super.createCoreData(file, schema, dataFolderPath)
	}
}
