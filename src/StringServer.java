import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//This program creates an echo server that allows clients to connect and then echoes all text received from them to all
//connected clients. Can take the port as parameter, but if run without one it sets port default to 2000.
public class StringServer {
    //a connection to the database holding username and hashed password pairs
    private Connection dbConnection;
    //a list of currently connected clients
    private LinkedList<Client> clientList;

    public static void main(String[] args) {
        //If an argument has been provided, an attempt is made to use it as the port number. If not, default is set to 2000
        int port = (args.length == 0) ? 2000 : Integer.parseInt(args[0]);
        StringServer server = new StringServer(port);

    }

    //Constructor method. Sets up the socket, database connection and runs the main client listener loop
    public StringServer(int port) {

        //Set up the mysql driver connection
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            String url = "jdbc:mysql://" + "atlas.dsv.su.se" + "/" + "db_20963145";
            dbConnection = DriverManager.getConnection(url, "usr_20963145", "963145");
            ServerSocket socket = new ServerSocket(port);
            //initialize the list of currently connected clients as a new empty linkedlist
            clientList = new LinkedList<>();
            //initialize the thread pool that will hold the client objects
            ExecutorService pool = Executors.newFixedThreadPool(15);
            //main loop which listens for new clients on the socket, makes a Client object that can be put into a thread
            //to listen for traffic from and send traffic to the client.
            while (true) {
                System.out.println("Listening for clients");
                Socket newSocket = socket.accept();
                Client client = new Client(this, newSocket);
                pool.submit(client);
                clientList.add(client);
                System.out.println("number of clients:" + clientList.size());
            }
        } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException e) {
            e.printStackTrace();
        }
    }

    //callback method allowing a receiver to tell the server to remove it from the list of connected clients
    public void end(Client client) {
        clientList.remove(client);
        System.out.println("Client removed. Number of clients: " + clientList.size());
    }

    //Sends the provided message to the entire list of clients.
    public void send(String message) {
        //iterate through the entire list of clients
        for (Client client : clientList) {
            //Set up a printwriter and send the message to the current client in the loop
            try {
                PrintWriter printWriter = new PrintWriter(client.getSocket().getOutputStream(), true);
                printWriter.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Takes a password and username and attempts to update the database with a new entry. True if successful, false if not
    public boolean registerUser(String username, String password) {
        String query = "INSERT INTO UserTable (Username, Password) " +
                "VALUES (?,?)";
        try {
            //set the parameters in the query to the ones provided and execute the statement
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            stmt.close();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    //
    public boolean verifyLogin(String username, String password) {
        try {
            //Select all rows from the user/hashed password table
            String query = "SELECT * FROM UserTable";
            Statement stmt = dbConnection.createStatement();
            //iterate through the rows and return true if a username/hashed password match is found. Otherwise defaults to false.
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                if (username.equalsIgnoreCase(rs.getString("Username")) && password.equalsIgnoreCase(rs.getString("Password"))) {
                    return true;
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }
}