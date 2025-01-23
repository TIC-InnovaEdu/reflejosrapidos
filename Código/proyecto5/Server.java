import java.awt.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import javax.swing.*;

public class Server {
    private static final int PORT = 5555;
    private static Connection conn;

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
        
        // Crear la interfaz gráfica del servidor
        JFrame frame = new JFrame("Servidor de Juego");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        JLabel statusLabel = new JLabel("¡Juego interactivo de reflejos rápidos!", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBackground(Color.BLACK);
        statusLabel.setOpaque(true);
        frame.add(statusLabel, BorderLayout.CENTER);

        frame.setVisible(true);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Esperando cliente...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado: " + socket.getRemoteSocketAddress());

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Pedir las credenciales al cliente
                out.println("Introduce tu usuario:");
                String username = in.readLine(); // Leer nombre de usuario
                out.println("Introduce tu contraseña:");
                String password = in.readLine(); // Leer contraseña

                // Validar las credenciales
                if (authenticateUser(username, password)) {
                    // Si la autenticación es exitosa, iniciar el juego
                    out.println("Autenticación exitosa.");
                    new ClientHandler(socket, out, in).start(); // Crear un hilo para manejar la comunicación con el cliente
                } else {
                    // Si la autenticación falla, cerrar la conexión
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
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();

            // Verificar si la consulta ha devuelto un resultado
            if (rs.next()) {
                System.out.println("Usuario y contraseña correctos.");
                return true; // Si encuentra el usuario y la contraseña, la autenticación es exitosa
            } else {
                System.out.println("Usuario o contraseña incorrectos.");
                return false; // Si no encuentra un registro que coincida
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Si ocurre algún error, autenticación fallida
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket, PrintWriter out, BufferedReader in) {
            this.socket = socket;
            this.out = out;
            this.in = in;
        }

        @Override
        public void run() {
            try {
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if ("Clic".equals(clientMessage)) {
                        // Aquí puedes agregar la lógica para manejar los clics y la parte del juego
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
