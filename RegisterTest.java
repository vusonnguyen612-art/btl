import Network.Message;
import Model.User;
import Model.RegularUser;
import Factory.UserFactory;
import java.io.*;
import java.net.*;

public class RegisterTest {
    public static void main(String[] args) throws Exception {
        // Test 1: Register only (no prior failed login)
        System.out.println("=== Test 1: Direct REGISTER ===");
        Socket socket = new Socket("localhost", 8989);
        socket.setSoTimeout(15000);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        User user = new RegularUser(null, "newuser_" + System.currentTimeMillis(), "pass123");
        user.setEmail("new@test.com");
        Message regMsg = new Message(Message.Type.REGISTER);
        regMsg.setData(user);
        regMsg.setContent("pass123|BIDDER_SELLER");
        out.writeObject(regMsg);
        out.flush();
        Object resp = in.readObject();
        System.out.println("Register response: " + ((Message)resp).getType() + " - " + ((Message)resp).getContent());
        if (((Message)resp).getData() instanceof User) {
            User created = (User)((Message)resp).getData();
            System.out.println("Created user: " + created.getUsername() + " / " + created.getId() + " / role: " + created.getRole());
        }
        socket.close();
        System.out.println("=== TEST 1 COMPLETE ===");
    }
}
