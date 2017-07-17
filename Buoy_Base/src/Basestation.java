import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
//import java.net.SocketException;
//import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Scanner;

import javax.imageio.ImageIO;

/***
 * 
 * Basestation class will request and receive either Image or Sensor data from
 * the Buoy. The class will accept packets in any order and will store these
 * packets in an ArrayList. When it determines it has received the final packet,
 * it reorders and combines the packets to successfully receive the sensor data
 * or the image.
 * 
 * @author Anil Poriya arp6763
 *
 */
public class Basestation {
	public static Scanner scan = new Scanner(System.in);
	public static void receive(String ipAdd, int portOut, int portIn) throws IOException {
		DatagramSocket basestation = new DatagramSocket(portIn);
		//DatagramPacket basePacket;
		InetAddress inetAdd;
		if(ipAdd.equals("")){
			inetAdd = InetAddress.getLocalHost();
		}
		else{
			inetAdd = InetAddress.getByName(ipAdd);
		}

		while (true) {
			int choice = getChoice();

			if (choice == 2) {
				requestSensorData(inetAdd, portOut, basestation);
			}
			else if(choice == 1){
				requestImage(inetAdd,portOut,basestation);
			}
			else if(choice == 3){
				closeConnection(inetAdd,portOut,basestation);
				break;
			}
			
			
			//scan.close();
		}
		System.out.println();
		System.out.println("Finished Requesting");
		basestation.close();
		scan.close();

	}
	
	/**
	 * sends byte in the packet indicating that the
	 * base station has done requesting all data and wants
	 * to close the connection.
	 * 
	 * @param inetAdd InetAddress of the base station.
	 * @param portOut the port to send the packet to.
	 * @param basestation the current socket which used for sending packets.
	 * @throws IOException
	 */
	private static void closeConnection(InetAddress inetAdd, int portOut, DatagramSocket basestation) throws IOException {
			byte[] close = new byte[1];
			close[0] = 3;
			DatagramPacket closePacket = new DatagramPacket(close,close.length,inetAdd,portOut);
			basestation.send(closePacket);
		
	}
	
	/***
	 * sends byte in the packet indicating that the base station
	 * wants to request an image.
	 * @param inetAdd InetAddress of the base station.
	 * @param portOut the port to send the packet to.
	 * @param basestation the current socket which used for sending packets.
	 * @throws IOException 
	 */
	private static void requestImage(InetAddress inetAdd, int portOut, DatagramSocket basestation) throws IOException {
		ArrayList<Image_Message> packetList = new ArrayList<>();
		Image_Message message = new Image_Message();
		byte[] nullData = new byte[0];
		message.data = nullData;
		packetList.add(0,message);
		
		byte[] data = new byte[1];
		data[0] = 1;
		DatagramPacket imagePacket;
		imagePacket = new DatagramPacket(data,data.length,inetAdd,portOut);
		basestation.send(imagePacket);
		byte[] size = new byte[4];
		imagePacket = new DatagramPacket(size,size.length);
		basestation.receive(imagePacket);
		int imageSize = ByteBuffer.wrap(size).getInt();
		byte[] originalData = new byte[imageSize];
		int totalReceived = 0;
		while(true){
			byte[] recMessage = new byte[1024];
			imagePacket = new DatagramPacket(recMessage,recMessage.length);
			basestation.receive(imagePacket);
			Image_Message mess = new Image_Message();
			byte[] sequence = new byte[4];
			for(int i = 0;i<4;i++){
				sequence[i] = recMessage[i];
			}
			int num = ByteBuffer.wrap(sequence).getInt();
			int last = recMessage[4];
			if(last == 0){
				byte[] partData = new byte[1019];
				for(int i = 5;i<recMessage.length;i++){
					partData[i-5] = recMessage[i];
				}
				totalReceived+=1019;
				mess.data = partData;
				packetList.add(num,mess);
			}
			else if(last == 1){
				byte[] partData = new byte[imageSize - totalReceived];
				for(int i = 0;i<partData.length;i++){
					partData[i] = recMessage[i+5];
				}
				totalReceived+=1019;
			}

			
			imagePacket = new DatagramPacket(sequence,sequence.length,inetAdd,portOut);
			basestation.send(imagePacket);
			if(last == 1){
				break;
			}
			
		}
		
		/***
		 * combining all the packets stored in ArrayList to form
		 * the original message.
		 */
		int start = 0;
		for(int i = 1;i<packetList.size()-1;i++){
				Image_Message mess = packetList.get(i);
				byte[] messData = mess.data;
				for(int j = 0;j<messData.length;j++){
					originalData[start] = messData[j];
					start++;
				}
 		}
		InputStream inputImage = new ByteArrayInputStream(originalData);
		BufferedImage imageRec = ImageIO.read(inputImage);
//		ImageIO.write(imageRec, "jpg", new File())
		ImageIO.write(imageRec,"jpg",new File("receivedImage.jpg"));
		//System.out.println(new String(originalData).trim());
		System.out.println("Image Transferred Successfully");
	}
	
	/***
	 * sends a byte in a packet stating that it wants to 
	 * know the sensor data from the distant buoy.
	 *  
	 * 
	 * @param ipAdd InetAddress of the base station.
	 * @param portOut the port to send the packet to.
	 * @param socket the current socket which is used for sending packets.
	 * @throws IOException
	 */
	private static void requestSensorData(InetAddress ipAdd, int portOut, DatagramSocket socket) throws IOException {
		byte[] data = new byte[1];
		data[0] = 2;
		DatagramPacket sensorPacket = new DatagramPacket(data, 
				data.length, ipAdd, portOut);
		socket.send(sensorPacket);
		byte[] sensorData = new byte[4]; // 4 byte array to store an int value.
		sensorPacket = new DatagramPacket(sensorData,sensorData.length);
		socket.receive(sensorPacket);
		int dataRec = ByteBuffer.wrap(sensorData).getInt();
		System.out.println("Received Sensor Data: "+ dataRec);

	}
	
	/***
	 * Get choice from the Base station whether it wants to request
	 * sensor data, image from distant buoy user or it has done
	 * requesting from user and wants to exit.
	 * 
	 * @return choice of request.
	 */
	private static int getChoice() {
		
		System.out.println("Enter your choice: \n1.Get Image \n2. Get Sensor Data \n3. Close Station");
		int choice = scan.nextInt();
		return choice;
	}
}
