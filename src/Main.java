import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.security.*;


public class Main extends Application {
    private Document template;
    private PrintWriter streamSender;
    private VBox messageWindow;
    private ObjectOutputStream outputStream;
    private Label receiptLabel;
    @Override
    public void start(Stage primaryStage) throws Exception {
        //initialize the text and file sockets, as well as their output writers
        Socket mainSocket = new Socket("127.0.0.1", 2000);
        Socket fileSocket = new Socket("atlas.dsv.su.se", 4848);
        streamSender = new PrintWriter(mainSocket.getOutputStream(), true);
        outputStream = new ObjectOutputStream(fileSocket.getOutputStream());
        //set up the XML template with anonymous as an initial name, this can be changed later through login
        setupXML("anonymous");
        //Initialize the objects that will handle receiving incoming traffic
        Receiver receiver = new Receiver(mainSocket, this);
        receiver.start();
        FileReceiver fileReceiver = new FileReceiver(fileSocket, this);
        fileReceiver.start();

        //Set up the UI elements
        Button sendImageButton = new Button();
        sendImageButton.setText("Send file");

        Button sendTextButton = new Button();
        sendTextButton.setText("Send text");

        GridPane rootGrid = new GridPane();
        ScrollPane scrollPane = new ScrollPane(rootGrid);
        scrollPane.setFitToHeight(true);

        VBox vBox = new VBox();
        messageWindow = vBox;
        ScrollPane vScrollPane = new ScrollPane(vBox);


        receiptLabel = new Label();
        TextField usernameField = new TextField();
        TextField passwordField = new PasswordField();
        Button loginButton = new Button("Login");
        Button registerButton = new Button("Register");
        HBox accountButtonBox = new HBox(loginButton,registerButton);

        rootGrid.add(vScrollPane, 0, 0);
        HBox buttonBox = new HBox();
        TextField textField = new TextField();

        rootGrid.add(textField, 0, 1);
        buttonBox.getChildren().add(sendImageButton);
        buttonBox.getChildren().add(sendTextButton);
        rootGrid.add(buttonBox, 0, 2);
        rootGrid.add(receiptLabel,0,3);
        rootGrid.add(usernameField,0,4);
        rootGrid.add(passwordField,1,4);
        rootGrid.add(accountButtonBox,0,5,2,1);

        usernameField.setText("Username");
        //Set up the handlers for the UI button elements
        loginButton.setOnMouseClicked(e->{
            if(usernameField.getText().length() > 0 && passwordField.getText().length() > 0){
                login(usernameField.getText(),passwordField.getText());
                usernameField.clear();
                passwordField.clear();
            }

        });

        registerButton.setOnMouseClicked(e -> {
            if(usernameField.getText().length() > 0 && passwordField.getText().length() > 0){
                register(usernameField.getText(),passwordField.getText());
                usernameField.clear();
                passwordField.clear();
            }
        });

        sendTextButton.setOnMouseClicked(e -> {
            sendText(textField.getText());
            textField.clear();
            vScrollPane.setVvalue(1.0);
        });
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JPGs", "*.jpg")
        );
        sendImageButton.setOnMouseClicked(event -> {
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            sendImage(selectedFile);
        });
        Scene scene = new Scene(scrollPane, 300, 250);
        primaryStage.setTitle("EXChat");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    //Receives a password as a plain string and returns a hashed string of the password
    private String hashPass(String password){
        //Set the default to INVALID in case the hashing fails
        String hashedPass = "INVALID";
        try {
            //Create an MD5 message digest and add the password string as a byte array to it
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(password.getBytes());
            //hash the password, returning a byte array
            byte[] hashOutput = messageDigest.digest();
            //take the byte array and translate it byte by byte to string, adding each translated byte to a stringbuilder
            StringBuilder sb = new StringBuilder();
            for (byte b: hashOutput){
                sb.append(String.format("%02x",b));
            }
            //if hashing was successful, the return value is set to the resulting hash
            hashedPass = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hashedPass;

    }

    //Sends a regular text message in a preset XML format
    private void sendText(String message) {
        try {
            //set the body element of the document template to the provided message string
            template.getRootElement().getChild("body").setText(message);
            //provide a format which makes the XMLOutputter output the whole xml document string on a single line
            Format format = Format.getCompactFormat();
            format.setLineSeparator("");
            XMLOutputter outputter = new XMLOutputter(format);
            //send the XML document template with the provided text added through the printwriter connected to the socket connection
            streamSender.println(outputter.outputString(template));
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }

    //Sends a plain utility message as a regular string
    private void sendUtilMessage(String message) {
        try {
            streamSender.println(message);
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }

    //Takes a username and password and sends a registration request to the server
    private void register(String username, String password){
        sendUtilMessage("REGISTRATIONATTEMPT-"+username+"-"+hashPass(password));
    }

    //Takes a username and password and sends a login request to the server
    private void login(String username, String password){
        sendUtilMessage("LOGINATTEMPT-"+username+"-"+hashPass(password));
    }

    //Takes a jpg image file and sends it to the server
    private void sendImage(File imageFile) {
        try {
            //read the image file into a form of image compatible with javafx
            BufferedImage image = ImageIO.read(imageFile);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);
            //put the image into a Storage object and send it
            Storage storage = new Storage(out.toByteArray());
            outputStream.writeObject(storage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Receives an image and adds it to the message window
    public void receiveImage(Image image) {
        Platform.runLater(() -> messageWindow.getChildren().add(new ImageView(image)));
    }

    //Receives a string and adds it to the message window
    public void receiveText(String text) {
        Platform.runLater(() -> messageWindow.getChildren().add(new Label(text)));
    }

    //utility method to notify the UI that a login attempt was rejected
    public void receiveLoginFailure(){
        Platform.runLater(() -> receiptLabel.setText("Login failed"));
    }

    //utility method to notify the UI that a registration attempt was successful
    public void receiveRegistrationSuccesss(){
        Platform.runLater(() -> receiptLabel.setText("Registration successful"));
    }
    //utility method to notify the UI that a registration attempt was rejected
    public void receiveRegistrationFailure(){
        Platform.runLater(() -> receiptLabel.setText("Registration failed"));
    }
    //utility method to notify the UI that a login attempt was successful and change the username in the xml template
    public void receiveLoginSuccess(String userName){
        Platform.runLater(() -> receiptLabel.setText("Login Successful, username set to "+userName));
        template.getRootElement().getChild("header").getChild("id").getChild("name").setText(userName);
    }

    //set up the XML template that will be used to send regular text messages
    private void setupXML(String nameString) {

        //Set up the document's structure
        Element message = new Element("message");

        //set the document's format through the message.dtd file
        Document document = new Document(message, new DocType("message.dtd"));

        //populate the document with the elements
        Element header = new Element("header");
        Element protocol = new Element("protocol");
        Element type = new Element("type");
        Element version = new Element("version");
        Element command = new Element("command");
        Element id = new Element("id");
        Element name = new Element("name");
        Element body = new Element("body");

        message.addContent(header);
        message.addContent(body);

        header.addContent(protocol);
        header.addContent(id);

        protocol.addContent(type);
        protocol.addContent(version);
        protocol.addContent(command);

        id.addContent(name);

        Element idref = document.getRootElement().getChild("header").getChild("id");
        idref.getChild("name").setText(nameString);

        template = document;

    }
}
