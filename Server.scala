import java.net._
import java.io._
import scala.io._
import scala.collection.mutable.ArrayBuffer
import java.util.ArrayList

object Server
{
  var clients = new ArrayList[Socket]();
  var numberOfClients = 0;
  var shouldRun = true;


  val connectionListener = new Thread(new Runnable {
    def run() {
      try {
        val server = new ServerSocket(5555);
        println("Listening for connections");
        while(shouldRun) {
          val currentClient = server.accept();
          clients.add(currentClient);
          numberOfClients = numberOfClients + 1;

          val requestListener = new Thread(new Runnable {
            def run() {
              var shouldRunLocal = true;
              try {
                while(shouldRunLocal) {
                  val in = new BufferedReader(new InputStreamReader(currentClient.getInputStream())).readLine();
                  val out = new PrintStream(currentClient.getOutputStream());

                  println("Client " + clients.indexOf(currentClient) + "sent: " + in);
                  out.println("ACK");
                  out.flush();

                  if(in.equals("DISCONNECT")){
                    clients.remove(currentClient);
                    numberOfClients = numberOfClients - 1;
                    shouldRunLocal = false;
                  }
                }
              } catch {
                case e: Exception => e.printStackTrace();
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

  def main(args: Array[String]) : Unit = {
    connectionListener.start();

    while(shouldRun) {
      var input = scala.io.StdIn.readLine();



      if(input.equals("EXIT")) {
        shouldRun = false;
        connectionListener.interrupt();
        System.exit(1);
      } else if(input.equals("STATUS")) {
        println("Number of clients: " + clients.size());
      }
    }
  }
}