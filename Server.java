import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Server {
    int port;
    Node node;
    public ArrayList<Message> buffer = new ArrayList<>();
    ServerSocket server;

    public Server(int port, Node node) {
        this.port = port;
        this.node = node;
    }

    //The processing that needs to be done upon arrival of a message
    public void handleMessage(Message msg) {
        System.out.println("[SERVER] Incoming message from "+ msg.nodeId + " : " + msg.clockMatrix);
        if (msg.messageType == MessageType.APPLICATION) {
            // Check clock with each in the list 
            int i = 0, flag = 0;
            while (i < buffer.size()) {
                int res = checkClock(msg.clockMatrix, buffer.get(i).clockMatrix);
                if (res < 0 || (res == 0 && msg.id < buffer.get(i).id)) {
                    buffer.add(i, msg);
                    flag = 1;
                    break;
                }
                i += 1;
            }
            if (flag == 0) buffer.add(msg);
            i = 0;
            System.out.println("Printing all the messages in the buffer");
            while (i < buffer.size()) {
                System.out.println(buffer.get(i).clockMatrix);
                i += 1;
            }
            checkBufferedMessages();
        }     
    }


    public void listen() {
        try {
            this.server = new ServerSocket(port);
            System.out.println("[SERVER] Started @ port: " + port);
            
            while (true) {
                Socket client = server.accept();
                // Start a new thread to handle the client connection
                Thread listener = new Thread(() -> {
                    try {
                        InputStream clientInputStream = client.getInputStream();
                        DataInputStream dataInputStream = new DataInputStream(clientInputStream);
                        long lastCheckTime = System.currentTimeMillis();
                        long checkInterval = 500;
                        while (!client.isClosed()) {

                            try {
                                // Reading Incoming Message.
                                int length = dataInputStream.readInt();
                                byte[] buffer = new byte[length];
                                dataInputStream.readFully(buffer);
                                Message msg = Message.fromByteArray(buffer);
                                synchronized (node) {
                                    handleMessage(msg);
                                }
                                long now = System.currentTimeMillis();
                                if (now - lastCheckTime >= checkInterval) {
                                    synchronized (node) {
                                        checkBufferedMessages();
                                    }
                                    lastCheckTime = now;
                                }
                            } catch (EOFException e) {
                                System.out.println("[SERVER] Connection closed by client");
                                break;
                            } catch (IOException | ClassNotFoundException e) {
                                e.printStackTrace();
                                break;
                            }

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                listener.start();
            }
        } catch (

        IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        Thread server = new Thread(() -> {
            System.out.println("[SERVER] Starting...");
            try {
                node.server.listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        server.start();
    }

    private Integer checkClock(List<Integer> A, List<Integer> B) {
        boolean greater = false;
        boolean less = false;
    
        for (int i = 0; i < A.size(); i++) {
            int a = A.get(i);
            int b = B.get(i);
            if (a > b) greater = true;
            else if (a < b) less = true;
        }
    
        if (greater && !less) return 1;     // A > B
        if (!greater && less) return -1;    // A < B
        return 0;    // A == B
    }
    
    //This function checks if message can be delivered or not based on the condition.
    private boolean canDeliver(Message msg) {
        Integer result = checkClock(node.clockMatrix, msg.clockMatrix);
        System.out.println("Check of Clock:" + node.clockMatrix + "||" + msg.clockMatrix + "==>" + result);
        if (result <= 0) return true;
        else return false; 
    }

    //This function is to deliver the message once checked if it can be delivered or not
    private void deliverMessage(Node node, Message msg) {
        int sender = msg.nodeId;
        
        // Merge the sender's clock into the local clock
        for (int i = 0; i < node.totalNodes; i++) {
            node.clockMatrix.set(i, Math.max(node.clockMatrix.get(i), msg.clockMatrix.get(i)));  
        }
        //node.clockMatrix.set(node.id, node.clockMatrix.get(node.id) + 1);
        
        node.msgReceived++;
    
        System.out.println("[SERVER] Message delivered from Node  " + sender + " : " + msg.clockMatrix);
        System.out.println("[SERVER] Total messages received: " + node.msgReceived);
        if (node.msgReceived == 3){
            System.out.println("[SERVER] All Required messages from the connected nodes received.");
            System.out.println("[SERVER]: CLOCK:" + node.clockMatrix);
        }
    }
    
    //This functions checks for the buffered messages if they can be sent now or not
    private void checkBufferedMessages() {
        boolean flag = false;
        for (int i = 0; i < buffer.size(); i++) {
            // Check condition for deliver
            if (canDeliver(buffer.get(i))) {
                synchronized(node) {
                    deliverMessage(node, buffer.get(i));
                }
                buffer.remove(i);
                flag = true;
                break;
            }   
        }
        if (flag) checkBufferedMessages();
        return;
    }
    
}
