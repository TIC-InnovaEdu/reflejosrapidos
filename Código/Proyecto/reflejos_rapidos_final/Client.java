import java.awt.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Random;
import javax.swing.*;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1"; // Dirección del servidor
    private static final int SERVER_PORT = 5555; // Puerto del servidor
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    private static int clicks = 0;
    private static final int MAX_CLICKS = 5;
    private static JButton redButton;
    private static JFrame frame;
    private static JTextField usernameField;
    private static JPasswordField passwordField;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::createAuthUI);
    }

    private static void createAuthUI() {
        frame = new JFrame("Autenticación");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null); // Centra la ventana
        frame.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Espaciado

        JLabel userLabel = new JLabel("Usuario:");
        JLabel passLabel = new JLabel("Contraseña:");
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        JButton loginButton = new JButton("Iniciar sesión");
        JButton registerButton = new JButton("Registrar");

        // Estilo de los labels
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));
        passLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userLabel.setForeground(Color.WHITE);
        passLabel.setForeground(Color.WHITE);

        // Estilo de los botones
        loginButton.setFont(new Font("Arial", Font.PLAIN, 12));
        loginButton.setPreferredSize(new Dimension(130, 30));
        registerButton.setFont(new Font("Arial", Font.PLAIN, 12));
        registerButton.setPreferredSize(new Dimension(130, 30));

        // Estilo del panel
        panel.setBackground(Color.DARK_GRAY);

        // Ubicar los componentes
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(userLabel, gbc);

        gbc.gridx = 1;
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(passLabel, gbc);

        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(loginButton, gbc);

        gbc.gridx = 1;
        panel.add(registerButton, gbc);

        frame.add(panel, BorderLayout.CENTER);

        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> showRegistrationForm());

        frame.setVisible(true);
    }

    private static void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(username); // Enviar usuario
            out.println(password); // Enviar contraseña

            String serverResponse = in.readLine(); // Leer respuesta del servidor
            System.out.println("Respuesta del servidor: " + serverResponse);

            if ("Autenticación exitosa.".equals(serverResponse)) {
                JOptionPane.showMessageDialog(frame, "Bienvenido " + username);
                SwingUtilities.invokeLater(Client::createGameUI); // Después de la autenticación exitosa, crear la interfaz del juego
            } else {
                JOptionPane.showMessageDialog(frame, "Credenciales incorrectas. Intenta de nuevo.");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error de conexión con el servidor.", "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private static void showRegistrationForm() {
        JFrame registerFrame = new JFrame("Registro de Usuario");
        registerFrame.setSize(400, 300);
        registerFrame.setLocationRelativeTo(frame);
        registerFrame.setLayout(new BorderLayout());

        JPanel registerPanel = new JPanel();
        registerPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Espaciado

        JLabel registerUserLabel = new JLabel("Usuario:");
        JLabel registerPassLabel = new JLabel("Contraseña:");
        JTextField registerUsernameField = new JTextField(15);
        JPasswordField registerPasswordField = new JPasswordField(15);
        JButton saveButton = new JButton("Registrar");

        // Estilo de los labels
        registerUserLabel.setFont(new Font("Arial", Font.BOLD, 14));
        registerPassLabel.setFont(new Font("Arial", Font.BOLD, 14));
        registerUserLabel.setForeground(Color.BLACK);
        registerPassLabel.setForeground(Color.BLACK);

        // Estilo de los botones
        saveButton.setFont(new Font("Arial", Font.PLAIN, 12));
        saveButton.setPreferredSize(new Dimension(100, 30));

        // Estilo del panel
        registerPanel.setBackground(Color.LIGHT_GRAY);

        // Ubicar los componentes
        gbc.gridx = 0;
        gbc.gridy = 0;
        registerPanel.add(registerUserLabel, gbc);

        gbc.gridx = 1;
        registerPanel.add(registerUsernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        registerPanel.add(registerPassLabel, gbc);

        gbc.gridx = 1;
        registerPanel.add(registerPasswordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        registerPanel.add(saveButton, gbc);

        registerFrame.add(registerPanel, BorderLayout.CENTER);

        saveButton.addActionListener(e -> saveUser(registerUsernameField.getText(), new String(registerPasswordField.getPassword()), registerFrame));

        registerFrame.setVisible(true);
    }

    private static void saveUser(String username, String password, JFrame registerFrame) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/juego_autenticacion", "root", "1234")) {
            String query = "INSERT INTO usuarios (username, password) VALUES (?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setString(1, username);
                pst.setString(2, password);
                pst.executeUpdate();
                JOptionPane.showMessageDialog(registerFrame, "Usuario registrado con éxito.");
                registerFrame.dispose(); // Cerrar el formulario de registro
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(registerFrame, "Error al registrar el usuario.");
        }
    }

    private static void createGameUI() {
        frame.setTitle("Juego de Reflejos Rápidos");
        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());

        JLabel statusLabel = new JLabel("Esperando a que inicie el juego...", SwingConstants.CENTER);
        frame.add(statusLabel, BorderLayout.NORTH);

        JProgressBar progressBar = new JProgressBar(0, MAX_CLICKS);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        frame.add(progressBar, BorderLayout.SOUTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(null);
        buttonPanel.setBackground(Color.WHITE);
        frame.add(buttonPanel, BorderLayout.CENTER);

        Random random = new Random();
        JButton[] buttons = new JButton[10];

        // Crear los 10 botones
        for (int i = 0; i < 10; i++) {
            JButton button = new JButton();
            button.setBounds(random.nextInt(300), random.nextInt(200), 50, 50);

            if (i == 0) {
                button.setBackground(Color.RED);
                redButton = button;
            } else {
                button.setBackground(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            }

            button.addActionListener(e -> {
                if (button == redButton && clicks < MAX_CLICKS) {
                    clicks++;
                    progressBar.setValue(clicks);
                    out.println("Clic");

                  //  if (clicks == MAX_CLICKS) {
                    //    JOptionPane.showMessageDialog(frame, "¡Has ganado!", "Juego Terminado", JOptionPane.INFORMATION_MESSAGE);
                      //  out.println("Fin del juego");
                        //closeConnection();
                      //  System.exit(0);
                   // }
                }
            });

            buttonPanel.add(button);
            buttons[i] = button;
        }

        // Agregar Timer para mover botones cada 700 ms
        Timer moveButtonsTimer = new Timer(750, e -> moveButtonsRandomly(random, buttons));
        moveButtonsTimer.start();

        frame.setVisible(true);

        // Iniciar un hilo para recibir mensajes del servidor
        new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println("Mensaje del servidor: " + serverMessage);
                    if (serverMessage.contains("¡Ganaste!")) {
                        JOptionPane.showMessageDialog(null, serverMessage, "Juego Terminado", JOptionPane.INFORMATION_MESSAGE);
                        closeConnection();
                        System.exit(0);
                    } else if (serverMessage.contains("¡Juego comenzado!")) {
                        statusLabel.setText("¡El juego ha comenzado, Haz clic el color rojo!");
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error de conexión con el servidor.", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }).start();
    }

    private static void moveButtonsRandomly(Random random, JButton[] buttons) {
        for (JButton button : buttons) {
            button.setLocation(random.nextInt(300), random.nextInt(200));
        }
    }

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



