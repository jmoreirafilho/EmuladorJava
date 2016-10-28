package computer;

import main.Modulo;

public class Cpu implements Runnable {
	public int CI;
	
	private int posicao_do_barramento;
	
	private int registrador_a;
	private int registrador_b;
	private int registrador_c;
	private int registrador_d;
	
	@Override
	public void run() {
		while(true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			int posicao = 0;
			
			if (Modulo.barramento.fila_dado.size() > posicao) {
				System.out.println("");
				int[] instrucao = Modulo.barramento.fila_dado.get(posicao);
//				System.out.println("CPU: Processou: ("+posicao+") => "+instrucao[0]+", "+instrucao[1]+", "+instrucao[2]+", "+instrucao[3]);
				switch (instrucao[1]) {
				case 1:
					this.processaComandoAdd(instrucao);
					break;
//				case 2:
//					resultado = this.processaComandoMov(instrucao);
//					break;
//				case 3:
//					resultado = this.processaComandoImul(instrucao);
//					break;
//				case 4:
//					resultado = this.processaComandoInc(instrucao);
//					break;
					default:
						System.out.println("Não é um comando ADD");
						break;
				}

				Modulo.barramento.somaContador("dado", "L");
				posicao++;
			}
			
			// Se essa instrução tem um numero que esta na RAM, grava sinal de leitura no barramento de controle
			// Caso contrário, processa a instrução e grava o valor
				// Caso o valor deva ser gravado nos registradores, usa o defineRegistradorX
				// Caso o valor deva ser gravado na memoria, grava sinal de Controle para gravar
				// Caso o contrário, apenas printa o resultado.
			
			// Se tiver algum sinal de endereço 

		}
	}

	private void processaComandoAdd(int[] instrucao) {

		int valor1 = this.pegaValor(instrucao[2]);
		int valor2 = this.pegaValor(instrucao[3]);
		
		int resultado = valor1 + valor2;
		
		this.gravaValorNaMemoriaRam(resultado);
	}

	private void gravaValorNaMemoriaRam(int resultado) {
		int[] dado = {resultado};
		Modulo.barramento.adicionaFilaDado(this.posicao_do_barramento, dado);
	}

	private int pegaValor(int valor) {
		if (valor < -1 && valor > -6) {
			switch (valor) { // Registrador
				case -2: // A
					return this.pegaRegistradorA();
				case -3: // B
					return this.pegaRegistradorB();
				case -4: // C
					return this.pegaRegistradorC();
				case -5: // D
					return this.pegaRegistradorD();
			}
		} else if (valor <= -6) { // Posição de memória
			int posicao_do_valor = this.posicaoNaRam(valor);
			return this.buscaValorNaMemoriaRam(posicao_do_valor);
		} 
		// numero inteiro
		return valor;
	}
	
	
	/**
	 * Envia sinal de controle e fica parado esperando o dado retornado
	 * 
	 * @param posicao_do_valor posição da memória Ram
	 * @return valor na posição informada
	 */
	private int buscaValorNaMemoriaRam(int posicao_do_valor) {
		this.posicao_do_barramento = Modulo.barramento.contador_escrita_controle;

		// envia sinal de controle e espera
		int[] sinal = {3, 2, posicao_do_valor};
		Modulo.barramento.adicionaFilaControle(this.posicao_do_barramento, sinal);
		
		return 0;
		
//		while(true){
//			if(Modulo.barramento.fila_dado.size() > this.posicao_do_barramento && 
//					Modulo.barramento.fila_endereco.get(this.posicao_do_barramento)[0] == 3){
//				System.out.println("opaaa");
//				return Modulo.barramento.fila_dado.get(this.posicao_do_barramento)[0];
//			}
//		}
	}

	/**
	 * Retorna qual o indice da memória RAM esse valor se refere.
	 * 
	 * @param valor
	 * @return Posição, da RAM, que esse valor corresponde
	 */
	private int posicaoNaRam(int valor) {
		return (valor + 6) + (Modulo.memoria_ram.memoria.length / 2);
	}

	private void processaComandoMov(int[] instrucao) {
		// TODO Auto-generated method stub
		
	}

	private void processaComandoImul(int[] instrucao) {
		// TODO Auto-generated method stub
		
	}

	private void processaComandoInc(int[] instrucao) {
		// TODO Auto-generated method stub
		
	}

	public int pegaRegistradorA() {
		return registrador_a;
	}

	public void defineRegistradorA(int registrador_a) {
		this.registrador_a = registrador_a;
	}

	public int pegaRegistradorB() {
		return registrador_b;
	}

	public void defineRegistradorB(int registrador_b) {
		this.registrador_b = registrador_b;
	}

	public int pegaRegistradorC() {
		return registrador_c;
	}

	public void defineRegistradorC(int registrador_c) {
		this.registrador_c = registrador_c;
	}

	public int pegaRegistradorD() {
		return registrador_d;
	}

	public void defineRegistradorD(int registrador_d) {
		this.registrador_d = registrador_d;
	}

	public void avancaCi() {
		this.CI++;
	}

}
