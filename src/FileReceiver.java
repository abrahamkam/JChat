import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

//This class handles receiving files using the Storage class
public class FileReceiver extends Thread {
    private Socket socket;
    private Main parent;

    public FileReceiver(Socket socket, Main parent) {
        this.socket = socket;
        this.parent = parent;
    }
    @Override
    public void run() {
        super.run();
        while (true) {
            try {
                //put the socket's inputstream into an objectinputstream to read the objects directly from the socket
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                //put the input coming from the socket into a storage class, since it's sent as a storage class
                Storage storage = (Storage) in.readObject();
                //read the bytearray in the storage object into a bufferedimage through a byte array input stream
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(storage.getData()));
                //convert it into an image compatible with javaFX UI elements and send it to the callback function
                //that puts it into the UI
                Image fxImage = SwingFXUtils.toFXImage(image, null);
                parent.receiveImage(fxImage);

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
    }
}
