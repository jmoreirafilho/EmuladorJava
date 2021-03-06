package main;

import java.util.Scanner;

public class inicio {

	private static Scanner scanner;

	public static void main(String[] args) {
		int tamanho = 0, largura = 0, clock = 0, cachePct = 0, politica = 0;

		scanner = new Scanner(System.in);
		
		System.out.println("Tamanho da mem�ria RAM (valor divisivel por 4) em bytes: ");
		tamanho = scanner.nextInt();

		System.out.println("\nLargura do barramento em bits: ");
		largura = scanner.nextInt();

		System.out.println("\nClock: ");
		clock = scanner.nextInt();

		System.out.println("\nTamanho da mem�ria cache (percentual): ");
		cachePct = scanner.nextInt();

		int cacheTamanho = (int)((cachePct * tamanho) / 100);
		
		System.out.print("\nQual politica deseja utilizar?\n1 -> LRU\n2 -> LFU\n3 -> COOLDOWN\n -> ");
		politica = scanner.nextInt();
		
		System.out.println("\n\n\n");
		
		try {
			Modulo.iniciaModulos(tamanho, largura, clock, cacheTamanho, politica);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}