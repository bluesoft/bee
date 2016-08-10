package br.com.bluesoft.bee

import br.com.bluesoft.bee.data.BeeDataValidatorAction
import org.junit.Before;
import org.junit.Test;

import br.com.bluesoft.bee.model.message.MessageLevel;


class BeeDataValidationActionTest {

	def validator
	
	@Before
	void 'setup'(){
		validator = new BeeDataValidatorAction()
	}
	
	@Test
	void deve_criar_mensagem_de_erro_quando_dado_do_csv_for_diferente_do_dado_do_banco_de_dados() {
		def sourceLine1 = ['267','IPVA A APROPRIAR','1','78','A']
		def destLine1 = ['267','IPVA A APROPRIAR','1','78','D']
		
		def source = [sourceLine1]
		def dest = [destLine1]
		def level = MessageLevel.WARNING
		def objectName =  'mapa_contabil'
		def message = 'This line was found in the database table [mapa_contabil] but was not found in the schema:'
		
		def messages = validator.validateTableFromSource(source, dest, level, objectName, message)
		
		assert messages?.size() == 1
		
		messages.each {
			assert it.message == 'This line was found in the database table [mapa_contabil] but was not found in the schema:[267, IPVA A APROPRIAR, 1, 78, A]'
			assert it.level == level
		}
	}
	
	@Test
	void deve_criar_mensagem_de_erro_quando_dado_do_banco_de_dados_for_diferente_do_dado_do_csv() {
		def sourceLine1 = ['267','IPVA A APROPRIAR','1','78','A']
		def destLine1 = ['267','IPVA A APROPRIAR','1','78','D']
		
		def source = [sourceLine1]
		def dest = [destLine1]
		def level = MessageLevel.ERROR
		def objectName =  'mapa_contabil'
		def message = 'This line was found in the schema [${objectName}] but was not found in the database:'
		
		def messages = validator.validateTableFromSource(dest, source, level, objectName, message)
		
		assert messages?.size() == 1
		
		
		
		messages.each {
			assert it.message == 'This line was found in the schema [${objectName}] but was not found in the database:[267, IPVA A APROPRIAR, 1, 78, D]'
			assert it.level == level
		}
	}

}
