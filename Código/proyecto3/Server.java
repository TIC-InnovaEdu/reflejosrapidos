import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {
    private static final int PORT = 5555;
    private static HashMap<PrintWriter, Integer> clientProgressMap = new HashMap<>();
    private static HashMap<PrintWriter, JLabel> clientProgressLabelMap = new HashMap<>();
    private static HashMap<PrintWriter, JProgressBar> clientProgressBarMap = new HashMap<>();
    private static HashMap<PrintWriter, JLabel> clientTimeLabelMap = new HashMap<>();
    private static HashMap<PrintWriter, Long> clientStartTimeMap = new HashMap<>();
    private static ArrayList<PrintWriter> clientWriters = new ArrayList<>();
    private static JPanel clientPanel;
    private static JFrame frame;
    private static JButton startButton;

    public static void main(String[] args) {
        System.out.println("Servidor iniciado...");
        
        // Crear la interfaz gráfica del servidor
        frame = new JFrame("Servidor de Juego");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        // Etiqueta de inicio de juego con estilo sombreado
        JLabel statusLabel = new JLabel("¡Juego interactivo de reflejos rápidos!", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBackground(Color.BLACK);
        statusLabel.setOpaque(true);
        frame.add(statusLabel, BorderLayout.CENTER);

        // Panel para los clientes y su progreso
        clientPanel = new JPanel();
        clientPanel.setLayout(new BoxLayout(clientPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(clientPanel);
        frame.add(scrollPane, BorderLayout.EAST);

        frame.setVisible(true);

        // Conectar al cliente
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Esperando cliente...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado: " + socket.getRemoteSocketAddress());

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Agregar el cliente a la lista de clientes conectados
                clientWriters.add(out);

                // Crear la etiqueta de nombre para el cliente
                JLabel progressLabel = new JLabel("Cliente " + socket.getRemoteSocketAddress() + " - Progreso:");
                clientPanel.add(progressLabel);
                
                // Crear una barra de progreso para el cliente
                JProgressBar progressBar = new JProgressBar(0, 100);
                progressBar.setValue(0);
                progressBar.setStringPainted(true);
                clientPanel.add(progressBar);

                // Crear una etiqueta para mostrar el tiempo total en segundos
                JLabel timeLabel = new JLabel("Tiempo: 0.0 segundos");
                clientPanel.add(timeLabel);

                frame.revalidate();  // Actualizar el panel

                clientProgressMap.put(out, 0);
                clientProgressLabelMap.put(out, progressLabel);
                clientProgressBarMap.put(out, progressBar);
                clientTimeLabelMap.put(out, timeLabel);

                // Para registrar el tiempo de inicio de cada cliente
                clientStartTimeMap.put(out, System.currentTimeMillis());

                // Habilitar el botón "Iniciar Juego" solo después de que se conecte al menos un cliente
                if (clientWriters.size() > 0) {
                    startButton = new JButton("Iniciar Juego");
                    startButton.setEnabled(true);
                    frame.add(startButton, BorderLayout.NORTH);
                    startButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                // Enviar mensaje de inicio de juego a todos los clientes
                                for (PrintWriter writer : clientWriters) {
                                    writer.println("¡Juego comenzado! Haz clic 5 veces.");
                                }
                                statusLabel.setText("¡Juego en progreso!");
                                startButton.setEnabled(false); // Deshabilitar el botón después de iniciar
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                    frame.revalidate();
                }

                // Crear un nuevo hilo para manejar la conexión del cliente
                new ClientHandler(socket, out, in).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clase para manejar la comunicación con cada cliente en un hilo independiente
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
                        int currentProgress = clientProgressMap.get(out) + 1;
                        clientProgressMap.put(out, currentProgress);

                        // Actualizar el progreso en el servidor
                        SwingUtilities.invokeLater(() -> {
                            JProgressBar progressBar = clientProgressBarMap.get(out);
                            int progress = currentProgress * 20; // Cada clic aumenta el progreso en un 20%
                            progressBar.setValue(progress);

                            // Actualizar el tiempo transcurrido en segundos y milisegundos
                            long startTime = clientStartTimeMap.get(out);
                            long elapsedTime = (System.currentTimeMillis() - startTime); // en milisegundos
                            double seconds = elapsedTime / 1000.0;
                            JLabel timeLabel = clientTimeLabelMap.get(out);
                            timeLabel.setText(String.format("Tiempo: %.2f segundos", seconds));

                            // Si el cliente alcanza los 5 clics, termina el juego
                            if (currentProgress == 5) {
                                out.println("¡Ganaste!");
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
