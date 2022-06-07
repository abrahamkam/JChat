import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.Socket;
//This class handles receipt of text from the connected string server
public class Receiver extends Thread{
    private BufferedReader reader;
    private Main client;
    public Receiver(Socket textSocket, Main client) {
        try {
            this.client = client;
            reader = new BufferedReader(new InputStreamReader(textSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //read input from the socket reader and process it
    @Override
    public void run() {
        super.run();

        while (true) {
            try {
                String read = reader.readLine();
                /**
                 * Check if the message received is in XML format. If not, check if it's a utility message and take appropriate action.
                 * if not a utility message, send it to the UI handler as a plain string
                 */
                if (read.startsWith("<?xml")) {
                    //build an XML document out of the message string, and retrieve name email and body from it
                    Document doc = new SAXBuilder().build(new StringReader(read));
                    String name = doc.getRootElement().getChild("header").getChild("id").getChild("name").getText();
                    String body = doc.getRootElement().getChild("body").getText();
                    //Print the finished message values
                    client.receiveText(">" + name + ": " + body);
                    //Call appropriate UI handler method based on what kind of utility message it is, if not xml
                } else if(read.startsWith("LOGINATTEMPT-SUCCESS")){
                    String[] splitRead =  read.split("-");
                    client.receiveLoginSuccess(splitRead[2]);
                } else if(read.startsWith("LOGINATTEMPT-FAILURE")){
                    client.receiveLoginFailure();
                } else if(read.startsWith("REGISTRATIONATTEMPT-SUCCESS")){
                    client.receiveRegistrationSuccesss();
                } else if(read.startsWith("REGISTRATIONATTEMPT-FAILURE")){
                    client.receiveRegistrationFailure();
                }//send as plain string to the UI handler if it's not recognized as xml or utility
                else {
                    client.receiveText(read);
                }

            } catch (IOException | JDOMException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
}
