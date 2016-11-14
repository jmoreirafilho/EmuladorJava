package computer;

import java.util.ArrayList;

import main.Modulo;

public class Cpu implements Runnable {
	public int CI = 0;
	private boolean pode_processar_instrucao = false;

	private int endereco_atual;
	private int[] instrucao_atual;
	ArrayList<Integer> valores = new ArrayList<Integer>(); // tam max = 3

	private int registrador_a;
	private int registrador_b;
	private int registrador_c;
	private int registrador_d;

	private final int NUMERO_DESSE_MODULO = 3;
	private int resultado;
	private boolean pode_gravar_resultado;
	private boolean grava_sinal_controle_mov;

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Processa uma instrução
			if (this.pode_processar_instrucao) {
				this.processaInstrucao();
			}
			
			if (this.pode_gravar_resultado) {
				this.gravaResultado();
			}
			
			if (this.grava_sinal_controle_mov) {
				int[] sinal_controle = {NUMERO_DESSE_MODULO, 2, 1, this.instrucao_atual[3]};
				Modulo.barramento.fila_controle.add(sinal_controle);
			}

		}
	}

	private void gravaResultado() {
		if (this.instrucao_atual[3] > -5) { // posição na memória ram
			int[] sinal_controle = {NUMERO_DESSE_MODULO, 2, 1, this.instrucao_atual[3]};
			Modulo.barramento.fila_controle.add(sinal_controle);
		} else if (this.instrucao_atual[3] > -6 && this.instrucao_atual[3] < -1) { // registrador
			switch (this.instrucao_atual[3]) {
			case -2: // A
				this.defineRegistradorA(this.resultado);
				break;
			case -3: // B
				this.defineRegistradorB(this.resultado);
				break;
			case -4: // C
				this.defineRegistradorC(this.resultado);
				break;
			case -5: // D
				this.defineRegistradorD(this.resultado);
				break;
			}
		}
	}

	private void processaInstrucao() {
		switch (this.instrucao_atual[2]) {
		case 1: // ADD
			this.processaAdd();
			break;
		case 2: // MOV
			this.processaMov();
			break;
		case 3: // IMUL
			this.processaImul();
			break;
		case 4: // INC
			this.processaInc();
			break;
		}
	}

	private void processaAdd() {
		int valor1 = this.valores.get(0);
		int valor2 = this.valores.get(1);
		this.resultado = valor1 + valor2;
		this.pode_gravar_resultado = true;
	}

	private void processaMov() {
		if (this.instrucao_atual[3] < -5) { // posicao da memória ram
			this.resultado = this.valores.get(1);
			this.grava_sinal_controle_mov = true;
		} else { // registrador
			switch (this.instrucao_atual[3]) {
			case -2: // A
				this.defineRegistradorA(this.valores.get(1));
				break;
			case -3: // B
				this.defineRegistradorB(this.valores.get(1));
				break;
			case -4: // C
				this.defineRegistradorC(this.valores.get(1));
				break;
			case -5: // D
				this.defineRegistradorD(this.valores.get(1));
				break;
			}
		}
	}

	private void processaImul() {
		int valor1 = this.valores.get(1);
		int valor2 = this.valores.get(2);
		this.resultado = valor1 * valor2;
		this.pode_gravar_resultado = true;
	}

	private void processaInc() {
		int valor = this.valores.get(0);
		this.resultado = valor + 1;
		this.pode_gravar_resultado = true;
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
		this.CI += 4;
		if (this.CI >= (Modulo.memoria_ram.memoria.length / 2)) {
			this.CI = 0;
		}
	}

	/**
	 * 
	 * Recebe sinal de endereço da RAM, após mandar um sinal de controle pedindo
	 * para ler
	 * 
	 * @param sinal_endereco
	 */
	public void recebeEndereco(int[] sinal_endereco) {
		System.out.println("CPU: recebeu sinal de endereco");
		this.endereco_atual = sinal_endereco[2];
		if (this.pode_processar_instrucao && this.grava_sinal_controle_mov) {
			this.mandaSinalDadoMov();
		} else if (this.pode_gravar_resultado) {
			int[] sinal_dado = {2, this.endereco_atual, this.resultado};
			Modulo.barramento.fila_dado.add(sinal_dado);
			this.preparaCpuParaProximaInstrucao();
		}
	}

	private void preparaCpuParaProximaInstrucao() {
		for (int i = 0; i < this.instrucao_atual.length; i++) {
			this.instrucao_atual = {};
		}
		this.instrucao_atual[0] = -1;
		this.instrucao_atual[1] = -1;
		this.instrucao_atual[2] = -1;
		this.instrucao_atual[3] = -1;
	}

	private void mandaSinalDadoMov() {
		int[] sinal_dado = {2, this.endereco_atual, this.resultado};
		Modulo.barramento.fila_dado.add(sinal_dado);
		this.grava_sinal_controle_mov = false;
	}

	/**
	 * Recebe sinal de dado da RAM
	 * 
	 * @param sinal_dado
	 */
	public void recebeDado(int[] sinal_dado) {
		System.out.println("CPU: recebeu sinal de dado");
		if (sinal_dado.length == 3) { // É um valor
			this.adicionaNaPrimeiraPosicaoDeValoresDisponivel(sinal_dado[1]);
		} else { // É uma instrução
			this.instrucao_atual = sinal_dado;
			this.pegaValoresDosParametros();
		}
		this.verificaSeInstrucaoEstaProntaParaSerProcessada();
	}

	private void verificaSeInstrucaoEstaProntaParaSerProcessada() {
		if (this.instrucao_atual.length > 0 && this.instrucao_atual[2] > 0 
				&& this.instrucao_atual[2] < 5 && this.valores.size() == 3) {
			this.pode_processar_instrucao = true;
		}
	}

	private void adicionaNaPrimeiraPosicaoDeValoresDisponivel(int valor) {
		for (int i = 0; i < 3; i++) {
			if (this.valores.size() < 0 && this.valores.get(i) != -1) {
				// adiciona o valor
				this.valores.add(valor);
			}
		}
	}

	private void pegaValoresDosParametros() {
		if (this.instrucao_atual.length == 7) { // instrução normal
			this.pedeValorParaRam(0, this.instrucao_atual[3]);
			if (this.instrucao_atual[4] != -1) {
				this.pedeValorParaRam(1, this.instrucao_atual[4]);
			} else {
				this.valores.add(1, -1);
			}
			if (this.instrucao_atual[5] != -1) {
				this.pedeValorParaRam(2, this.instrucao_atual[5]);
			} else {
				this.valores.add(2, -1);
			}
		} else {
			// LOOP
		}
	}

	private void pedeValorParaRam(int indice, int parametro) {
		if (parametro > -6 && parametro < -1) {
			// registrador
			switch (parametro) {
			case -2:
				this.valores.add(indice, this.pegaRegistradorA());
				break;
			case -3:
				this.valores.add(indice, this.pegaRegistradorB());
				break;
			case -4:
				this.valores.add(indice, this.pegaRegistradorC());
				break;
			case -5:
				this.valores.add(indice, this.pegaRegistradorD());
				break;
			}
		} else {
			// posição de memória
			int[] sinal_de_controle = {NUMERO_DESSE_MODULO, 2, 0, parametro};
			Modulo.barramento.fila_controle.add(sinal_de_controle);
		}
	}

}
