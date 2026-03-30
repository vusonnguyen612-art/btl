package Network;

import java.net.*;
import java.io.*;
import Service.*;
import Model.*;

public class ClientHandler extends Thread {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            out = new PrintWriter(socket.getOutputStream(), true);

            String msg;

            while ((msg = in.readLine()) != null) {

                String[] parts = msg.split(" ");

                switch (parts[0]) {

                    case "CREATE":
                        String itemName = parts[1];
                        int price = Integer.parseInt(parts[2]);

                        Item item = new Item(itemName, "", price);
                        Server.auctionService.createAuction(item);

                        out.println("Tạo auction OK");
                        break;

                    case "BID":
                        int amount = Integer.parseInt(parts[1]);

                        boolean ok = Server.auctionService.placeBid(
                                socket.getInetAddress().toString(),
                                amount
                        );

                        if (ok) out.println("Bid OK");
                        else out.println("Bid fail");
                        break;

                    case "END":
                        Server.auctionService.endAuction();
                        out.println("Auction ended");
                        break;

                    default:
                        out.println("Unknown command");
                }
            }

        } catch (Exception e) {
            System.out.println("Client rời");
        }
    }
}
