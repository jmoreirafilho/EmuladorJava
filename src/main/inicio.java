package main;

import java.util.Scanner;

public class inicio {

	private static Scanner scanner;

	public static void main(String[] args) {
		int tamanho = 0, largura = 0, clock = 0, cachePct = 0, cacheTamanho = 0, politica = 0;

		scanner = new Scanner(System.in);
		
		System.out.println("Tamanho da memória RAM (valor divisivel por 4) em bytes: ");
		tamanho = scanner.nextInt();

		System.out.println("\nLargura do barramento em bits: ");
		largura = scanner.nextInt();

		System.out.println("\nClock: ");
		clock = scanner.nextInt();

		System.out.println("\nTamanho da memória cache (percentual): ");
		cachePct = scanner.nextInt();

		cacheTamanho = (cachePct / 100) * tamanho;

		System.out.print("\nQual politica deseja utilizar?\n1 -> FIFO\n2 -> LRU\n3 -> LFU\n -> ");
		politica = scanner.nextInt();
		
		System.out.println("\n\n\n");
		
		try {
			Modulo.iniciaModulos(tamanho, largura, clock, cacheTamanho, politica);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}