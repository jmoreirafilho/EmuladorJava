package computer;

import java.util.ArrayList;

public class Cache {
	public int tamanhoCache;
	public int timestamp;
	public int indiceDaRam;
	public ArrayList<Integer> conteudo = new ArrayList<Integer>();
	
	public int getTamanhoCache() {
		return tamanhoCache;
	}
	public void setTamanhoCache(int tamanhoCache) {
		this.tamanhoCache = tamanhoCache;
	}
	public int getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
	public int getIndiceDaRam() {
		return indiceDaRam;
	}
	public void setIndiceDaRam(int indiceDaRam) {
		this.indiceDaRam = indiceDaRam;
	}
	public ArrayList<Integer> getConteudo() {
		return conteudo;
	}
	public void setConteudo(ArrayList<Integer> conteudo) {
		this.conteudo = conteudo;
	}
	
	
}