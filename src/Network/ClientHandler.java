package Network;

import java.net.*;
import java.io.*;

public class ClientHandler extends Thread {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.username = "User" + (int)(Math.random() * 1000);
    }

    public void send(String msg) {
        out.println(msg);
    }

    public void run() {
        try {
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            out = new PrintWriter(
                    socket.getOutputStream(), true
            );

            Server.broadcast("🔌 " + username + " đã vào");

            String msg;

            while ((msg = in.readLine()) != null) {

                String[] parts = msg.split(" ");

                switch (parts[0]) {

                    case "CREATE":
                        String item = parts[1];
                        String price = parts[2];

                        Server.broadcast("📦 Auction: " + item + " giá " + price);
                        break;

                    case "BID":
                        int amount = Integer.parseInt(parts[1]);

                        Server.broadcast("🔥 " + username + " bid " + amount);
                        break;

                    case "EXIT":
                        Server.clients.remove(this);
                        Server.broadcast("❌ " + username + " rời");
                        socket.close();
                        return;

                    default:
                        send("Sai lệnh!");
                }
            }

        } catch (Exception e) {
            System.out.println(username + " mất kết nối");
        }
    }
}