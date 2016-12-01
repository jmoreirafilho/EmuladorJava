package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logs {
	private String local_do_log = "C:\\Sistemas\\EmuladorJava\\src\\main\\logs.txt";
	
	public Logs() {
		// Deleta arquivo, caso exista
		File arquivo = new File(this.local_do_log);
		arquivo.delete();
		
		// Cria o arquivo e usa para preencher
		try {
			FileWriter arq = new FileWriter(this.local_do_log);
			arq.write("-------------- LOGS DO EMULADOR --------------");
			arq.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Erro ao criar arquivo de logs!");
		}
	}
	
	public void add(String linha) {
		FileWriter arquivo;
		try {
			arquivo = new FileWriter(this.local_do_log);
			arquivo.write(linha);
			arquivo.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Erro ao gravar arquivo!");
		}
	}
}
