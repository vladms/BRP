import java.net._
import java.io._
import scala.io._
import java.util.ArrayList

object Server {
  var clients = new ArrayList[Socket]();
  var numberOfClients = 0;
  var shouldRun = true;
  var userRequestingConnectionToAnotherUser = new Socket();


  val connectionListener = new Thread(new Runnable {
    def run() {
      try {
        val server = new ServerSocket(5555);
        var numberOfConnectedUsers = 0;
        println("Listening for connections");

        while (shouldRun) {
          val currentClient = server.accept();

          clients.add(currentClient);
          numberOfClients = numberOfClients + 1;
          val requestListener = new Thread(new Runnable {
            def run() {
              var shouldRunLocal = true;

              try {
                while (shouldRunLocal) {
                  var in = new BufferedReader(new InputStreamReader(currentClient.getInputStream())).readLine();
                  val out = new PrintStream(currentClient.getOutputStream());
                  if (in == null) {
                    in = "DISCONNECT";
                  }


                  println("Client " + clients.indexOf(currentClient) + " sent: " + in);

                  if (in.equals("DISCONNECT")) {
                    //Client disconnected
                    clients.remove(currentClient);
                    numberOfClients = numberOfClients - 1;
                    shouldRunLocal = false;
                  } else if (in.equals("REQ_USERLIST")) {
                    //Client requests users list
                    var currentIndex = 0;
                    var clientList = "";
                    for (i <- 0 to clients.size() - 1) {
                      if (clients.get(i) != currentClient) {
                        clientList = clientList + "CLIENT" + i + clients.get(i).getInetAddress() + "|";
                      }
                    }
                    out.println(clientList);
                  } else if (in.startsWith("CLIENT")) {
                    //Client wants to connect to CLIENT X

                    userRequestingConnectionToAnotherUser = currentClient;
                    val listeningUserIndex = Integer.valueOf(in.replaceAll("[^0-9]", ""));
                    numberOfConnectedUsers += 1;
                    val userToUserSocket = 5555 + numberOfConnectedUsers;
                    //Send to the receiver user the socket that he should open
                    val client = clients.get(listeningUserIndex);

                    val receiverUserOut = new PrintStream(client.getOutputStream());
                    receiverUserOut.println("LISTENSOCKET/" + userToUserSocket);

                  } else if (in.startsWith("SUCCESS")) {
                    val sendingUserIndex = Integer.valueOf(in.replaceAll("[^0-9]", ""));
                    val receiverUserOut = new PrintStream(userRequestingConnectionToAnotherUser.getOutputStream());

                    receiverUserOut.println("TALKSOCKET/" + sendingUserIndex);

                  } else if (in.equals("FAILURE")) {
                    numberOfConnectedUsers -= 1;
                    val receiverUserOut = new PrintStream(userRequestingConnectionToAnotherUser.getOutputStream());
                    receiverUserOut.println("RECEIVER_USER_CAN'T_OPEN_SOCKET");
                  }


                }
              } catch {
                case e: Exception => clients.remove(currentClient);
              }
            }
          });

          requestListener.start();

          println("Client connected!");
        }
      } catch {
        case e: Exception => println("EXCEPTION IN CONN_LISTENER " + e.printStackTrace());
      }
    }
  });

  def main(args: Array[String]): Unit = {
    connectionListener.start();

    while (shouldRun) {
      var input = scala.io.StdIn.readLine();

      if (input.equals("EXIT")) {
        shouldRun = false;
        connectionListener.interrupt();
        System.exit(1);
      } else if (input.equals("STATUS")) {
        println("Number of clients: " + clients.size());
      }
    }
  }
}