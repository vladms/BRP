import java.net._
import java.io._
import scala.io._
import swing._
import Swing._
import java.util.ArrayList

object Client extends MainFrame with App
{
  title = "Simple Client";

  preferredSize = (500, 500);

  val socket = new Socket(InetAddress.getByName("localhost"), 5555);
  var in = new BufferedSource(socket.getInputStream).getLines();
  val out = new PrintStream(socket.getOutputStream);
  val messsageTextField = new TextField(20);
  val userlist = new ArrayList[String];

  println("Client initialized:");

  contents = new BorderPanel()
  {
    add(new FlowPanel()
    {
      contents += new Button(new Action("Send")
      {
        def apply
        {
          out.println(messsageTextField.text);
          out.flush();
          println("Client received: " + in.next);
        }
      });

      contents += messsageTextField;
	  

      contents += new Button(new Action("Close")
      {
        def apply
        {
          out.println("DISCONNECT");
          out.flush();
          socket.close();
          System.exit(0);
        }
      })
	  
	  contents += new Button(new Action("Refresh user list"){
		def apply{
			out.println("REQ_USERLIST");
			out.flush();
			val receivedList = in.next();
			val splitList = receivedList.split("\\|");
			
			for ( str <- splitList) {
				userlist.add(str);
				println(str);
			}
		}
	  })
    }, BorderPanel.Position.Center)
  }

  pack();
  visible = true;
}