package controlador;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import modelo.Dispositivos;

public class ControlScanner {

	public Dispositivos escannearIP (String ip, int Tiempo) {
		try {
			InetAddress direccion = InetAddress.getByName(ip);
			long inicio = System.currentTimeMillis();
			boolean respuesta = direccion.isReachable(Tiempo);
			long fin = System.currentTimeMillis();
			long tiempo = fin - inicio;
			if (respuesta) {
				String nombreHost = direccion.getHostName();
				return new Dispositivos(ip, nombreHost, true, tiempo);
			} else {
				return new Dispositivos(ip, "Desconocido", false, 0);
			}
		} catch (IOException e) {
			return new Dispositivos(ip, "Error de Red", false, 0);
		}
	}

	public List<Dispositivos> escanearRango(String ipBase, int inicioHost, int finHost, int tiempo){
		return escanearRango(ipBase, inicioHost, finHost, tiempo, null);
	}

	public List<Dispositivos> escanearRango(String ipBase, int inicioHost, int finHost, int tiempo,
			IntConsumer progreso){
		List<Dispositivos> Resultados = new ArrayList<>();
		AtomicInteger terminados = new AtomicInteger(0);
		ExecutorService executor = Executors.newFixedThreadPool(20);
		for(int i = inicioHost; i <= finHost; i++) {
			String ipAprobar = ipBase + "." + i;
			executor.submit(() -> {
				try {
					Dispositivos dev = escannearIP(ipAprobar, tiempo);
					// Se guardan todos los dispositivos (activos e inactivos)
					synchronized (Resultados) {
						Resultados.add(dev);
					}
				} finally {
					int n = terminados.incrementAndGet();
					if (progreso != null) {
						progreso.accept(n);
					}
				}
			});
		}
		executor.shutdown();
		try {
			executor.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return Resultados;
	}

	public String validarDatos(String ip, String inicio, String fin, String tiempo) {
		ip = ip.trim();
		if (ip.isEmpty()) {
			return "Ingresá la IP de la subred (ej: 192.168.1).";
		}
		if (!esSubredValida(ip)) {
			return "La subred debe tener 3 números entre 0 y 255 (ej: 192.168.1).";
		}
		Integer desde = aEntero(inicio);
		Integer hasta = aEntero(fin);
		Integer ms = aEntero(tiempo);
		if (desde == null || hasta == null) {
			return "Desde y Hasta deben ser números enteros.";
		}
		if (desde < 1 || hasta > 254) {
			return "Desde y Hasta deben estar entre 1 y 254.";
		}
		if (desde > hasta) {
			return "Desde no puede ser mayor que Hasta.";
		}
		if (ms == null || ms < 1 || ms > 10000) {
			return "El tiempo máximo debe ser un número entre 1 y 10000 ms.";
		}
		return null;
	}

	public boolean esSubredValida(String ip) {
		String[] partes = ip.trim().split("\\.", -1);
		if (partes.length != 3) {
			return false;
		}
		for (String p : partes) {
			Integer n = aEntero(p);
			if (n == null || n < 0 || n > 255) {
				return false;
			}
		}
		return true;
	}

	public void guardarResultados(List<Dispositivos> lista, File archivo) throws IOException {
		try (BufferedWriter w = Files.newBufferedWriter(archivo.toPath(), StandardCharsets.UTF_8)) {
			w.write("IP,Nombre,Estado,Tiempo (ms)");
			w.newLine();
			for (Dispositivos d : lista) {
				w.write(d.getIp() + ","
						+ "\"" + d.getNombre().replace("\"", "\"\"") + "\","
						+ (d.isConectado() ? "Activo" : "Inactivo") + ","
						+ (d.isConectado() ? String.valueOf(d.getTiempo()) : "-"));
				w.newLine();
			}
		}
	}

	private Integer aEntero(String texto) {
		try {
			return Integer.parseInt(texto.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}