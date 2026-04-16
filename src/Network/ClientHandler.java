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
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out = new PrintWriter(socket.getOutputStream(), true);

            Server.broadcast("🔌 " + username + " vào");

            String msg;

            while ((msg = in.readLine()) != null) {

                String[] parts = msg.split(" ");

                switch (parts[0]) {

                    case "LOGIN":
                        if (parts.length < 2) {
                            send("❌ Cú pháp: LOGIN <username>");
                            break;
                        }
                        String newUsername = parts[1];
                        boolean usernameTaken = Server.clientHandlers.stream()
                            .anyMatch(h -> h != this && h.username.equals(newUsername));
                        if (usernameTaken) {
                            send("❌ Username đã được sử dụng");
                            break;
                        }
                        Server.broadcast("🔌 " + username + " đổi tên thành " + newUsername);
                        this.username = newUsername;
                        send("✅ Đăng nhập thành công với username: " + username);
                        break;

                    case "CREATE":
                        if (username.startsWith("User")) {
                            send("❌ Bạn cần đăng nhập trước bằng lệnh LOGIN <username>");
                            break;
                        }
                        if (parts.length<3){
                            send("❌ Lệnh CREATE thiếu tham số. Cú pháp đúng là: CREATE <tên sản phẩm> <giá>");
                            break;
                        }
                        String item = parts[1];
                        int price = Integer.parseInt(parts[2]);

                        Server.auctionService.createAuction(item, price, username);

                        Server.broadcast("📦"+username+" đã tạo phiên đấu giá sản phẩm:  " + item + " | Giá: " + price);
                        break;

                    case "BID":
                        if(parts.length<2){
                            send("❌ Lệnh BID thiếu số tiền. Cú pháp đúng là : BID <Số Tiền>");
                            break;
                        }
                        int amount = Integer.parseInt(parts[1]);

                        String result = Server.auctionService.placeBid(username, amount);

                        Server.broadcast(result);
                        break;

                    case "AUTOBID":
                        if(parts.length<4){
                            send("❌ Cú pháp: AUTOBID <max_price> <step>");
                            send("   Ví dụ: AUTOBID 10000 500 (tự động bid tối đa 10000, mỗi lần +500)");
                            break;
                        }
                        int maxPrice = Integer.parseInt(parts[1]);
                        int step = Integer.parseInt(parts[2]);
                        String autoResult = Server.auctionService.registerAutoBid(username, maxPrice, step);
                        send(autoResult);
                        Server.broadcast(autoResult);
                        break;

                    case "CANCELAUTO":
                        String cancelResult = Server.auctionService.cancelAutoBid(username);
                        send(cancelResult);
                        Server.broadcast(cancelResult);
                        break;

                    case "END":
                        String res = Server.auctionService.endAuction(username);

                        send(res);
                        if (!res.startsWith("❌")) {
                            Server.broadcast(res);
                        }
                        break;
                }
            }

        } catch (Exception e) {
            System.out.println(username + " rời");
        }
    }
}