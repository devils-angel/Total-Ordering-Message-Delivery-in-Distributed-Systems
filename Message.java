import java.io.*;
import java.util.*;

enum MessageType {
    APPLICATION,
};

//Message class to keep track of message Id , Node Id and clack Matrix in order to implement causal delievery system
public class Message implements Serializable {
    public int id = -1;
    public int nodeId = 0;
    public MessageType messageType;   
    List<Integer> clockMatrix;

    // Message Constructor with Id, nodeId and clock Matrix
    public Message(int id, int nodeId, List<Integer> clockMatrix) {
        this.messageType = MessageType.APPLICATION;
        this.id = id;
        this.nodeId = nodeId;
        this.clockMatrix = new ArrayList<>(clockMatrix);
    }


    public byte[] toMessageBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(this);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static Message fromByteArray(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (Message) objectInputStream.readObject();
        }
    }
}
