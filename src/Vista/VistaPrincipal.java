package Vista;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;

import controlador.ControlScanner;
import modelo.Dispositivos;

@SuppressWarnings("serial")
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

	private ControlScanner controlador;
	private List<Dispositivos> resultados = new ArrayList<>();
	private boolean escaneando = false;

	// Paleta de Colores
	private final Color COLOR_BG = new Color(24, 25, 32);
	private final Color COLOR_CARD = new Color(33, 35, 45);
	private final Color COLOR_TEXT_PRIMARY = new Color(255, 255, 255); 
	private final Color COLOR_ACCENT = new Color(88, 101, 242);
	private final Color COLOR_DANGER = new Color(220, 53, 69);
	private final Color COLOR_SUCCESS = new Color(40, 167, 69);
	private final Color COLOR_INPUT_BG = new Color(20, 21, 27);
	private final Color COLOR_BORDER = new Color(50, 54, 70);

	public VistaPrincipal() {
		controlador = new ControlScanner();

		setTitle("Escáner de Red LAN - TP Redes");
		setSize(920, 620);
		setMinimumSize(new Dimension(850, 550));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		getContentPane().setBackground(COLOR_BG);
		setLayout(new BorderLayout(15, 15));
		((JPanel) getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

		// ==================== PANEL NORTE ====================
		JPanel jpNorte = new JPanel();
		jpNorte.setLayout(new BoxLayout(jpNorte, BoxLayout.Y_AXIS));
		jpNorte.setOpaque(false);

		// Tarjeta de Formulario
		JPanel jpFormulario = crearPanelTarjeta();
		jpFormulario.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));

		jpFormulario.add(crearEtiqueta("IP Subred:"));
		txtIp = crearTextField("192.168.1", 8);
		jpFormulario.add(txtIp);

		jpFormulario.add(crearEtiqueta("Desde:"));
		txtInicioIp = crearTextField("1", 3);
		jpFormulario.add(txtInicioIp);

		jpFormulario.add(crearEtiqueta("Hasta:"));
		txtFinIp = crearTextField("20", 3);
		jpFormulario.add(txtFinIp);

		jpFormulario.add(crearEtiqueta("Tiempo Máx (ms):"));
		txtTiempoMax = crearTextField("1000", 4);
		jpFormulario.add(txtTiempoMax);

		btnEscanear = crearBoton("Escanear", COLOR_ACCENT);
		jpFormulario.add(btnEscanear);

		// Tarjeta de Filtros
		JPanel jpBotones = crearPanelTarjeta();
		jpBotones.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));

		btnLimpiar = crearBoton("Limpiar", new Color(200, 205, 215));
		jpBotones.add(btnLimpiar);

		btnGuardar = crearBoton("Guardar CSV", COLOR_SUCCESS);
		jpBotones.add(btnGuardar);

		jpBotones.add(Box.createHorizontalStrut(15));
		jpBotones.add(crearEtiqueta("Buscar:"));
		txtFiltro = crearTextField("", 10);
		jpBotones.add(txtFiltro);

		jpBotones.add(crearEtiqueta("Estado:"));
		cmbEstado = new JComboBox<>(new String[] {"Todos", "Activos", "Inactivos"});
		estilarComboBox(cmbEstado);
		jpBotones.add(cmbEstado);

		// Mensaje de estado
		lblMensaje = new JLabel(" ");
		lblMensaje.setFont(new Font("Segoe UI", Font.BOLD, 12));
		lblMensaje.setBorder(new EmptyBorder(5, 5, 5, 5));

		jpNorte.add(jpFormulario);
		jpNorte.add(Box.createVerticalStrut(10));
		jpNorte.add(jpBotones);
		jpNorte.add(Box.createVerticalStrut(5));
		jpNorte.add(lblMensaje);

		add(jpNorte, BorderLayout.NORTH);

		// ==================== TABLA CENTRO ====================
		String[] columnas = {"IP", "Nombre Dispositivo", "Estado", "Tiempo Respuesta (ms)"};
		modeloTabla = new DefaultTableModel(columnas, 0) {
			@Override
			public boolean isCellEditable(int fila, int columna) {
				return false;
			}

			@Override
			public Class<?> getColumnClass(int columna) {
				return columna == 3 ? Long.class : String.class;
			}
		};
		tablaResultados = new JTable(modeloTabla);
		estilarTabla(tablaResultados);

		sorter = new TableRowSorter<>(modeloTabla);
		sorter.setComparator(0, (a, b) -> compararIp((String) a, (String) b));
		tablaResultados.setRowSorter(sorter);

		JScrollPane scrollTabla = new JScrollPane(tablaResultados);
		scrollTabla.getViewport().setBackground(COLOR_CARD);
		scrollTabla.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
		add(scrollTabla, BorderLayout.CENTER);

		// ==================== PANEL INFERIOR ====================
		JPanel panelInferior = new JPanel(new BorderLayout(10, 8));
		panelInferior.setOpaque(false);

		barraProgreso = new JProgressBar();
		barraProgreso.setStringPainted(true);
		barraProgreso.setPreferredSize(new Dimension(barraProgreso.getPreferredSize().width, 22));
		barraProgreso.setBackground(COLOR_CARD);
		barraProgreso.setForeground(COLOR_ACCENT);
		barraProgreso.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

		lblTotal = new JLabel("Equipos que respondieron: 0");
		lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 13));
		lblTotal.setForeground(COLOR_TEXT_PRIMARY);

		panelInferior.add(barraProgreso, BorderLayout.CENTER);
		panelInferior.add(lblTotal, BorderLayout.SOUTH);

		add(panelInferior, BorderLayout.SOUTH);

		// ==================== EVENTOS ====================
		DocumentListener validar = alCambiar(this::validarCampos);
		txtIp.getDocument().addDocumentListener(validar);
		txtInicioIp.getDocument().addDocumentListener(validar);
		txtFinIp.getDocument().addDocumentListener(validar);
		txtTiempoMax.getDocument().addDocumentListener(validar);

		txtFiltro.getDocument().addDocumentListener(alCambiar(this::filtrar));
		cmbEstado.addActionListener(e -> filtrar());

		btnEscanear.addActionListener(e -> escanear());
		btnLimpiar.addActionListener(e -> limpiar());
		btnGuardar.addActionListener(e -> guardar());

		validarCampos();
	}

	// ==================== ESTILOS Y COMPONENTES ====================

	private JPanel crearPanelTarjeta() {
		JPanel panel = new JPanel();
		panel.setBackground(COLOR_CARD);
		panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
				new EmptyBorder(5, 10, 5, 10)
		));
		return panel;
	}

	private JLabel crearEtiqueta(String texto) {
		JLabel label = new JLabel(texto);
		label.setForeground(new Color(255, 255, 255));
		label.setFont(new Font("Segoe UI", Font.BOLD, 13));
		return label;
	}

	private JTextField crearTextField(String textoInicial, int columnas) {
		JTextField tf = new JTextField(textoInicial, columnas);
		tf.setBackground(COLOR_INPUT_BG);
		tf.setForeground(Color.WHITE);
		tf.setCaretColor(Color.WHITE);
		tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		tf.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(COLOR_BORDER, 1),
				new EmptyBorder(4, 6, 4, 6)
		));
		return tf;
	}

	// BOTONES CON TEXTO NEGRO SOBRE FONDO CLARO/BOTÓN
	private JButton crearBoton(String texto, Color bg) {
		JButton btn = new JButton(texto);
		btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
		btn.setForeground(Color.BLACK); // Texto negro para legibilidad
		btn.setBackground(bg);
		btn.setFocusPainted(false);
		btn.setBorder(new EmptyBorder(6, 14, 6, 14));
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		return btn;
	}

	// COMBOBOX CON TEXTO NEGRO SOBRE FONDO BLANCO/GRIS
	private void estilarComboBox(JComboBox<String> combo) {
		combo.setBackground(Color.WHITE);
		combo.setForeground(Color.BLACK); // Texto negro
		combo.setFont(new Font("Segoe UI", Font.BOLD, 12));
		combo.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
	}

	// TABLA CON ENCABEZADO DE TEXTO NEGRO SOBRE FONDO BLANCO
	private void estilarTabla(JTable tabla) {
		tabla.setBackground(COLOR_CARD);
		tabla.setForeground(COLOR_TEXT_PRIMARY);
		tabla.setGridColor(COLOR_BORDER);
		tabla.setRowHeight(28);
		tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		tabla.setSelectionBackground(COLOR_ACCENT);
		tabla.setSelectionForeground(Color.WHITE);

		JTableHeader header = tabla.getTableHeader();
		header.setBackground(Color.WHITE);
		header.setForeground(Color.BLACK); // Texto negro en los títulos de la tabla
		header.setFont(new Font("Segoe UI", Font.BOLD, 12));
		header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
		header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));

		tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if (!isSelected) {
					c.setBackground(row % 2 == 0 ? COLOR_CARD : new Color(28, 30, 39));
					if (column == 2 && value != null) {
						if (value.toString().equals("Activo")) {
							c.setForeground(new Color(46, 204, 113));
						} else {
							c.setForeground(COLOR_DANGER);
						}
					} else {
						c.setForeground(COLOR_TEXT_PRIMARY);
					}
				}
				return c;
			}
		});
	}

	// ==================== LÓGICA DE EVENTOS ====================

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

	private void validarCampos() {
		String error = controlador.validarDatos(txtIp.getText(), txtInicioIp.getText(),
				txtFinIp.getText(), txtTiempoMax.getText());

		boolean subredMal = !txtIp.getText().isEmpty() && !controlador.esSubredValida(txtIp.getText());
		txtIp.setBackground(subredMal ? new Color(70, 20, 25) : COLOR_INPUT_BG);

		if (error == null) {
			lblMensaje.setForeground(new Color(46, 204, 113));
			lblMensaje.setText("Datos correctos. Listo para escanear.");
		} else {
			lblMensaje.setForeground(COLOR_DANGER);
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
		lblMensaje.setForeground(new Color(200, 205, 220));
		lblMensaje.setText("Escaneando la red...");
		barraProgreso.setMaximum(fin - inicio + 1);

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
					lblMensaje.setForeground(COLOR_SUCCESS);
					lblMensaje.setText("Escaneo finalizado correctamente.");
				} catch (Exception ex) {
					lblMensaje.setForeground(COLOR_DANGER);
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

	private void mostrarResultados() {
		resultados.sort((a, b) -> compararIp(a.getIp(), b.getIp()));
		int activos = 0;
		for (Dispositivos d : resultados) {
			modeloTabla.addRow(new Object[] {
					d.getIp(),
					d.getNombre(),
					d.isConectado() ? "Activo" : "Inactivo",
					d.isConectado() ? Long.valueOf(d.getTiempo()) : null });
			if (d.isConectado()) {
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
		selector.setSelectedFile(new java.io.File("resultados_escaneo.csv"));
		selector.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
		if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		java.io.File archivo = selector.getSelectedFile();
		if (!archivo.getName().toLowerCase().endsWith(".csv")) {
			archivo = new java.io.File(archivo.getParentFile(), archivo.getName() + ".csv");
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
}