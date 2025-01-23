import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1"; // Dirección del servidor
    private static final int SERVER_PORT = 5555; // Puerto del servidor
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    private static int clicks = 0;
    private static JButton clickButton;
    private static JFrame frame;
    private static JLabel statusLabel;

    public static void main(String[] args) {
        // Iniciar la interfaz de autenticación cuando se ejecute el programa
        SwingUtilities.invokeLater(Client::createAuthUI);
    }

    // Crear la interfaz de autenticación
    private static void createAuthUI() {
        frame = new JFrame("Autenticación");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(3, 2));
        JLabel userLabel = new JLabel("Usuario:");
        JTextField usernameField = new JTextField();
        JLabel passLabel = new JLabel("Contraseña:");
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Iniciar sesión");

        panel.add(userLabel);
        panel.add(usernameField);
        panel.add(passLabel);
        panel.add(passwordField);
        panel.add(new JLabel()); // Celda vacía
        panel.add(loginButton);

        frame.add(panel, BorderLayout.CENTER);

        // Acción al hacer clic en el botón de login
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            try {
                // Conectar al servidor
                socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Enviar las credenciales al servidor
                out.println(username);
                out.println(password);

                // Leer la respuesta del servidor
                String serverResponse = in.readLine(); // Leer la respuesta del servidor
                System.out.println("Respuesta del servidor: " + serverResponse); // Depuración

                // Si la autenticación es exitosa, iniciar el juego
                if ("Autenticación exitosa.".equals(serverResponse)) {
                    SwingUtilities.invokeLater(Client::createGameUI); // Iniciar juego
                } else {
                    JOptionPane.showMessageDialog(frame, "Credenciales incorrectas. Intenta de nuevo.");
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error de conexión con el servidor.", "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        frame.setVisible(true);
    }

    // Crear la interfaz de juego
    private static void createGameUI() {
        frame.setTitle("Juego de Clics");
        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());

        statusLabel = new JLabel("Esperando a que inicie el juego...", SwingConstants.CENTER);
        frame.add(statusLabel, BorderLayout.CENTER);

        clickButton = new JButton("Haz clic!");
        clickButton.setEnabled(false);
        clickButton.addActionListener(e -> {
            if (clicks < 5) {
                clicks++;
                out.println("Clic");
                if (clicks == 5) {
                    clickButton.setEnabled(false);
                }
            }
        });

        frame.add(clickButton, BorderLayout.SOUTH);
        frame.setVisible(true);

        // Hilo para recibir mensajes del servidor
        new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    if (serverMessage.contains("¡Ganaste!")) {
                        JOptionPane.showMessageDialog(null, "¡Ganaste!", "Juego Terminado", JOptionPane.INFORMATION_MESSAGE);
                        clickButton.setEnabled(false);
                    } else if (serverMessage.contains("¡Juego comenzado!")) {
                        statusLabel.setText("");
                        clickButton.setEnabled(true);
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error de conexión con el servidor.", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            } finally {
                closeConnection();
            }
        }).start();
    }

    // Cerrar la conexión con el servidor
    private static void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
