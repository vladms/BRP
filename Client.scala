import java.awt._
import java.awt.event._
import java.net._
import java.io._
import javax.swing._
import javax.swing.event._
import scala.io._
import java.util.ArrayList
import scala.swing.event.SelectionChanged

object Client extends JFrame {

  class ClientClass() {
    setTitle("Test Client");

    setSize(600, 400);

    val socket = new Socket(InetAddress.getByName("localhost"), 5555);
    var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    val out = new PrintStream(socket.getOutputStream);
    val userlist = new ArrayList[String];
    var userArray: Array[String] = Array();
    var table = new JList[String]();
    var socketPeerToPeer = new Socket();
    var inPeerToPeer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    var outPeerToPeer = new PrintStream(socket.getOutputStream());
    var peerConnected = false;
    var isSending = false;
    var listening = true;
    val maxNumberOfRetransmissions = 4;
    var server = new ServerSocket();
    println("Client initialized:");

    def startListening(address: String, name: String) {
      listening = true;
      try {
        server = new ServerSocket(Integer.valueOf(address));
        println("Server created");
        val listeningThread = new Thread(new Runnable {
          def run() {
            try {
              out.println("SUCCESS/" + address + "/" + name);
              println("Listening for other client");
              while (listening) {
                val otherClient = server.accept();
                peerConnected = true;
                contents.remove(table);
                messageArea = new JTextArea(20, 20);
                contents.add(messageArea);
                contents.repaint();
                contents.setVisible(false);
                contents.setVisible(true);
                socketPeerToPeer = otherClient;
                inPeerToPeer = new BufferedReader(new InputStreamReader(socketPeerToPeer.getInputStream()));
                outPeerToPeer = new PrintStream(socketPeerToPeer.getOutputStream());
                startBRP();
              }

              server.close();
              peerConnected = false;

            } catch {
              case e: Exception => out.println("FAILURE");
                println(e);
            }

            contents.remove(messageArea);
            contents.add(table);
            contents.repaint();
            contents.setVisible(false);
            contents.setVisible(true);
          }
        });

        listeningThread.start();
      } catch {
        case e: Exception => ;
      }
    }
    def startBRP(){

      var shouldRunLocal = true;
      var enteredSimulation = -1;
      var maxEnteredSimulation = 2;


        try {
            // val in = new BufferedReader(new InputStreamReader(socketPeerToPeer.getInputStream()));
            // val out = new PrintStream(socketPeerToPeer.getOutputStream(), true);

          while (shouldRunLocal) {
            if (isSending == false) {
              
              var messageFromTheOtherClient = inPeerToPeer.readLine();

              if (messageFromTheOtherClient.equals("DISCONNECT")){
                  println("I should autocancel myself!");
                  shouldRunLocal = false;
                  listening = false;
                  socketPeerToPeer.close();
                  server.close();

                  contents.remove(messageArea);
                  contents.add(table);
                  contents.repaint();
                  contents.setVisible(false);
                  contents.setVisible(true);
              } else if (!messageFromTheOtherClient.equals("IGNORE")) {
                
                var numberOfChunks = 0;
                var waitForOtherChunks = false;
                var currentMessage = "";
                
                println("RECEIVED:  " + messageFromTheOtherClient);

                if (messageFromTheOtherClient.split("#")(0).equals("START_SENDING")) {
                  println("start_sending");
                  numberOfChunks = Integer.valueOf(messageFromTheOtherClient.split("#")(1));
                  currentMessage = "";
                  waitForOtherChunks = true;
                  outPeerToPeer.println("IGNORE");
                  outPeerToPeer.println("ACKNOWLEDGED");
                  println("SENT: ACKNOWLEDGED");
                }
                currentMessage = "";
                  while (waitForOtherChunks) {
                    messageFromTheOtherClient = inPeerToPeer.readLine();
                    println("messageFromTheOtherClient: " + messageFromTheOtherClient);  

                    if (numberOfChunks == 4 && enteredSimulation < maxEnteredSimulation){
                      enteredSimulation += 1;
                      println("SIMULATE ERROR");  
                      outPeerToPeer.println("NOT ACKNOWLEDGED");
                    } else {
                      currentMessage = currentMessage + messageFromTheOtherClient;
                      numberOfChunks = numberOfChunks - 1;
                      println("RECEIVED:  " + messageFromTheOtherClient);
                      outPeerToPeer.println("ACKNOWLEDGED");
                      println("SENT: ACKNOWLEDGED");
                      if (numberOfChunks == 0) {
                       waitForOtherChunks = false;
                     }
                    }
                }

                println("MESSAGE:  " + currentMessage);
                messageArea.append("OTHER_PERSON: " + currentMessage + "\n");
              }

            }
          }
          listening = false;
        } catch {
          case e: Exception => println(e);
          listening = false;
          shouldRunLocal = false;
          peerConnected = false;
        }

    }
    def connectToPeer(address: String) {
      println("SocketAdressToConnect: " + address);
      socketPeerToPeer = new Socket(InetAddress.getByName("localhost"), Integer.valueOf(address));
      inPeerToPeer = new BufferedReader(new InputStreamReader(socketPeerToPeer.getInputStream()));
      outPeerToPeer = new PrintStream(socketPeerToPeer.getOutputStream());
      peerConnected = true;
      contents.remove(table);
      messageArea = new JTextArea(20, 20);
      contents.add(messageArea);
      contents.repaint();
      contents.setVisible(false);
      contents.setVisible(true);
      println("Connected to the other client");
      // val outPeerToPeer = new PrintStream(socketPeerToPeer.getOutputStream);
      // outPeerToPeer.println("#Requester user: I have connected to you and this is my message");
      // var inPeerToPeer = new BufferedSource(socketPeerToPeer.getInputStream).getLines();
      // System.out.println(inPeerToPeer.next());
      
      startBRP();
    }

    def disconnectFromPeer() {
      outPeerToPeer.println("DISCONNECT");
      socketPeerToPeer.close();
      listening = false;
      isSending = false;
    }

    // startListening();

    val messsageTextField = new JTextField(20);
    val contents = new JPanel();
    val sendButton = new JButton("Send");
    val closeButton = new JButton("Close");
    val requestButton = new JButton("Request user list");
    val tablePanel = new JPanel();
    var messageArea = new JTextArea(30, 30);

    sendButton.addActionListener(new ActionListener() {
      def actionPerformed(e: ActionEvent) {

        if (peerConnected){
          var nrOfTries = 0;
          if (!isSending){

            isSending = true;

            messageArea.append("YOU: " + messsageTextField.getText() + "\n");
            var messageToSend = messsageTextField.getText();
            

            var messageLength = messageToSend.length();
            var currentChunk = 0;
            if (messageLength > 0){
              // val outPeerToPeer = new PrintStream(socketPeerToPeer.getOutputStream(), true);
              // val inPeerToPeer = new BufferedReader(new InputStreamReader(socketPeerToPeer.getInputStream()))
              println("SENDING :" + messsageTextField.getText());

              outPeerToPeer.println("START_SENDING#" + messageLength);
              println("SENT: " + "START_SENDING#" + messageLength);

              var response = inPeerToPeer.readLine();
              if (response.length() > 0){
                println("RECEIVED: " + response); 
                if (response.equals("ACKNOWLEDGED")) {
                  while (isSending) {
                      var chunkToSend = messageToSend.substring(currentChunk, currentChunk + 1);

                      outPeerToPeer.println(chunkToSend);
                      println("SENT: " + chunkToSend);

                      response = inPeerToPeer.readLine();
                      println("RECEIVED: " + response);            
                      if (response != null){
                        if (response.equals("ACKNOWLEDGED")) {
                          currentChunk = currentChunk + 1;
                          nrOfTries = 0;
                          if (currentChunk == messageLength) {
                            isSending = false;
                            messsageTextField.setText("");
                          }
                        } else {
                          nrOfTries += 1;
                          if (nrOfTries == maxNumberOfRetransmissions){
                            println("Current junk: " +currentChunk + " was not sent! Moving on!");
                            nrOfTries = 0;
                            currentChunk += 1;
                            if (currentChunk == messageLength) {
                              isSending = false;
                              messsageTextField.setText("");
                            }
                          }
                        }
                      }
                 }
               }
             } else {
              //Response is NULL
              println("Response is null"); 
              isSending = false;

             }
            
          }
        } else {
                    isSending = false;
        }


        }
      };
    });

    contents.add(sendButton);

    contents.add(messsageTextField);


    closeButton.addActionListener(new ActionListener() {
      def actionPerformed(e: ActionEvent) {
        println("CLOSE");
      };
    });

    contents.add(closeButton);


    requestButton.addActionListener(new ActionListener() {
      def actionPerformed(e: ActionEvent) {
        if (peerConnected) {
          disconnectFromPeer();
          contents.remove(messageArea);
          contents.add(table);
          contents.repaint();
          contents.setVisible(false);
          contents.setVisible(true);
        }
        out.println("REQ_USERLIST");
      };
    })

    contents.add(requestButton);

    contents.add(table);

    add(contents);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setVisible(true);

    val serverListener = new Thread(new Runnable {
      def run() {
        var shouldRunLocal = true;
        try {
          while (shouldRunLocal) {
            // var in = new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine();
            val socketToOpen = in.readLine();

            println("Server sent " + socketToOpen);

            if (socketToOpen.startsWith("LISTENSOCKET")) {
              if (!peerConnected){
                startListening(socketToOpen.split("/")(1), socketToOpen.split("/")(2));
              } else {
                out.println("FAILURE/" + socketToOpen.split("/")(1));    
              }
            } else if (socketToOpen.startsWith("TALKSOCKET")) {
              connectToPeer(socketToOpen.split("/")(1));
            } else if (socketToOpen.startsWith("CLIENT")) {
              val receivedList = socketToOpen;
              val splitList = receivedList.split("\\|");
              var selectedUserListIndex = -1;
              println("List of clients connected to server: ");

              userlist.clear();
              for (str <- splitList) {
                userlist.add(str);
                println(str);
              }

              userArray = Array();
              userArray = userlist.toArray[String](userArray);

              contents.remove(messageArea);
              contents.remove(table);
              table = new JList[String](userArray);
              table.setMinimumSize(new Dimension(200, 200));
              table.setPreferredSize(new Dimension(200, 200));
              table.addListSelectionListener(new ListSelectionListener() {
                def valueChanged(e: ListSelectionEvent) {

                  if (selectedUserListIndex != table.getSelectedIndex()) {
                    selectedUserListIndex = table.getSelectedIndex();
                    out.println(userlist.get(selectedUserListIndex).split("/")(0));
                    println("Selected index from table:")
                    println(table.getSelectedIndex());
                    println("clientToConnectSocket " + userlist.get(selectedUserListIndex).split("/")(0));
                  }
                }
              });
              contents.add(table);
              contents.repaint();
              contents.setVisible(false);
              contents.setVisible(true);
            } else if (socketToOpen.startsWith("RECEIVER_USER_CAN")){
              messsageTextField.setText("Selected user is busy!");
              println("Selected user is busy!");
            }
          }
        } catch {
          case e: Exception => println(e);
        }
      }
    });
    serverListener.start();
  }

  def main(args: Array[String]) {
    val client = new ClientClass();
  }
}