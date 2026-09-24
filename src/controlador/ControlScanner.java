package controlador;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

import modelo.Dispositivos;

public class ControlScanner {

	public Dispositivos escannearIP (String ip,int Tiempo) {
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
		List<Dispositivos> Resultados = new ArrayList<>();
		ExecutorService executor = Executors.newFixedThreadPool(20);
		for(int i = inicioHost; i<= finHost; i++) {
			String ipAprobar = ipBase +"."+i;
			executor.submit(()->{
				Dispositivos dev = escannearIP(ipAprobar, tiempo);
				if (dev.isConectado()) {
					synchronized (Resultados) {
                        Resultados.add(dev);
                    }
				}
			});
		}
		executor.shutdown();
		try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
		return Resultados;
	}
	}

/**package vista;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List; // evita el conflicto con java.awt.List
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;

import controlador.ControladorEscanner;
import modelo.Dispositivos;

public class VistaPrincipal extends JFrame {
	private JTextField txtIp;
	private JTextField txtInicioIp;
	private JTextField txtFinIp;
	private JTextField txtTiempoMax;
	private JTextField txtFiltro;
	private JComboBox<String> cmbEstado;
	private JButton btnEscanear;
	private JButton btnLimpiar;
	private JButton btnGuardar;
	private JLabel lblMensaje;
	private JLabel lblTotal;
    private JTable tablaResultados;
    private DefaultTableModel modeloTabla;
    private TableRowSorter<DefaultTableModel> sorter;
    private JProgressBar barraProgreso;

    private ControladorEscanner controlador;
    private List<Dispositivos> resultados = new ArrayList<>();
    private boolean escaneando = false;

    public VistaPrincipal() {
    	controlador = new ControladorEscanner();

    	setTitle("Escáner de Red LAN - TP Redes");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Fila 1: datos del rango
        JPanel jpFormulario = new JPanel(new FlowLayout(FlowLayout.LEFT,10,10));

        jpFormulario.add(new JLabel("IP Subred:"));
        txtIp = new JTextField("192.168.1", 8);
        jpFormulario.add(txtIp);

        jpFormulario.add(new JLabel("Desde:"));
        txtInicioIp = new JTextField("1", 3);
        jpFormulario.add(txtInicioIp);

        jpFormulario.add(new JLabel("Hasta:"));
        txtFinIp = new JTextField("20", 3);
        jpFormulario.add(txtFinIp);

        jpFormulario.add(new JLabel("Tiempo Máx (ms):"));
        txtTiempoMax = new JTextField("1000", 4);
        jpFormulario.add(txtTiempoMax);

        // Fila 2: botones y filtros
        JPanel jpBotones = new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));

        btnEscanear = new JButton("Escanear");
        jpBotones.add(btnEscanear);

        btnLimpiar = new JButton("Limpiar");
        jpBotones.add(btnLimpiar);

        btnGuardar = new JButton("Guardar resultados");
        jpBotones.add(btnGuardar);

        jpBotones.add(new JLabel("Filtrar (IP o nombre):"));
        txtFiltro = new JTextField(10);
        jpBotones.add(txtFiltro);

        cmbEstado = new JComboBox<>(new String[] {"Todos", "Activos", "Inactivos"});
        jpBotones.add(cmbEstado);

        // Fila 3: mensaje de validación
        lblMensaje = new JLabel(" ");
        lblMensaje.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JPanel jpNorte = new JPanel(new GridLayout(3, 1));
        jpNorte.add(jpFormulario);
        jpNorte.add(jpBotones);
        jpNorte.add(lblMensaje);
        add(jpNorte, BorderLayout.NORTH);


        String[] columnas = {"IP","Nombre Dispositivo","Estado","Tiempo Respuesta (ms)"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
        	@Override
        	public boolean isCellEditable(int fila, int columna) {
        		return false; // la tabla es solo de lectura
        	}
        	@Override
        	public Class<?> getColumnClass(int columna) {
        		// Con Long, la columna de tiempo se ordena como número
        		return columna == 3 ? Long.class : String.class;
        	}
        };
        tablaResultados = new JTable(modeloTabla);

        // Permite ordenar haciendo clic en los títulos y filtrar las filas
        sorter = new TableRowSorter<>(modeloTabla);
        sorter.setComparator(0, (a, b) -> compararIp((String) a, (String) b));
        tablaResultados.setRowSorter(sorter);

        JScrollPane scrollTabla = new JScrollPane(tablaResultados);
        add(scrollTabla, BorderLayout.CENTER);


        JPanel panelInferior = new JPanel(new BorderLayout(5, 5));
        barraProgreso = new JProgressBar();
        barraProgreso.setStringPainted(true);
        panelInferior.add(barraProgreso, BorderLayout.CENTER);

        lblTotal = new JLabel("Equipos que respondieron: 0");
        panelInferior.add(lblTotal, BorderLayout.SOUTH);

        add(panelInferior, BorderLayout.SOUTH);


        // Validación mientras se escribe
        DocumentListener validar = alCambiar(this::validarCampos);
        txtIp.getDocument().addDocumentListener(validar);
        txtInicioIp.getDocument().addDocumentListener(validar);
        txtFinIp.getDocument().addDocumentListener(validar);
        txtTiempoMax.getDocument().addDocumentListener(validar);

        // Filtros de la tabla
        txtFiltro.getDocument().addDocumentListener(alCambiar(this::filtrar));
        cmbEstado.addActionListener(e -> filtrar());

        btnEscanear.addActionListener(e -> escanear());
        btnLimpiar.addActionListener(e -> limpiar());
        btnGuardar.addActionListener(e -> guardar());

        validarCampos();
    }

    // Crea un DocumentListener que ejecuta la acción cada vez que cambia el texto
    private DocumentListener alCambiar(Runnable accion) {
    	return new DocumentListener() {
    		@Override
    		public void insertUpdate(DocumentEvent e) { accion.run(); }
    		@Override
    		public void removeUpdate(DocumentEvent e) { accion.run(); }
    		@Override
    		public void changedUpdate(DocumentEvent e) { accion.run(); }
    	};
    }

    // Revisa los datos mientras el usuario escribe y muestra qué está mal
    private void validarCampos() {
    	String error = controlador.validarDatos(txtIp.getText(), txtInicioIp.getText(),
    			txtFinIp.getText(), txtTiempoMax.getText());

    	boolean subredMal = !txtIp.getText().isEmpty() && !controlador.esSubredValida(txtIp.getText());
    	txtIp.setBackground(subredMal ? new Color(255, 200, 200) : Color.WHITE);

    	if (error == null) {
    		lblMensaje.setForeground(new Color(0, 120, 0));
    		lblMensaje.setText("Datos correctos. Listo para escanear.");
    	} else {
    		lblMensaje.setForeground(Color.RED);
    		lblMensaje.setText(error);
    	}
    	btnEscanear.setEnabled(error == null && !escaneando);
    }

    private void escanear() {
    	String ip = txtIp.getText().trim();
    	int inicio = Integer.parseInt(txtInicioIp.getText().trim());
    	int fin = Integer.parseInt(txtFinIp.getText().trim());
    	int tiempo = Integer.parseInt(txtTiempoMax.getText().trim());

    	limpiar();
    	escaneando = true;
    	btnEscanear.setEnabled(false);
    	btnLimpiar.setEnabled(false);
    	btnGuardar.setEnabled(false);
    	lblMensaje.setForeground(Color.DARK_GRAY);
    	lblMensaje.setText("Escaneando...");
    	barraProgreso.setMaximum(fin - inicio + 1);

    	// SwingWorker: hace el escaneo en segundo plano para que la ventana no se congele
    	SwingWorker<List<Dispositivos>, Integer> worker = new SwingWorker<List<Dispositivos>, Integer>() {
    		@Override
    		protected List<Dispositivos> doInBackground() {
    			return controlador.escanearRango(ip, inicio, fin, tiempo, terminados -> publish(terminados));
    		}

    		@Override
    		protected void process(List<Integer> avances) {
    			int ultimo = avances.get(avances.size() - 1);
    			barraProgreso.setValue(ultimo);
    			barraProgreso.setString(ultimo + " / " + barraProgreso.getMaximum());
    		}

    		@Override
    		protected void done() {
    			try {
    				resultados = get();
    				mostrarResultados();
    				lblMensaje.setText("Escaneo finalizado.");
    			} catch (Exception ex) {
    				lblMensaje.setForeground(Color.RED);
    				lblMensaje.setText("Ocurrió un error durante el escaneo.");
    				JOptionPane.showMessageDialog(VistaPrincipal.this,
    						"Ocurrió un error durante el escaneo:\n" + ex.getMessage(),
    						"Error", JOptionPane.ERROR_MESSAGE);
    			}
    			escaneando = false;
    			btnLimpiar.setEnabled(true);
    			btnGuardar.setEnabled(true);
    			validarCampos();
    		}
    	};
    	worker.execute();
    }

    // Carga la tabla con los resultados, ordenados por IP
    private void mostrarResultados() {
    	resultados.sort((a, b) -> compararIp(a.getIp(), b.getIp()));
    	int activos = 0;
    	for (Dispositivos d : resultados) {
    		modeloTabla.addRow(new Object[] {
    				d.getIp(),
    				d.getNombre(),
    				d.getConectado() ? "Activo" : "Inactivo",
    				d.getConectado() ? Long.valueOf(d.getTiempoRespuesta()) : null });
    		if (d.getConectado()) {
    			activos++;
    		}
    	}
    	lblTotal.setText("Equipos que respondieron: " + activos + " de " + resultados.size());
    }

    private void limpiar() {
    	modeloTabla.setRowCount(0);
    	resultados = new ArrayList<>();
    	barraProgreso.setValue(0);
    	barraProgreso.setString(null);
    	lblTotal.setText("Equipos que respondieron: 0");
    }

    private void guardar() {
    	if (resultados.isEmpty()) {
    		JOptionPane.showMessageDialog(this, "Todavía no hay resultados para guardar.",
    				"Sin resultados", JOptionPane.INFORMATION_MESSAGE);
    		return;
    	}
    	JFileChooser selector = new JFileChooser();
    	selector.setSelectedFile(new File("resultados_escaneo.csv"));
    	selector.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
    	if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
    		return;
    	}
    	File archivo = selector.getSelectedFile();
    	if (!archivo.getName().toLowerCase().endsWith(".csv")) {
    		archivo = new File(archivo.getParentFile(), archivo.getName() + ".csv");
    	}
    	try {
    		controlador.guardarResultados(resultados, archivo);
    		JOptionPane.showMessageDialog(this, "Resultados guardados en:\n" + archivo.getAbsolutePath(),
    				"Guardado", JOptionPane.INFORMATION_MESSAGE);
    	} catch (IOException ex) {
    		JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo:\n" + ex.getMessage(),
    				"Error", JOptionPane.ERROR_MESSAGE);
    	}
    }

    // Filtra la tabla por texto (IP o nombre) y por estado
    private void filtrar() {
    	List<RowFilter<Object, Object>> filtros = new ArrayList<>();
    	String texto = txtFiltro.getText().trim();
    	if (!texto.isEmpty()) {
    		filtros.add(RowFilter.regexFilter("(?i)" + Pattern.quote(texto), 0, 1));
    	}
    	if (cmbEstado.getSelectedIndex() == 1) {
    		filtros.add(RowFilter.regexFilter("^Activo$", 2));
    	} else if (cmbEstado.getSelectedIndex() == 2) {
    		filtros.add(RowFilter.regexFilter("^Inactivo$", 2));
    	}

    	if (filtros.isEmpty()) {
    		sorter.setRowFilter(null);
    	} else {
    		sorter.setRowFilter(RowFilter.andFilter(filtros));
    	}
    }

    // Compara dos IP número por número (así 192.168.1.20 va antes que 192.168.1.100)
    private int compararIp(String a, String b) {
    	String[] pa = a.split("\\.");
    	String[] pb = b.split("\\.");
    	for (int i = 0; i < 4; i++) {
    		int c = Integer.compare(Integer.parseInt(pa[i]), Integer.parseInt(pb[i]));
    		if (c != 0) {
    			return c;
    		}
    	}
    	return 0;
    }

}*/