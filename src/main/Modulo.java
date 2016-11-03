package main;

import computer.Barramento;
import computer.Cpu;
import computer.EntradaSaida;
import computer.MemoriaRam;

public class Modulo {
	
	public static Barramento barramento;
	public static EntradaSaida entrada_saida;
	public static MemoriaRam memoria_ram;
	public static Cpu cpu;

	public static void iniciaModulos(int tamanho, int largura, int frequencia) throws InterruptedException {
		barramento = new Barramento(largura, frequencia);
		memoria_ram = new MemoriaRam(tamanho);
		entrada_saida = new EntradaSaida();
		cpu = new Cpu();
		
		if(entrada_saida.compilaArquivo()){
			Thread e = new Thread(entrada_saida);
			e.start();
		}
		
		Thread r = new Thread(memoria_ram);
		r.start();
		
		Thread c = new Thread(cpu);
		c.start();
		
	}
}