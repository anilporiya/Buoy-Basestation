import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
//import java.net.SocketException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Scanner;

import javax.imageio.ImageIO;

/***
 * Buoy class will send the Image or Sensor Data depending
 * on what Base station has requested.
 * For Sensor Data --> Prompt user to input the sensor data.
 * For Image --> Prompt user for the entire file path of image including
 * extension
 * 
 * @author Anil Poriya arp6763
 *
 */
public class Buoy {
	public static Scanner scan = new Scanner(System.in);
	public static void send(String ipAdd, int portOut, int portIn) throws IOException{
		
		DatagramSocket buoy = new DatagramSocket(portIn);
		InetAddress inetAdd;
		inetAdd = getInetAdd(ipAdd);
		DatagramPacket buoyPacket;
		while(true){
			
			byte[] requested = new byte[1];
			buoyPacket = new DatagramPacket(requested,requested.length);
			buoy.receive(buoyPacket);
			int choice = requested[0];
			
			if(choice == 2){
				sendSensorData(inetAdd,portOut,buoy);
			}
			else if(choice == 3){
				break;
			}
			else if(choice == 1){
				sendImage(inetAdd,portOut,buoy);
			}
			//scan.close();
		}
		
		System.out.println();
		System.out.println("Done Serving Basestation");
		buoy.close();
		scan.close();
	}
	
	private static void sendImage(InetAddress inetAdd, int portOut, DatagramSocket buoy) throws IOException {
		int maxSegSize = 1019;
		ArrayList<Image_Message> packetList = new ArrayList<>();
		Image_Message message = new Image_Message();
		byte[] nullData = new byte[0];
		message.data = nullData;
		packetList.add(0,message);
		
		System.out.println("Enter the full path of the Image including .txt extension");
		String file = scan.nextLine();
		BufferedImage imageToSend = ImageIO.read(new File(file));
		ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
		ImageIO.write(imageToSend,"jpg",imageOutputStream);
		imageOutputStream.flush();
		byte[] imageData = imageOutputStream.toByteArray();
		//byte[] size = new byte[4];
		byte[] size = ByteBuffer.allocate(4).putInt(imageData.length).array();
//		for(int i = 0;i<4;i++){
//			size[i] = length[i];
//		}
		DatagramPacket imagePacket;
		imagePacket = new DatagramPacket(size,size.length,inetAdd,portOut);
		buoy.send(imagePacket);
		int noOfPackets = (imageData.length/maxSegSize)+1;
		boolean[] track = new boolean[noOfPackets+1];
		int start = 0;
		int i;
		for(i=1;i<noOfPackets;i++){
			Image_Message mess = new Image_Message();
			byte[] data = new byte[1024];
			byte[] sequenceNum = ByteBuffer.allocate(4).putInt(i).array();
			for(int j = 0;j<4;j++){
				data[j] = sequenceNum[j];
			}
			data[4] = 0;
			for(int j = 5;j<data.length;j++){
				data[j] = imageData[start];
				start++;
				//endOfMessage++;
			}
			mess.data = data;
			packetList.add(i,mess);
		}

		byte[] data = new byte[imageData.length-start+4];
		if(data.length > 4){
			byte[] sequenceNum = ByteBuffer.allocate(4).putInt(i).array();
			for(int j = 0;j<4;j++){
					data[j]= sequenceNum[j];
			}
			for(int j = 4;j<data.length;j++){
				data[j] = imageData[start];
				start++;
			}
		}
		Image_Message mess = new Image_Message();
		mess.data = data;
		packetList.add(i,mess);
		while(!allReceived(track)){
				
				int num = 1;
				for(num =1;num<track.length;num++){
					if(!track[num]){
						break;
					}
				}
				
				Image_Message packet = packetList.get(num);
				byte[] dataToSend = packet.data;
				if(num == track.length-1){
					dataToSend[4] = 1;
				}
				imagePacket = new DatagramPacket(dataToSend,dataToSend.length,inetAdd,portOut);
				boolean ACK = false;
				while(!ACK){
					
					buoy.send(imagePacket);
					byte[] resp = new byte[4];
					DatagramPacket recPacket = new DatagramPacket(resp,resp.length);
					buoy.setSoTimeout(10000);
					try {
						buoy.receive(recPacket);
					} catch (SocketTimeoutException e) {
						//System.out.println("Resending");
						continue;
					}

					buoy.setSoTimeout(0);
					int seq = ByteBuffer.wrap(resp).getInt();
					track[seq] = true;
					ACK = true;
				}	
		}

		
		
		
	}

	private static boolean allReceived(boolean[] track) {
		for(int i = 1;i<track.length;i++){
			if(track[i] == false)
				return false;
		}
		return true;
	}

	/***
	 * Takes sensor data from the user and sends this sensor data
	 * across the network to the base station.
	 * 
	 * @param inetAdd InetAddress of the buoy.
	 * @param portOut the port to send the packet to.(port of base station)
	 * @param buoy  the current socket which is used for sending packets.
	 * @throws IOException
	 */
	private static void sendSensorData(InetAddress inetAdd, int portOut, DatagramSocket buoy) throws IOException {
			//Scanner scan = new Scanner(System.in);
			System.out.println("Basestation has requested sensor data: ");
			int data = scan.nextInt();
			byte[] dataToSend = ByteBuffer.allocate(4).putInt(data).array();
			DatagramPacket sensorData = new DatagramPacket(dataToSend,dataToSend.length,inetAdd,portOut);
			buoy.send(sensorData);
	
	}
	
	/***
	 * returns the InetAddress of the buoy based on what String of
	 * IP address was sent as a command line parameter.
	 * @param ipAdd IP address string passed as a command line parameter.
	 * @return
	 * @throws UnknownHostException
	 */
	private static InetAddress getInetAdd(String ipAdd) throws UnknownHostException {
		if(ipAdd.equals("")){
			return InetAddress.getLocalHost();
		}
		else{
			return InetAddress.getByName(ipAdd);
		}
	}

}
