import java.awt._
import java.awt.event._
import java.net._
import java.io._
import javax.swing._
import javax.swing.event._
import scala.io._
import java.util.ArrayList
import scala.swing.event.SelectionChanged

object Client extends JFrame{

  class ClientClass() {
    setTitle("Test Client");

    setSize(600, 400);

    val socket = new Socket(InetAddress.getByName("localhost"), 5555);
    var in = new BufferedSource(socket.getInputStream).getLines();
    val out = new PrintStream(socket.getOutputStream);
    val userlist = new ArrayList[String];
    var userArray : Array[String] = Array();
    var table = new JList[String]();

    println("Client initialized:");

    def startListening() {
      try {
        val server = new ServerSocket(5556);
        println("Server created");
        val listeningThread = new Thread(new Runnable {
          def run() {
            try {

              println("Listening for other client");
              while (true) {
                val otherClient = server.accept();

                println("Other client has connected!");

                val clientThread = new Thread(new Runnable {
                  def run() {
                    var in = new BufferedReader(new InputStreamReader(otherClient.getInputStream()));
                    val out = new PrintStream(otherClient.getOutputStream());

                    while (in.ready()) {

                      val message = in.readLine();
                      out.println("Response after accepting connection from you");
                      println(message);
                    }
                  }
                })

                clientThread.start();
              }
            } catch {
              case e: Exception => println(e);
            }
          }
        });

        listeningThread.start();
      } catch {
        case e: Exception => ;
      }
    }

    def connectToPeer(address: String) {
      val socketPeerToPeer = new Socket(InetAddress.getByName(address), 5556);
      println("Connected to first client");
      val outPeerToPeer = new PrintStream(socketPeerToPeer.getOutputStream);
      outPeerToPeer.println("I have connected to you and this is my message");
      var inPeerToPeer = new BufferedSource(socketPeerToPeer.getInputStream).getLines();

      System.out.println(inPeerToPeer.next());
    }

    startListening();

    val messsageTextField = new JTextField(20);
    val contents = new JPanel();
    val sendButton = new JButton("Send");
    val closeButton = new JButton("Close");
    val requestButton = new JButton("Request user list");
    val tablePanel = new JPanel();

    sendButton.addActionListener(new ActionListener() {
      def actionPerformed(e: ActionEvent) {
        println("SEND");
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
        val receivedList = in.next();
        val splitList = receivedList.split("\\|");
        println("List of clients connected to server: ");

        userlist.clear();
        for ( str <- splitList) {
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
          def valueChanged(e: ListSelectionEvent){
            println(table.getSelectedIndex());
          }
        });
        contents.add(table);
        contents.repaint();
        contents.setVisible(false);
        contents.setVisible(true);
      };
    })

    contents.add(requestButton);

    contents.add(table);

    add(contents);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setVisible(true);
  }

  def main(args: Array[String]) {
    val client = new ClientClass();
  }
}