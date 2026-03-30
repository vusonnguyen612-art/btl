package Network;

import java.net.*;
import java.io.*;

public class Client {

    public static void main(String[] args) throws Exception {

        Socket socket = new Socket("localhost", 1234);

        BufferedReader console = new BufferedReader(
                new InputStreamReader(System.in)
        );

        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );

        PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true
        );

        String msg;

        while (true) {
            msg = console.readLine();
            out.println(msg);

            System.out.println("Server: " + in.readLine());
        }
    }
}
