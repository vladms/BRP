import java.net._
import java.io._
import scala.io._
import java.util.ArrayList

object Server {
  var clients = new ArrayList[Socket]();
  var numberOfClients = 0;
  var shouldRun = true;


  val connectionListener = new Thread(new Runnable {
    def run() {
      try {
        val server = new ServerSocket(5555);
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
                    clients.remove(currentClient);
                    numberOfClients = numberOfClients - 1;
                    shouldRunLocal = false;
                  } else if (in.equals("REQ_USERLIST")) {
                    var currentIndex = 0;
                    var clientList = "";
                    for (i <- 0 to clients.size() - 1) {
                      if (clients.get(i) != currentClient) {
                        clientList = clientList + "CLIENT" + i + clients.get(i).getInetAddress() + "|";
                      }
                    }
                    out.println(clientList);
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