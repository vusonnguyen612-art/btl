package Network;

import java.net.*;
import java.util.*;

import Service.*;
import Model.*;

public class Server {

    public static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        ServerSocket server = new ServerSocket(1234);
        System.out.println("Server đang chạy...");

        while (true) {
            Socket socket = server.accept();

            ClientHandler client = new ClientHandler(socket);
            clients.add(client);

            client.start();
        }
    }

    public static void broadcast(String msg) {
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }
}
