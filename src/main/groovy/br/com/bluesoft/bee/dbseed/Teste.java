package br.com.bluesoft.bee.dbseed;

public class Teste {
	
	public static void main(String[] args) {
		String texto = "Teste ' Teste ";
		texto = texto.replaceAll("'", "\\'");
		System.out.println(texto);
	}

}
