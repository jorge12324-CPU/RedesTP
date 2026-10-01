package Vista;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;

import controlador.ControlScanner;
import modelo.Dispositivos;

@SuppressWarnings("serial")
public class VistaPrincipal extends JFrame {
	private JTextField txtIpInicio;
	private JTextField txtIpFin;
	private JTextField txtTiempoMax;
	private JTextField txtReintentos;

	private JButton btnIniciar;
	private JButton btnDetener;
	private JButton btnLimpiar;
	private JButton btnGuardar;
	private JButton btnMostrarActivos;

	private JLabel lblEstadoEscaneo;
	private JLabel lblEquiposActivos;

	private JTable tablaResultados;
	private DefaultTableModel modeloTabla;
	private TableRowSorter<DefaultTableModel> sorter;

	private ControlScanner controlador;
	private List<Dispositivos> resultados = new ArrayList<>();
	private SwingWorker<List<Dispositivos>, Void> workerEscaneo;

	private boolean soloActivos = false;

	// Colores similares a la interfaz Swing clásica celeste
	private final Color COLOR_FONDO_SUPERIOR = new Color(225, 245, 254);
	private final Color COLOR_FONDO_GENERAL = new Color(238, 238, 238);
	private final Color COLOR_BARRA_ESTADO = new Color(160, 185, 215);
	private final Color COLOR_BORDE_INPUT = new Color(51, 153, 255);

	public VistaPrincipal() {
		controlador = new ControlScanner();

		setTitle("Escáner de Red");
		setSize(800, 550);
		setMinimumSize(new Dimension(750, 500));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		setLayout(new BorderLayout());

		// ==================== PANEL NORTE (FORMULARIO) ====================
		JPanel panelNorte = new JPanel(new GridBagLayout());
		panelNorte.setBackground(COLOR_FONDO_SUPERIOR);
		panelNorte.setBorder(new EmptyBorder(10, 15, 10, 15));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3, 5, 3, 5);
		gbc.anchor = GridBagConstraints.WEST;

		// Fila 0: IP Inicio
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		panelNorte.add(crearEtiqueta("IP de inicio:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
		txtIpInicio = crearTextField("10.160.7.223");
		panelNorte.add(txtIpInicio, gbc);

		// Fila 1: IP Fin
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
		panelNorte.add(crearEtiqueta("IP de fin:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
		txtIpFin = crearTextField("10.160.7.233");
		panelNorte.add(txtIpFin, gbc);

		// Fila 2: Tiempo de espera
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
		panelNorte.add(crearEtiqueta("Tiempo de espera (ms):"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
		txtTiempoMax = crearTextField("1000");
		panelNorte.add(txtTiempoMax, gbc);

		// Fila 3: Número de reintentos
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
		panelNorte.add(crearEtiqueta("Número de reintentos:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
		txtReintentos = crearTextField("1");
		panelNorte.add(txtReintentos, gbc);

		add(panelNorte, BorderLayout.NORTH);

		// ==================== TABLA (CENTRO) ====================
		String[] columnas = {"IP", "Nombre equipo", "Activo", "Tiempo (ms)"};
		modeloTabla = new DefaultTableModel(columnas, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				if (columnIndex == 2) return Boolean.class; // Checkbox en columna "Activo"
				if (columnIndex == 3) return Long.class;
				return String.class;
			}
		};

		tablaResultados = new JTable(modeloTabla);
		tablaResultados.setRowHeight(22);
		tablaResultados.setFont(new Font("Tahoma", Font.PLAIN, 12));
		tablaResultados.getTableHeader().setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		// Alineación a la derecha para el Tiempo (ms)
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
		tablaResultados.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

		sorter = new TableRowSorter<>(modeloTabla);
		tablaResultados.setRowSorter(sorter);

		JScrollPane scrollTabla = new JScrollPane(tablaResultados);
		scrollTabla.getViewport().setBackground(Color.WHITE);
		scrollTabla.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		add(scrollTabla, BorderLayout.CENTER);

		// ==================== PANEL INFERIOR ====================
		JPanel panelSur = new JPanel();
		panelSur.setLayout(new BoxLayout(panelSur, BoxLayout.Y_AXIS));
		panelSur.setBackground(COLOR_FONDO_GENERAL);

		// Barra azul indicadora de estado
		JPanel panelBarraEstado = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
		panelBarraEstado.setBackground(COLOR_BARRA_ESTADO);
		lblEstadoEscaneo = new JLabel("Listo");
		lblEstadoEscaneo.setForeground(Color.WHITE);
		lblEstadoEscaneo.setFont(new Font("Tahoma", Font.BOLD, 13));
		panelBarraEstado.add(lblEstadoEscaneo);

		// Contador de activos
		JPanel panelContador = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
		panelContador.setBackground(COLOR_FONDO_SUPERIOR);
		lblEquiposActivos = new JLabel("Equipos activos: 0");
		lblEquiposActivos.setFont(new Font("Tahoma", Font.BOLD, 12));
		panelContador.add(lblEquiposActivos);

		// Botones de acción
		JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
		panelBotones.setBackground(COLOR_FONDO_GENERAL);

		btnIniciar = new JButton("Iniciar escaneo");
		btnDetener = new JButton("Detener escaneo");
		btnLimpiar = new JButton("Limpiar");
		btnGuardar = new JButton("Guardar resultados");
		btnMostrarActivos = new JButton("Mostrar solo activos");

		btnDetener.setEnabled(false);

		panelBotones.add(btnIniciar);
		panelBotones.add(btnDetener);
		panelBotones.add(btnLimpiar);
		panelBotones.add(btnGuardar);
		panelBotones.add(btnMostrarActivos);

		panelSur.add(panelBarraEstado);
		panelSur.add(panelContador);
		panelSur.add(panelBotones);

		add(panelSur, BorderLayout.SOUTH);

		// ==================== EVENTOS ====================
		btnIniciar.addActionListener(e -> iniciarEscaneo());
		btnDetener.addActionListener(e -> detenerEscaneo());
		btnLimpiar.addActionListener(e -> limpiarTabla());
		btnGuardar.addActionListener(e -> guardarResultados());
		btnMostrarActivos.addActionListener(e -> alternarFiltroActivos());
	}

	private JLabel crearEtiqueta(String texto) {
		JLabel label = new JLabel(texto);
		label.setFont(new Font("Tahoma", Font.BOLD, 11));
		return label;
	}

	private JTextField crearTextField(String textoInicial) {
		JTextField tf = new JTextField(textoInicial);
		tf.setFont(new Font("Tahoma", Font.PLAIN, 12));
		tf.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(COLOR_BORDE_INPUT, 1),
				new EmptyBorder(3, 5, 3, 5)
		));
		return tf;
	}

	private void iniciarEscaneo() {
		String ipInicioStr = txtIpInicio.getText().trim();
		String ipFinStr = txtIpFin.getText().trim();
		String msStr = txtTiempoMax.getText().trim();

		// Extraer IP base (ej: 10.160.7) y rangos
		int ultimoPuntoIni = ipInicioStr.lastIndexOf('.');
		int ultimoPuntoFin = ipFinStr.lastIndexOf('.');

		if (ultimoPuntoIni == -1 || ultimoPuntoFin == -1) {
			JOptionPane.showMessageDialog(this, "Las direcciones IP ingresadas no son válidas.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String ipBase = ipInicioStr.substring(0, ultimoPuntoIni);
		int inicioHost = Integer.parseInt(ipInicioStr.substring(ultimoPuntoIni + 1));
		int finHost = Integer.parseInt(ipFinStr.substring(ultimoPuntoFin + 1));
		int tiempo = Integer.parseInt(msStr);

		limpiarTabla();
		btnIniciar.setEnabled(false);
		btnDetener.setEnabled(true);
		lblEstadoEscaneo.setText("Escaneando red...");

		workerEscaneo = new SwingWorker<List<Dispositivos>, Void>() {
			@Override
			protected List<Dispositivos> doInBackground() {
				return controlador.escanearRango(ipBase, inicioHost, finHost, tiempo);
			}

			@Override
			protected void done() {
				if (!isCancelled()) {
					try {
						resultados = get();
						mostrarResultados();
						lblEstadoEscaneo.setText("Escaneo finalizado");
					} catch (Exception ex) {
						lblEstadoEscaneo.setText("Error en el escaneo");
					}
				} else {
					lblEstadoEscaneo.setText("Escaneo detenido");
				}
				btnIniciar.setEnabled(true);
				btnDetener.setEnabled(false);
			}
		};
		workerEscaneo.execute();
	}

	private void detenerEscaneo() {
		if (workerEscaneo != null && !workerEscaneo.isDone()) {
			workerEscaneo.cancel(true);
			btnDetener.setEnabled(false);
			btnIniciar.setEnabled(true);
			lblEstadoEscaneo.setText("Escaneo detenido");
		}
	}

	private void mostrarResultados() {
		modeloTabla.setRowCount(0);
		int activos = 0;
		for (Dispositivos d : resultados) {
			modeloTabla.addRow(new Object[] {
					d.getIp(),
					d.getNombre(),
					d.isConectado(), // Boolean -> Muestra Checkbox tildado
					d.isConectado() ? Long.valueOf(d.getTiempo()) : null
			});
			if (d.isConectado()) {
				activos++;
			}
		}
		lblEquiposActivos.setText("Equipos activos: " + activos);
	}

	private void limpiarTabla() {
		modeloTabla.setRowCount(0);
		resultados.clear();
		lblEquiposActivos.setText("Equipos activos: 0");
		lblEstadoEscaneo.setText("Listo");
	}

	private void alternarFiltroActivos() {
		soloActivos = !soloActivos;
		if (soloActivos) {
			btnMostrarActivos.setText("Mostrar todos");
			sorter.setRowFilter(RowFilter.regexFilter("true", 2));
		} else {
			btnMostrarActivos.setText("Mostrar solo activos");
			sorter.setRowFilter(null);
		}
	}

	private void guardarResultados() {
		if (resultados.isEmpty()) {
			JOptionPane.showMessageDialog(this, "No hay datos para guardar.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));
		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			if (!file.getName().toLowerCase().endsWith(".csv")) {
				file = new File(file.getParentFile(), file.getName() + ".csv");
			}
			try {
				controlador.guardarResultados(resultados, file);
				JOptionPane.showMessageDialog(this, "Guardado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, "Error al guardar el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}