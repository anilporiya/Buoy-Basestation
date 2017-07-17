import java.io.IOException;

/**
 *author Anil Poriya arp6763
 *
 */
public class Controller {

	public static void main(String[] args) throws IOException {
		
		int udpPortOut = 0;
		int udpPortIn = 0;
		String ipAdd = "";
		//int length = args.length;
		switch(args[0]){
		  	case "-bs": // Base Station
		  			   udpPortOut = Integer.parseInt(args[1]);
		  			   udpPortIn = Integer.parseInt(args[2]);
		  			   if(args.length == 4){
		  				 ipAdd = args[3];
		  			   }
		  			   
		  			   
		  			   Basestation.receive(ipAdd,udpPortOut,udpPortIn);
		  			   break;
		  	case "-bu":
		  			   udpPortOut = Integer.parseInt(args[1]);
		  			   udpPortIn = Integer.parseInt(args[2]);
		  			   if(args.length == 4){
		  				 ipAdd = args[3];
		  			   }
		  			   
		  			   
		  			   Buoy.send(ipAdd, udpPortOut,udpPortIn);
		  			   break;
		}

	}

}
