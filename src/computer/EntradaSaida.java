package computer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.Modulo;

/**
 * @author Airton
 * Dicionario ------- 
 * 0 -> Sinal de Leitura 
 * 1 -> ADD || ES || Sinal de Gravação
 * 2 -> MOV || RAM || Sinal de LOOP
 * 3 -> IMUL || CPU 
 * 4 -> INC 
 * 5 -> DEC 
 * 6 -> LABEL
 * 7 -> LOOP
 * -1 -> null 
 * -2 -> A || >
 * -3 -> B || <
 * -4 -> C || >=
 * -5 -> D || <=
 * -6 -> == || Daqui pra menos ficam as
 *         posições de memória [ex: 0x004 = 4 => ((4 + 6) * -1) => -10]
 * -7 -> !=
 */
public class EntradaSaida implements Runnable {
	private ArrayList<int[]> instrucoes_convertidas = new ArrayList<int[]>();
	private BufferedReader leitor;

	private final int NUMERO_DESSE_MODULO = 1;

	private ArrayList<Integer> endereco_atual = new ArrayList<Integer>();
	private boolean pode_mandar_sinal_de_controle = true;
	private boolean pode_mandar_sinal_de_dado = false;
	private int indice_da_instrucao_analisada;
	private int sinal_loop;

	/**
	 * Lê o arquivo e preenche a lista de instruções convertidas.
	 * 
	 * @return TRUE caso não hajam erros com nenhuma intrução, FALSE caso
	 *         contrário.
	 */
	public boolean compilaArquivo() {
		// Lê o arquivo e passa para a lista de instruções
		// String caminho =
		// "C:\\Users\\Airton\\workspace\\Emulador\\src\\main\\asm.txt";
//		String caminho = "C:\\sistemas\\EmuladorJava\\src\\main\\asm.txt";
		String caminho = "C:\\sistemas\\EmuladorJava\\src\\main\\asm2.txt";
		try {
			leitor = new BufferedReader(new FileReader(caminho));
			while (leitor.ready()) {
				System.out.println("ES: Compila Instrucao");
				// Valida a sintaxe de cada instrução
				if (!this.analisadorSintatico(leitor.readLine().toLowerCase())) {
					return false;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		int contador_de_instrucoes_enviadas = 0;

		// origem, ação, endereço
		int[] sinal_controle = { NUMERO_DESSE_MODULO, 2, 1, -1 };

		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (this.pode_mandar_sinal_de_controle) {
				this.calculaNumeroDeInstrucoesQuePodemSerPassadas();
				for (int j = 0; j < Modulo.barramento.numero_de_instrucoes_passadas; j++) {
					if (this.sinal_loop == j) {
						System.out.println("ES: mandou sinal de controle (LOOP)");
						int[] sinal_controle_loop = {NUMERO_DESSE_MODULO, 2, 2, -1};
						Modulo.barramento.adicionaFilaControle(sinal_controle_loop);
					} else {
						System.out.println("ES: mandou sinal de controle");
						Modulo.barramento.adicionaFilaControle(sinal_controle);
					}
				}
				this.pode_mandar_sinal_de_controle = false;
			}

			if (this.pode_mandar_sinal_de_dado) {
				for (int j = 0; j < Modulo.barramento.numero_de_instrucoes_passadas; j++) {
					System.out.println("ES: mandou sinal de dado");
					// Pega o comando em buffer, de acordo com o CI da CPU
					int[] sinal_dado = this.buffer(contador_de_instrucoes_enviadas);

					// Adiciona o destino e o endereço na fila de dado local
					sinal_dado[0] = NUMERO_DESSE_MODULO;
					sinal_dado[1] = this.endereco_atual.get(0);

					// Adiciona na fila de dado do barramento
					Modulo.barramento.adicionaFilaDado(sinal_dado);

					contador_de_instrucoes_enviadas++;
					this.endereco_atual.remove(0);
				}
				this.pode_mandar_sinal_de_controle = true;
				this.pode_mandar_sinal_de_dado = false;
			}

			// Se o contador de instrucoes enviadas ler a última instrução, mata
			// a thread de entrada e saida
			if (contador_de_instrucoes_enviadas >= this.instrucoes_convertidas.size()) {
				System.out.println("Thread ES acabou!");
				Modulo.barramento.es_finalizada = true;
				Thread.interrupted();
				break;
			}

		}
	}

	private void calculaNumeroDeInstrucoesQuePodemSerPassadas() {
		Modulo.barramento.numero_de_instrucoes_passadas = 0;
		int peso = 0;
		for (int i = this.indice_da_instrucao_analisada; i < this.instrucoes_convertidas.size(); i++) {
			if (this.instrucoes_convertidas.get(i).length < 8) {
				// instrução normal
				peso += 16;
			} else {
				// loop
				peso += 20;
				this.sinal_loop = Modulo.barramento.numero_de_instrucoes_passadas; 
			}
			
			if (peso <= Modulo.barramento.largura_de_banda) {
				Modulo.barramento.numero_de_instrucoes_passadas++;
			} else {
				this.sinal_loop = -1;
				break;
			}
		}
	}

	/**
	 * Busca a instrução compilada na posição parametrizada.
	 * 
	 * @param contador_instrucao
	 * @return Instrução compilada.
	 */
	public int[] buffer(int contador_instrucao) {
		if (contador_instrucao >= instrucoes_convertidas.size()) {
			return null;
		}
		return instrucoes_convertidas.get(contador_instrucao);
	}

	/**
	 * Analisa se há algum erro na instrução parametrizada e, caso não haja,
	 * adiciona a intrução na fila de intruções convertidas.
	 * 
	 * @param comando
	 * @return TRUE caso a instrução for adicionada na fila de instruções
	 *         convetidas, FALSE se houver algum erro na instrução.
	 */
	public boolean analisadorSintatico(String comando) {
		Matcher acerto_add = Pattern.compile("^add\\s+(\\w+)\\s*,\\s*(\\w+)\\s*$").matcher(comando);
		Matcher acerto_imul = Pattern.compile("^imul\\s+(\\w+)\\s*,\\s*(\\w+)\\s*,\\s*(\\w+)\\s*$").matcher(comando);
		Matcher acerto_mov = Pattern.compile("^mov\\s+(\\w+)\\s*,\\s*(\\w+)\\s*$").matcher(comando);
		Matcher acerto_inc = Pattern.compile("^inc\\s+(\\w+)\\s*$").matcher(comando);
		Matcher acerto_dec = Pattern.compile("^dec\\s+(\\w+)\\s*$").matcher(comando);
		Matcher acerto_label = Pattern.compile("^label\\s+(\\w+)\\s*$").matcher(comando);
		Matcher acerto_jump = Pattern.compile("^\\((\\w+)\\s+(<|>|<=|>=|==|!=)\\s+(\\w+)\\)\\?jump\\((\\w+)\\)\\:0\\s*$").matcher(comando);

		int[] comando_convertido = null;

		if (acerto_add.matches()) {
			comando_convertido = this.converteComandoAdd(acerto_add);
		} else if (acerto_imul.matches()) {
			comando_convertido = this.converteComandoImul(acerto_imul);
		} else if (acerto_mov.matches()) {
			comando_convertido = this.converteComandoMov(acerto_mov);
		} else if (acerto_inc.matches()) {
			comando_convertido = this.converteComandoInc(acerto_inc);
		} else if (acerto_dec.matches()) {
			comando_convertido = this.converteComandoDec(acerto_dec);
		} else if (acerto_label.matches()) {
			comando_convertido = this.converteComandoLabel(acerto_label);
		} else if (acerto_jump.matches()) {
			comando_convertido = this.converteComandoJump(acerto_jump);
		}

		if (comando_convertido != null) {
			instrucoes_convertidas.add(comando_convertido);
			return true;
		}
		return false;
	}

	/**
	 * Converte o valor parametrizado de String para um int representando esse
	 * valor.
	 * 
	 * @param valor
	 * @return Valor convertido em inteiro ou NULL caso o valor não seja válido.
	 */
	public Integer converteValor(String valor) {
		if (valor.length() > 2 && (valor.substring(0, 2)).equals("0x")) {
			if (Long.parseLong(valor.replace("0x", ""), 16) > Modulo.memoria_ram.tamanho) {
				return null;
			}
			// +6 = pula para o primeiro valor depois do 5, para quando
			// multiplicar por -1
			return ((Integer.parseInt(valor.replace("0x", "")) + 6) * -1);
		} else if (valor.equals("a") || valor.equals("b") || valor.equals("c") || valor.equals("d")) {
			switch (valor) {
			case "a":
				return -2;
			case "b":
				return -3;
			case "c":
				return -4;
			case "d":
				return -5;
			}
		} else if (valor.equals(">") || valor.equals("<") || valor.equals(">=") 
				|| valor.equals("<=") || valor.equals("==") || valor.equals("!=")) {
			switch (valor) {
			case ">":
				return -2;
			case "<":
				return -3;
			case ">=":
				return -4;
			case "<=":
				return -5;
			case "==":
				return -6;
			case "!=":
				return -7;
			}
		} else if (Integer.parseInt(valor) >= 0) {
			return Integer.parseInt(valor);
		}
		return null;
	}

	/**
	 * Converte instrução ADD para números representativos.
	 * 
	 * @param acerto
	 * @return Vetor de inteiros convertidos ou NULL caso haja erro na
	 *         transformação de algum valor.
	 */
	private int[] converteComandoAdd(Matcher acerto) {
		Integer valor1 = this.converteValor(acerto.group(1));
		Integer valor2 = this.converteValor(acerto.group(2));
		if (valor1 == null || valor2 == null) {
			return null;
		}
		int[] valores = { -1, -1, 1, valor1, valor2, -1, 2 };
		return valores;
	}

	/**
	 * Converte instrução MOV para números representativos.
	 * 
	 * @param acerto
	 * @return Vetor de inteiros convertidos ou NULL caso haja erro na
	 *         transformação de algum valor.
	 */
	private int[] converteComandoMov(Matcher acerto) {
		Integer valor1 = this.converteValor(acerto.group(1));
		Integer valor2 = this.converteValor(acerto.group(2));
		if (valor1 == null || valor2 == null) {
			return null;
		}
		int[] valores = { -1, -1, 2, valor1, valor2, -1, 2 };
		return valores;
	}

	/**
	 * Converte instrução IMUL para números representativos.
	 * 
	 * @param acerto
	 * @return Vetor de inteiros convertidos ou NULL caso haja erro na
	 *         transformação de algum valor.
	 */
	private int[] converteComandoImul(Matcher acerto) {
		Integer valor1 = this.converteValor(acerto.group(1));
		Integer valor2 = this.converteValor(acerto.group(2));
		Integer valor3 = this.converteValor(acerto.group(3));
		if (valor1 == null || valor2 == null || valor3 == null) {
			return null;
		}
		int[] valores = { -1, -1, 3, valor1, valor2, valor3, 2 };
		return valores;
	}

	/**
	 * Converte instrução INC para números representativos.
	 * 
	 * @param acerto
	 * @return Vetor de inteiros convertidos ou NULL caso haja erro na
	 *         transformação de algum valor.
	 */
	private int[] converteComandoInc(Matcher acerto) {
		Integer valor1 = this.converteValor(acerto.group(1));
		if (valor1 == null) {
			return null;
		}
		int[] valores = { -1, -1, 4, valor1, -1, -1, 2 };
		return valores;
	}

	/**
	 * Converte instrução DEC para números representativos.
	 * 
	 * @param acerto
	 * @return Vetor de inteiros convertidos ou NULL caso haja erro na
	 *         transformação de algum valor.
	 */
	private int[] converteComandoDec(Matcher acerto) {
		Integer valor1 = this.converteValor(acerto.group(1));
		if (valor1 == null) {
			return null;
		}
		int[] valores = { -1, -1, 5, valor1, -1, -1, 2 };
		return valores;
	}
	
	/**
	 * Converte instrução LABEL para números representativos.
	 * 
	 * @param acerto
	 * @return Vetor de inteiros convertidos ou NULL caso haja erro na
	 *         transformação de algum valor.
	 */
	private int[] converteComandoLabel(Matcher acerto) {
		Integer valor1 = this.converteValor(acerto.group(1));
		if (valor1 == null) {
			return null;
		}
		int[] valores = { -1, -1, 6, valor1, -1, -1, 2 };
		return valores;
	}
	
	/**
	 * Converte instrução JUMP para números representativos.
	 * 
	 * @param acerto
	 * @return Vetor de inteiros convertidos ou NULL caso haja erro na
	 *         transformação de algum valor.
	 */
	private int[] converteComandoJump(Matcher acerto) {
		Integer valor1 = this.converteValor(acerto.group(1));
		Integer valor2 = this.converteValor(acerto.group(2));
		Integer valor3 = this.converteValor(acerto.group(3));
		Integer valor4 = this.converteValor(acerto.group(4));
		if (valor1 == null || valor2 == null || valor3 == null || valor4 == null) {
			return null;
		}
		int[] valores = { -1, -1, 7, valor1, valor2, valor3, valor4, 2 };
		return valores;
	}

	/**
	 * Recebe um sinal de endereço e deve mandar um sinal de dado, pela Thread
	 * 
	 * @param sinal_controle
	 */
	public void recebeEndereco(int[] sinal_endereco) {
		System.out.println("ES: recebeu sinal de endereco");
		this.endereco_atual.add(sinal_endereco[2]);
		this.pode_mandar_sinal_de_dado = true;
	}

}