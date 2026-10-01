package modelo;

public class Dispositivos {
    private String ip;
    private String nombre;
    private boolean conectado;
    private long tiempo;

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
        return conectado;
    }

    public void setConectado(boolean conectado) {
        this.conectado = conectado;
    }

    public long getTiempo() {
        return tiempo;
    }

    public void setTiempo(long tiempo) {
        this.tiempo = tiempo;
    }

    public Dispositivos(String ip, String nombre, boolean conectado, long tiempo) {
        super();
        this.ip = ip;
        this.nombre = nombre;
        this.conectado = conectado;
        this.tiempo = tiempo;
    }

    @Override
    public String toString() {
        return "IP=" + ip
                + ", Nombre=" + nombre
                + ", Estado=" + (conectado ? "Activo" : "Inactivo")
                + ", Tiempo=" + (conectado ? tiempo + " ms" : "-");
    }
}