package main;

import java.util.Scanner;

public class inicio {

	private static Scanner scanner;

	public static void main(String[] args) {
		int tamanho = 0, largura = 0, clock = 0;

		scanner = new Scanner(System.in);
		
		System.out.println("Tamanho da memória RAM (valor divisivel por 4) em bytes: ");
		tamanho = scanner.nextInt();

		System.out.println("\nLargura do barramento em bits: ");
		largura = scanner.nextInt();

		System.out.println("\nClock: ");
		clock = scanner.nextInt();

		try {
			Modulo.iniciaModulos(tamanho, largura, clock);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
