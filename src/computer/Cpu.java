package computer;

import java.util.ArrayList;

import main.Modulo;

public class Cpu implements Runnable {
	public int CI = 0;
	private boolean pode_processar_instrucao = false;

	private int endereco_atual;
	private ArrayList<Integer> instrucao_atual = new ArrayList<Integer>();
	private ArrayList<Integer> valores = new ArrayList<Integer>(); // tam max = 3
	
	// 0 => valor do label
	// 1 => indice do label
	private ArrayList<Integer> ci_provisorio = new ArrayList<Integer>(); 

	private int registrador_a;
	private int registrador_b;
	private int registrador_c;
	private int registrador_d;

	private final int NUMERO_DESSE_MODULO = 3;
	private int resultado;
	private boolean pode_gravar_resultado;
	private boolean grava_sinal_controle_mov;
	private boolean pode_pedir_instrucao = true;
	private boolean pode_gravar_resposta_para_ram = false;
	private boolean pode_mandar_dado_mov = false;
	private int num_instrucoes_esperadas = 0;
	private boolean ultimo_loop = false;
	private boolean processamento_finalizado = false;

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (this.pode_pedir_instrucao && Modulo.barramento.es_finalizada && !this.processamento_finalizado) {
				if (this.ultimo_loop) {
					this.processamento_finalizado  = true;
				}
				System.out.println("CPU: pediu instrução ("+this.CI+")");
				int[] sinal_controle = {NUMERO_DESSE_MODULO, 2, 0, this.CI};
				Modulo.barramento.fila_controle.add(sinal_controle);
				if (this.ci_provisorio.size() == 2) {
					this.ci_provisorio.set(1, this.CI);
				} else {
					this.ci_provisorio.add(-1);
					this.ci_provisorio.add(this.CI);
				}
				this.avancaCi();
				this.pode_pedir_instrucao = false;
			}

			// Processa uma instrução
			if (this.pode_processar_instrucao) {
				this.processaInstrucao();
				this.pode_processar_instrucao = false;
			}
			
			if (this.pode_gravar_resultado) {
				this.pode_gravar_resultado = false;
				this.pode_gravar_resposta_para_ram  = true;
				this.gravaResultado();
			}
			
			if (this.grava_sinal_controle_mov) {
				System.out.println("CPU: gravou sinal de controle do MOV");
				int[] sinal_controle = {NUMERO_DESSE_MODULO, 2, 1, this.instrucao_atual.get(3)};
				Modulo.barramento.fila_controle.add(sinal_controle);
				this.grava_sinal_controle_mov = false;
				this.pode_mandar_dado_mov = true;
			}

		}
	}

	private void gravaResultado() {
		if (this.instrucao_atual.get(3) < -5) { // posição na memória ram
			System.out.println("CPU: gravou resultado na RAM");
			int posicao_real = (this.instrucao_atual.get(3) * -1) - 6 + (Modulo.memoria_ram.tamanho / 2);
			int[] sinal_controle = {NUMERO_DESSE_MODULO, 2, 1, posicao_real};
			Modulo.barramento.fila_controle.add(sinal_controle);
		} else if (this.instrucao_atual.get(3) > -6 && this.instrucao_atual.get(3) < -1) { // registrador
			System.out.println("CPU: gravou resultado nos registradores");
			switch (this.instrucao_atual.get(3)) {
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
			System.out.print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n| "+this.registrador_a+" | "+this.registrador_b+" | "+this.registrador_c+" | "+this.registrador_d+" |\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
			this.preparaCpuParaProximaInstrucao();
		}
	}

	private void processaInstrucao() {
		switch (this.instrucao_atual.get(2)) {
		case 1: // ADD
			System.out.println("CPU: processou instrucao ADD");
			this.processaAdd();
			break;
		case 2: // MOV
			System.out.println("CPU: processou instrucao MOV");
			this.processaMov();
			break;
		case 3: // IMUL
			this.processaImul();
			System.out.println("CPU: processou instrucao IMUL");
			break;
		case 4: // INC
			this.processaInc();
			System.out.println("CPU: processou instrucao INC");
			break;
		case 5: // DEC
			this.processaDec();
			System.out.println("CPU: processou instrucao DEC");
			break;
		case 6: // LABEL
			this.processaLabel();
			System.out.println("CPU: processou instrucao LABEL");
			break;
		case 7: // JUMP
			this.processaJump();
			System.out.println("CPU: processou instrucao JUMP");
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
		if (this.instrucao_atual.get(3) < -5) { // posicao da memória ram
			this.resultado = this.valores.get(1);
			this.grava_sinal_controle_mov = true;
			System.out.println("CPU: processou mov: "+this.resultado);
		} else { // registrador
			switch (this.instrucao_atual.get(3)) {
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
	
	private void processaDec() {
		int valor = this.valores.get(0);
		this.resultado = valor - 1;
		this.pode_gravar_resultado = true;
	}
	
	private void processaLabel() {
		this.ci_provisorio.set(0, this.valores.get(0));
		this.preparaCpuParaProximaInstrucao();
	}
	
	private void processaJump() {
		int param_1 = this.valores.get(0);
		int simbolo_comparativo = this.valores.get(1);
		int param_2 = this.valores.get(2);
		int indice_do_jump = this.valores.get(3);
		
		boolean condicao = false;
		
		switch (simbolo_comparativo) {
		case -2: // >
			if (param_1 > param_2) {
				condicao = true;
			}
			break;
		case -3: // <
			if (param_1 < param_2) {
				condicao = true;
			}
			break;
		case -4: // >=
			if (param_1 >= param_2) {
				condicao = true;
			}
			break;
		case -5: // <=
			if (param_1 <= param_2) {
				condicao = true;
			}
			break;
		case -6: // ==
			if (param_1 == param_2) {
				condicao = true;
			}
			break;
		case -7: // !=
			if (param_1 != param_2) {
				condicao = true;
			}
			break;
		}
		
		if (condicao && this.ci_provisorio.get(0) == indice_do_jump) {
			this.CI = this.ci_provisorio.get(1);
		}
		
		this.preparaCpuParaProximaInstrucao();
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
		if (this.ultimo_loop) {
			System.out.println("CPU: fim do Processamento");
			Thread.interrupted();
			this.pode_pedir_instrucao = false;
		}
		this.CI += 4;
		if (this.CI >= (Modulo.memoria_ram.memoria.length / 2)) {
			this.CI = 0;
		}
		if (this.CI == Modulo.barramento.ultima_posicao_inserida) {
			this.ultimo_loop = true;
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
		System.out.println("CPU: recebeu sinal de endereco: "+sinal_endereco[0]+", "+sinal_endereco[1]+", "+sinal_endereco[2]);
		this.endereco_atual = sinal_endereco[2];
		if (this.pode_mandar_dado_mov) {
			this.mandaSinalDadoMov();
			this.preparaCpuParaProximaInstrucao();
		} else if (this.pode_gravar_resposta_para_ram) {
			System.out.println("CPU: gravou sinal de dado");
			int[] sinal_dado = {2, this.endereco_atual, this.resultado};
			Modulo.barramento.fila_dado.add(sinal_dado);
			this.preparaCpuParaProximaInstrucao();
		}
	}

	private void preparaCpuParaProximaInstrucao() {
		this.instrucao_atual.clear();
		this.valores.clear();
		this.pode_pedir_instrucao = true;
		this.pode_gravar_resposta_para_ram = false;
		this.num_instrucoes_esperadas = 0;
		System.out.println("CPU: resetou valores para proximo loop");
	}

	private void mandaSinalDadoMov() {
		int posicao_real = (this.endereco_atual * -1) - 6 + (Modulo.memoria_ram.tamanho / 2);
		int[] sinal_dado = {2, posicao_real, this.resultado};
		Modulo.barramento.fila_dado.add(sinal_dado);
		this.pode_mandar_dado_mov = false;
		System.out.println("CPU: gravou sinal de dado (MOV)");
	}

	/**
	 * Recebe sinal de dado da RAM
	 * 
	 * @param sinal_dado
	 */
	public void recebeDado(int[] sinal_dado) {
		System.out.println("CPU: recebeu sinal de dado");
		if (sinal_dado.length == 3) { // É um valor
			this.adicionaNaPrimeiraPosicaoDeValoresDisponivel(sinal_dado[2]);
			this.num_instrucoes_esperadas--;
		} else { // É uma instrução
			for (int i = 0; i < sinal_dado.length; i++) {
				this.instrucao_atual.add(sinal_dado[i]);
			}
			this.pegaValoresDosParametros();
		}
		this.verificaSeInstrucaoEstaProntaParaSerProcessada();
	}

	private void verificaSeInstrucaoEstaProntaParaSerProcessada() {
		System.out.println("CPU: verifica se a instrucao esta pronta para ser utilizada ("+this.num_instrucoes_esperadas+")");
		for (int i = 0; i < this.valores.size(); i++) {
			if (i == 0) {
				System.out.println("---------------------------------------------------------------------------");
			}
			System.out.print("| "+this.valores.get(i)+" | ");
		}
		System.out.println("");
		
		if (this.instrucao_atual.size() > 0 && this.instrucao_atual.get(2) > 0 
				&& this.instrucao_atual.get(2) < 8 && this.valores.size() == 3 
				&& this.num_instrucoes_esperadas == 0) {
			this.pode_processar_instrucao = true;
		}
	}

	private void adicionaNaPrimeiraPosicaoDeValoresDisponivel(int valor) {
		System.out.println("CPU: recebeu um valor para um parametro buscado");
		boolean pode_adicionar = true;
		for (int i = 0; i < this.valores.size(); i++) {
			if (this.valores.size() > 0 && this.valores.get(i) == -1 && pode_adicionar) {
				pode_adicionar = false;
				
				// adiciona o valor
				System.out.println("CPU: adicionou "+valor+" na posicao "+i);
				this.valores.set(i, valor);
			}
		}
	}

	private void pegaValoresDosParametros() {
		if (this.instrucao_atual.size() == 7) { // instrução normal
			if (this.valores.size() == 0) {
				for (int i = 0; i < 3; i++) {
					this.valores.add(-1);
				}	
			}
			
			// é um mov ou imul, nao precisa pegar o primeiro valor, apenas converter
			if (this.instrucao_atual.get(2) > 1 && this.instrucao_atual.get(2) < 4) { 
				int posicao_real = ( this.instrucao_atual.get(3) * -1 ) - 6 + (Modulo.memoria_ram.tamanho / 2);
				System.out.println("CPU: Converte "+this.instrucao_atual.get(3)+" para "+posicao_real);
				this.valores.set(0, posicao_real);
			} else {
				this.pedeValorParaRam(0, this.instrucao_atual.get(3));
			}
			
			if (this.instrucao_atual.get(4) != -1) {
				this.pedeValorParaRam(1, this.instrucao_atual.get(4));
			}
			
			if (this.instrucao_atual.get(5) != -1) {
				this.pedeValorParaRam(2, this.instrucao_atual.get(5));
			}
		}
	}

	private void pedeValorParaRam(int indice, int parametro) {
		System.out.println("CPU: computa parametro "+parametro);
		if (parametro > -6 && parametro < -1) {
			// registrador
			switch (parametro) {
			case -2:
				this.valores.set(indice, this.pegaRegistradorA());
				break;
			case -3:
				this.valores.set(indice, this.pegaRegistradorB());
				break;
			case -4:
				this.valores.set(indice, this.pegaRegistradorC());
				break;
			case -5:
				this.valores.set(indice, this.pegaRegistradorD());
				break;
			}
		} else if (parametro < -5) {
			// posição de memória
			this.num_instrucoes_esperadas++;
			int parametro_real = (parametro * -1) - 6 + (Modulo.memoria_ram.tamanho / 2);
			System.out.println("CPU: manda sinal de controle para buscar parametro ("+parametro_real+")");
			int[] sinal_de_controle = {NUMERO_DESSE_MODULO, 2, 0, parametro_real};
			Modulo.barramento.fila_controle.add(sinal_de_controle);
		} else {
			this.valores.set(indice, parametro);
		}
	}

}