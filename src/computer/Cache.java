package computer;

import com.sun.jmx.snmp.Timestamp;

public class Cache {
	public Long timestamp;
	public int indiceDaRam;
	public int conteudo;
	
	public Long pegaTimestamp() {
		return timestamp;
	}
	public void defineTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
	
	public int pegaIndiceDaRam() {
		return indiceDaRam;
	}
	public void defineIndiceDaRam(int indiceDaRam) {
		this.indiceDaRam = indiceDaRam;
	}
	
	public int pegaConteudo() {
		return conteudo;
	}
	public void defineConteudo(int conteudo) {
		this.conteudo = conteudo;
	}
	
	public void add(int indiceDaRam, int conteudo, Long timestamp) {
		if (timestamp == null) {
			Timestamp ts = new Timestamp();
			timestamp = ts.getDateTime();
		}
		this.defineTimestamp(timestamp);
		this.defineConteudo(conteudo);
		this.defineIndiceDaRam(indiceDaRam);
	}
	
	public void remove() {
		this.defineTimestamp(null);
		this.defineConteudo(0);
		this.defineIndiceDaRam(-1);
	}
	
	public void renova() {
		Timestamp ts = new Timestamp();
		this.defineTimestamp(ts.getDateTime());
	}
	
}