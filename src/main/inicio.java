package main;

import java.util.Scanner;

//import java.util.Scanner;

public class inicio {

	public static void main(String[] args) {
		int tamanho = 0, largura = 0, frequencia = 0;

		Scanner scanner = new Scanner(System.in);

		System.out.print("Tamanho: ");
		tamanho = scanner.nextInt();

		System.out.print("\nLargura: ");
		largura = scanner.nextInt();

		System.out.print("\nFrequência: ");
		frequencia = scanner.nextInt();

		try {
			Modulo.iniciaModulos(tamanho, largura, frequencia);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
