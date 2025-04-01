import java.util.*; 
import java.io.*;    
import java.net.*;   // To handle socket connections

public class Client {

    List<Socket> channelList; //List to keep track of teh connected nodes
    Node node;
    
    //Client constructor in order to establish a connection
    public Client(Node node) {
        this.node = node;
        synchronized (node) {
            this.channelList = connectChannels(node);
        }
    }

    //Method that establishes connection
    public List<Socket> connectChannels(Node node) {
        System.out.println("[CLIENT] Making channel array...");
        List<Socket> channelList = new ArrayList<>();
        //dealing with 4 nodes so neighbours are set in the config file.
        for (Integer neighbour : node.neighbours.get(node.id)) {
            String host = node.getHost(neighbour);
            int port = node.getPort(neighbour);
            try {
                Socket client = new Socket();
                client.connect(new InetSocketAddress(host, port));
                client.setKeepAlive(true);
                channelList.add(client);
                node.idToChannelMap.put(node.hostToId_PortMap.get(host).get(0), client);
                System.out.println("[CLIENT] Connected to " + host + ":" + port);
            } catch (IOException error) {
                System.out.println("[CLIENT] Unable to connect to " + host + ":" + port);
            }
        }
        return channelList;
    }

    public void mapProtocol() {
        while (node.msgSent < node.totalMsg) {
            synchronized (node) {
                sendBulkMsg(node);            
            }
        }
    }

    //Method that creates a new message along with the clock matrix 
    public void sendBulkMsg(Node node) {
        // Updating clock Matrix before sending.
        node.clockMatrix.set(node.id, node.clockMatrix.get(node.id) + 1);
        Message msg = new Message(node.msgSent, node.id, node.clockMatrix);
        for (int j = 0; j < node.neighbours.get(node.id).size(); j++) {
            Socket channel = channelList.get(j);
            Client.sendMsg(msg, channel, node);
        }
        node.msgSent += 1;
        System.out.println("Sent Message with Clock:" + node.clockMatrix);
    }

    //this function is called by sendBulkMsg which converts msg to byte data to send data along with updated matrix
    public static void sendMsg(Message msg, Socket channel, Node node) {
        try {
            OutputStream outStream = channel.getOutputStream();
            DataOutputStream dataOut = new DataOutputStream(outStream);

            byte[] msgBytes = msg.toMessageBytes();
            dataOut.writeInt(msgBytes.length);
            dataOut.write(msgBytes); // Send message
            dataOut.flush();
        } catch (IOException error) {
            error.printStackTrace();
        }
    }
    
    //Client initialisation
    public void init() {
        Thread client = new Thread(() -> {
            System.out.println("[CLIENT] Starting...");
            try {
                System.out.println("[CLIENT] Initialising Causal Message Delivery Mechanism...");
                node.client.mapProtocol();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        client.start();
    }
}