import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Random;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 5555;
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    private static int clicks = 0;
    private static JButton clickButton;

    private static JFrame frame;
    private static JLabel statusLabel;

    public static void main(String[] args) {
        try {
            // Conectar al servidor
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Crear la interfaz gráfica
            SwingUtilities.invokeLater(Client::createUI);

            // Leer mensajes del servidor
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                System.out.println(serverMessage);
                if (serverMessage.contains("¡Ganaste!")) {
                    JOptionPane.showMessageDialog(null, "¡Ganaste!", "Juego Terminado", JOptionPane.INFORMATION_MESSAGE);
                    clickButton.setEnabled(false); // Deshabilitar botón después de ganar
                    break;
                } else if (serverMessage.contains("¡Perdiste!")) {
                    JOptionPane.showMessageDialog(null, "¡Perdiste!", "Juego Terminado", JOptionPane.INFORMATION_MESSAGE);
                    clickButton.setEnabled(false); // Deshabilitar botón después de perder
                    break;
                } else if (serverMessage.contains("¡Juego comenzado!")) {
                    // Habilitar el botón "Haz clic!" cuando el servidor lo indique
                    statusLabel.setText("");
                    clickButton.setEnabled(true);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createUI() {
        frame = new JFrame("Juego de Clics");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        // Etiqueta de espera
        statusLabel = new JLabel("Esperando a que inicie el juego...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setForeground(Color.RED);
        frame.add(statusLabel, BorderLayout.CENTER);

        // Botón que aparece después de hacer clic en "Jugar"
        clickButton = new JButton("Haz clic!");
        clickButton.setEnabled(false); // Inicialmente deshabilitado
        clickButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (clicks < 5) {
                    clicks++;
                    out.println("Clic");
                    moveButton();
                    if (clicks == 5) {
                        clickButton.setEnabled(false);
                    }
                }
            }
        });

        frame.add(clickButton, BorderLayout.NORTH);

        frame.setVisible(true);
    }

    // Función para mover el botón "Haz clic!" a una posición aleatoria
    private static void moveButton() {
        Random rand = new Random();
        int x = rand.nextInt(frame.getWidth() - clickButton.getWidth());
        int y = rand.nextInt(frame.getHeight() - clickButton.getHeight());
        clickButton.setBounds(x, y, 100, 50);
    }
}
