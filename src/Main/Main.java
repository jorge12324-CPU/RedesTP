package Main;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import Vista.VistaPrincipal;

public class Main {
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
		SwingUtilities.invokeLater(() -> new VistaPrincipal().setVisible(true));
	}
}

//esto va desp
/**package principal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import vista.VistaPrincipal;


public class Main {
	public static void main(String[] args) {
		try {
			// Usa el aspecto nativo del sistema operativo
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// Si falla, se usa el aspecto por defecto de Java
		}
		SwingUtilities.invokeLater(() -> new VistaPrincipal().setVisible(true));
	}
}
**/