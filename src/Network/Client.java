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

        // Thread nhận dữ liệu từ server
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (Exception e) {}
        }).start();

        // Gửi dữ liệu
        String input;
        while ((input = console.readLine()) != null) {
            out.println(input);
        }
    }
}