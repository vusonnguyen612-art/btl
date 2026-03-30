package Network;

import java.net.*;
import Service.*;
import Model.*;

public class Server {

    public static AuctionService auctionService;

    public static void main(String[] args) throws Exception {

        ServerSocket server = new ServerSocket(1234);
        System.out.println("Server chạy...");

        auctionService = new AuctionService(null);

        while (true) {
            Socket socket = server.accept();
            new ClientHandler(socket).start();
        }
    }
}
