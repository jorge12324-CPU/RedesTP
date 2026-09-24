package modelo;

public class Dispositivos {
	private String ip;
	private String nombre;
	private boolean isConectado;
	private long Tiempo;
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public boolean isConectado() {
		return isConectado;
	}
	public void setisConectado(boolean isConectado) {
		this.isConectado = isConectado;
	}
	public long getTiempo() {
		return Tiempo;
	}
	public void setTiempo(long tiempo) {
		Tiempo = tiempo;
	}
	public Dispositivos(String ip, String nombre, boolean isConectado, long tiempo) {
		super();
		this.ip = ip;
		this.nombre = nombre;
		this.isConectado = isConectado;
		Tiempo = tiempo;
	}
	@Override
	public String toString() {
		return "IP=" + ip
				+ ", Nombre=" + nombre
				+ ", Estado=" + (isConectado ? "Activo" : "Inactivo")
				+ ", Tiempo=" + (isConectado ? Tiempo + " ms" : "-");

	}
}
 