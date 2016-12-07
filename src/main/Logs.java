package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Logs {
	private String local_do_log = "C:\\sistemas\\EmuladorJava\\src\\main\\";
//	private String local_do_log = "C:\\Users\\Airton\\workspace\\Emulador\\src\\main\\";
	
	public Logs(String nome_arquivo) {
		// Deleta arquivo, caso exista
		this.local_do_log += nome_arquivo;
		File arquivo = new File(this.local_do_log);
		arquivo.delete();
		
		// Cria o arquivo e usa para preencher
		try {
			FileWriter arq = new FileWriter(this.local_do_log);
			arq.write("-------------- LOGS DO EMULADOR --------------");
			arq.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Erro ao criar arquivo "+nome_arquivo+" de log!");
		}
	}
	
	public void add(String linha) {
		linha = "\n" + linha;
		try {
			Files.write(Paths.get(this.local_do_log), linha.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
