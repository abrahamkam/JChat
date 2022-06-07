import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

//this class represents a connected client, holding its socket and listening to incoming traffic from it to forward to the server
//as well as handling communication with the server's login/registration utility functions
public class Client implements Runnable {

    private Socket socket;
    private StringServer server;

    //constructor, only initializes the global socket and server objects with the ones received upon client creation
    public Client(StringServer server, Socket client) {
        this.server = server;
        this.socket = client;
        System.out.println(socket.isClosed());
        System.out.println("Client connected");
    }

    //run method which listens to the client for incoming traffic and forwards it to the server, as well as listening
    //for and handling utility messages back and forth with the client
    @Override
    public void run() {
        try  {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter streamSender = new PrintWriter(socket.getOutputStream(), true);
            String message;
            while (true) {
                //read and store a line as it comes in from the socket's input stream through the buffered reader
                message = reader.readLine();
                System.out.println("message: " + message);
                //check if an exit command has been received, and request that the server remove the current client
                //if it has
                if (message.equalsIgnoreCase("exit")) {
                    System.out.println("exit received");
                    server.end(this);
                    break;
                }//The following else-if tree handles utility messages. Format is LOGINATTEMPT/REGISTRATIONATTEMPT-USERNAME-PASSWORD
                //The server responds with either a LOGINATTEMPT/REGISTRATIONATTEMPT-FAILURE or LOGINATTEMPT/REGISTRATION-SUCCESS/FAILURE(-username if it's a login attempt)
                else if(message.trim().startsWith("LOGINATTEMPT")){
                    //
                    String[] splitString = message.split("-");
                    String username = splitString[1];
                    String password = splitString[2];
                    if(server.verifyLogin(username, password)){
                        System.out.println("worked");
                        streamSender.println("LOGINATTEMPT-SUCCESS-"+username);
                    } else {
                        streamSender.println("LOGINATTEMPT-FAILURE");
                    }
                    server.verifyLogin(username, password);
                } else if(message.trim().startsWith("REGISTRATIONATTEMPT")){
                    String[] splitString = message.split("-");
                    String username = splitString[1];
                    String password = splitString[2];
                    if(server.registerUser(username, password)){
                        streamSender.println("REGISTRATIONATTEMPT-SUCCESS");
                    } else {
                        streamSender.println("REGISTRATIONATTEMPT-FAILURE");
                    }
                }else {
                    //if it wasn't a utility command, forward it to the server for sending to the client list
                    server.send(message);
                }

            }
        } catch (IOException e) {
            //if the socket connection is lost or an error occurs, request that server remove this client
            e.printStackTrace();
            server.end(this);
        }
    }

    //getter method used by the server to get the client connection's socket
    public Socket getSocket() {
        return socket;
    }
}
