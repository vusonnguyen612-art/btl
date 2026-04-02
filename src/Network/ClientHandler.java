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

            Server.broadcast("🔌 " + username + " vào");

            String msg;

            while ((msg = in.readLine()) != null) {

                String[] parts = msg.split(" ");

                switch (parts[0]) {

                    case "CREATE":
                        String item = parts[1];
                        int price = Integer.parseInt(parts[2]);

                        Server.auctionService.createAuction(item, price);

                        Server.broadcast("📦 Auction: " + item + " | Giá: " + price);
                        break;

                    case "BID":
                        int amount = Integer.parseInt(parts[1]);

                        String result = Server.auctionService.placeBid(username, amount);

                        Server.broadcast(result);
                        break;

                    case "END":
                        String res = Server.auctionService.endAuction();

                        Server.broadcast(res);
                        break;

                    default:
                        send("❌ Sai lệnh");
                }
            }

        } catch (Exception e) {
            System.out.println(username + " rời");
        }
    }
}