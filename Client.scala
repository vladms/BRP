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
    var in = new BufferedSource(socket.getInputStream).getLines();
    val out = new PrintStream(socket.getOutputStream);
    val userlist = new ArrayList[String];
    var userArray: Array[String] = Array();
    var table = new JList[String]();
    var socketPeerToPeer = new Socket();
    var peerConnected = false;

    println("Client initialized:");
    val listenerSocket = 5556;

    def startListening() {
      try {
        val server = new ServerSocket(listenerSocket);
        println("Server created");
        val listeningThread = new Thread(new Runnable {
          def run() {
            try {
              peerConnected = true;
              out.println("SUCCESS/" + listenerSocket);
              out.flush();
              println("Listening for other client");
              while (true) {
                val otherClient = server.accept();
                socketPeerToPeer = otherClient;
                startBRP();
                println("Other client has connected!");

                val clientThread = new Thread(new Runnable {
                  def run() {
                    var in = new BufferedReader(new InputStreamReader(otherClient.getInputStream()));
                    val out = new PrintStream(otherClient.getOutputStream());

                    while (in.ready()) {
                      val message = in.readLine();
                      println("Message readed from listening user: " + message);
                      out.println("#listeningUser: Response after accepting connection from you");
                      println(message);
                    }
                  }
                })

                clientThread.start();
              }
            } catch {
              case e: Exception => out.println("FAILURE");
                println(e);
            }
          }
        });

        listeningThread.start();
      } catch {
        case e: Exception => ;
      }
    }
    def startBRP(){

      var shouldRunLocal = true;
        try {
          while (shouldRunLocal) {
            var in = new BufferedReader(new InputStreamReader(socketPeerToPeer.getInputStream())).readLine();
            val messageFromTheOtherClient = in;
            println("The other user sent:  " + messageFromTheOtherClient);
            if (messageFromTheOtherClient.equals("DISCONNECT")){
              println("I should autocancel myself!");
              shouldRunLocal = false;
            }

            
          }
        } catch {
          case e: Exception => println("Error errorError errorError errorError error");
        }

    }
    def connectToPeer(address: String) {
      peerConnected = true;
      println("SocketAdressToConnect: " + address);
      socketPeerToPeer = new Socket(InetAddress.getByName("localhost"), Integer.valueOf(address));
      println("Connected to the other client");
      val outPeerToPeer = new PrintStream(socketPeerToPeer.getOutputStream);
      outPeerToPeer.println("#Requester user: I have connected to you and this is my message");
      var inPeerToPeer = new BufferedSource(socketPeerToPeer.getInputStream).getLines();
      System.out.println(inPeerToPeer.next());
      
      startBRP();
    }

    // startListening();

    val messsageTextField = new JTextField(20);
    val contents = new JPanel();
    val sendButton = new JButton("Send");
    val closeButton = new JButton("Close");
    val requestButton = new JButton("Request user list");
    val tablePanel = new JPanel();

    sendButton.addActionListener(new ActionListener() {
      def actionPerformed(e: ActionEvent) {

        if (peerConnected){
          println("SENDING :");
          println(messsageTextField.getText());
          
          val outPeerToPeer = new PrintStream(socketPeerToPeer.getOutputStream);
          outPeerToPeer.println(messsageTextField.getText());
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
        out.println("REQ_USERLIST");
        out.flush();

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
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine();
            val socketToOpen = in;

            println("Server sent " + socketToOpen);

            if (socketToOpen.startsWith("LISTENSOCKET")) {
              startListening();
            } else if (socketToOpen.startsWith("TALKSOCKET")) {
              connectToPeer(socketToOpen.split("/")(1));
            } else if (socketToOpen.startsWith("CLIENT")) {
              val receivedList = in;
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

              contents.remove(table);
              table = new JList[String](userArray);
              table.setMinimumSize(new Dimension(200, 200));
              table.setPreferredSize(new Dimension(200, 200));
              table.addListSelectionListener(new ListSelectionListener() {
                def valueChanged(e: ListSelectionEvent) {

                  if (selectedUserListIndex != table.getSelectedIndex()) {
                    out.println(userlist.get(0).split("/")(0));
                    selectedUserListIndex = table.getSelectedIndex();
                    println("Selected index from table:")
                    println(table.getSelectedIndex());
                    val clientToConnect = in;
                    println("clientToConnectSocket " + clientToConnect);

                  }
                }
              });
              contents.add(table);
              contents.repaint();
              contents.setVisible(false);
              contents.setVisible(true);
            }
          }
        } catch {
          case e: Exception => println("Error errorError errorError errorError error");
        }
      }
    });
    serverListener.start();
  }

  def main(args: Array[String]) {
    val client = new ClientClass();
  }
}