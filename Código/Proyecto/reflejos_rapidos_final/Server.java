import java.awt.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class Server {
    private static final int PORT = 5555;
    private static Connection conn;
    private static JLabel statusLabel;
    private static JProgressBar progressBar;
    private static final int MAX_CLICKS = 5;

    public static void main(String[] args) {
        System.out.println("Servidor iniciado...");

        // Conexión a la base de datos MySQL
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/juego_autenticacion", "root", "1234");
            System.out.println("Conexión exitosa a la base de datos.");
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Configuración de la interfaz gráfica del servidor
        JFrame frame = new JFrame("Servidor de Juego");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null); // Centrar la ventana
        frame.setLayout(new BorderLayout());

        // Configuración del JLabel de estado
        statusLabel = new JLabel("Esperando Jugadores", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 20));
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.LIGHT_GRAY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Espaciado
        frame.add(statusLabel, BorderLayout.NORTH);

        // Barra de progreso
        progressBar = new JProgressBar(0, MAX_CLICKS);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setFont(new Font("Arial", Font.PLAIN, 16));
       // progressBar.setForeground(Color.WHITE);
       // progressBar.setBackground(Color.LIGHT_GRAY);
        progressBar.setBounds(50, 80, 300, 30); // Posición y tamaño de la barra
        frame.add(progressBar, BorderLayout.CENTER);

        // Crear el botón para mostrar el ranking
        JButton rankingButton = new JButton("Mostrar Ranking");
        rankingButton.setBounds(200, 300, 200, 30);
        rankingButton.setBackground(Color.LIGHT_GRAY);
        rankingButton.setFont(new Font("Arial", Font.BOLD, 16));
        rankingButton.addActionListener(e -> showRanking(frame)); // Acción del botón
        frame.add(rankingButton, BorderLayout.SOUTH);

        frame.setVisible(true);

        // Iniciar el servidor para aceptar clientes
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Esperando cliente...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado: " + socket.getRemoteSocketAddress());

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String username = in.readLine();
                String password = in.readLine();

                if (authenticateUser(username, password)) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Usuario conectado: " + username));
                    out.println("Autenticación exitosa.");
                    out.println("¡Juego comenzado!");
                    new ClientHandler(socket, out, in, username, frame).start();
                } else {
                    out.println("Autenticación fallida. Cerrando conexión.");
                    socket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean authenticateUser(String username, String password) {
        try {
            String query = "SELECT * FROM usuarios WHERE username = ? AND password = ?";
            try (PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setString(1, username);
                pst.setString(2, password);
                try (ResultSet rs = pst.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void updateBestTime(String username, long duration) {
        String query = "UPDATE usuarios SET mejor_tiempo = ? WHERE username = ? AND (mejor_tiempo IS NULL OR mejor_tiempo > ?)";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setLong(1, duration);
            pst.setString(2, username);
            pst.setLong(3, duration);
            int rowsUpdated = pst.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Mejor tiempo actualizado para el usuario: " + username + " - Tiempo: " + duration + " ms");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void showRanking(JFrame frame) {
        String query = "SELECT username, mejor_tiempo FROM usuarios WHERE mejor_tiempo IS NOT NULL ORDER BY mejor_tiempo ASC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            // Crear modelo de tabla
            DefaultTableModel model = new DefaultTableModel(new Object[]{"#","Usuario", "Mejor Tiempo (ms)"}, 0);

            int rank = 1;
            while (rs.next()) {
                String username = rs.getString("username");
                long mejorTiempo = rs.getLong("mejor_tiempo");
                model.addRow(new Object[]{rank++, username, mejorTiempo});
            }

            // Crear JTable y mostrarla en una ventana
            JTable table = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(table);
            table.setFillsViewportHeight(true);
            JFrame rankingFrame = new JFrame("Ranking de Jugadores");
            rankingFrame.setSize(600, 400);
            rankingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            rankingFrame.add(scrollPane);
            rankingFrame.setVisible(true);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;
        private final String username;
        private int clicks;
        private long startTime;
        private final JFrame frame;

        public ClientHandler(Socket socket, PrintWriter out, BufferedReader in, String username, JFrame frame) {
            this.socket = socket;
            this.out = out;
            this.in = in;
            this.username = username;
            this.clicks = 0;
            this.startTime = 0;
            this.frame = frame;
        }

        @Override
        public void run() {
            try {
                String clientMessage;

                // Comienza el juego
                out.println("¡El juego ha comenzado!");
                startTime = System.currentTimeMillis(); // Registrar el tiempo al iniciar el juego

                while ((clientMessage = in.readLine()) != null) {
                    if ("Clic".equals(clientMessage)) {
                        clicks++;
                        System.out.println("Clic recibido de " + username + ". Total: " + clicks);

                        // Actualizar barra de progreso
                        int progress = (int) ((clicks / (float) MAX_CLICKS) * 100);
                        SwingUtilities.invokeLater(() -> progressBar.setValue(clicks));

                        out.println("Progreso: " + progress + "%");

                        if (clicks >= MAX_CLICKS) {
                            long endTime = System.currentTimeMillis();
                            long duration = endTime - startTime;
                            out.println("¡Ganaste! El tiempo total fue: " + duration + " milisegundos.");

                            // Actualizar el mejor tiempo en la base de datos
                            updateBestTime(username, duration);

                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}





