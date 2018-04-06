import java.io.File;

import java.lang.*;
import java.io.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;


public class TFTPPrimary {
	
	//Configurable options
	public static int defaultPort 	=	69;
	public static int currentPort	=	defaultPort;
	public static String host 		=	"192.168.1.226";
	//static final String host 		=	"127.0.0.1";
	public static int maxDataBytes	=	512;
	public static final int OCTET	=	1;
	public static final int NETASCII=	2;
	
	public static void main(String[] args) throws IOException {
		
		//sendFile( host );
		getFile( host, "face.png", OCTET );
	}
	
	//Select and send file
	public static void sendFile( String host, int mode ) throws IOException{
		
		//Create the file chooser
		JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		jfc.setCurrentDirectory( new File( System.getProperty("user.dir") ) );
		
		File selectedFile;
		
		int returnValue = jfc.showOpenDialog(null);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			selectedFile = jfc.getSelectedFile();
			
			sendFile( host, selectedFile, mode );
		}else{
			System.out.println( "No file selected.  Exiting..." );
			System.exit( 0 );
		}
		
		
	}
	
	//Sends a file to the given host
	public static void sendFile( String host, File file, int mode ) throws IOException{
		
		//Get our TID/source port
		int sourcePort = getTID();
		
		//Get address of the host, just in case
		InetAddress address = InetAddress.getByName( host );
		
		//Create a socket
		DatagramSocket dsocket = new DatagramSocket( sourcePort );
		
		//Send a write request packet
		System.out.println( "Sending write request on port: " + sourcePort );
		System.out.println( "Requesting to write " + file.getName() );
		DatagramPacket requestPacket = Packets.createWRQPacket( file.getName() );
		dsocket.send( requestPacket );
		
		//Wait for reply
		DatagramPacket recievePacket = Packets.createACKPacket( new byte[] { 0, 0 } );
		dsocket.receive( recievePacket );
		
		//Check if we got an ACK
		if( recievePacket.getData()[1] != 4 ){
			System.out.println( "Recieved error. Exiting..." );
			System.exit( 0 );
		}else{
			System.out.println( "Recieved ACK" );
		}
		
		//Get the source port from the ACK to use as new destination port for this connection
		currentPort = recievePacket.getPort();
		
		byte[] buffer = new byte[ maxDataBytes ];
		
		InputStream input = new FileInputStream( file );
		
		short currentBlock = 1;
		
		while( input.read( buffer ) != -1 ){
			
			//Send the next block
			DatagramPacket dataPacket = Packets.createDATAPacket( currentBlock, buffer );
			dsocket.send( dataPacket );
			
			//Wait for ACK
			DatagramPacket dataACKPacket = Packets.createACKPacket( new byte[] { 0, 0 } );
			dsocket.receive( dataACKPacket );
			
			//Check if we got an ACK
			if( dataACKPacket.getData()[1] != 4 ){
				System.out.println( "Recieved error. Exiting..." );
				System.exit( 0 );
			}
			
			//Advance the current block number
			currentBlock++;
		}
		
		//Send closing packet, just in case
		System.out.println( "Sending closing packet" );
		DatagramPacket closingPacket = Packets.createDATAPacket( currentBlock, new byte[1] );
		dsocket.send( closingPacket );
		
		System.out.println( "Finished." );
		
		input.close();
		dsocket.close();
		
		//Reset the port
		currentPort = defaultPort;
		
	}
	
	//Gets the specified file from the server
	public static void getFile( String host, String fileName, int mode ) throws IOException{
		
		//Get our TID/source port
		int sourcePort = getTID();
		
		//Get address of the host, just in case
		InetAddress address = InetAddress.getByName( host );
		
		//Create a socket
		DatagramSocket dsocket = new DatagramSocket( sourcePort );
		
		//Send a read request packet
		System.out.println( "Sending read request on port: " + sourcePort );
		System.out.println( "Requesting to read " + fileName );
		DatagramPacket requestPacket = Packets.createRRQPacket( fileName );
		dsocket.send( requestPacket );
		
		//Create file output
		FileOutputStream output = new FileOutputStream( fileName );
		
		//Path path = Paths.get( fileName );
		
		//Get first data packet
		DatagramPacket recievePacket = Packets.createDATAPacket( (short) 1, new byte[ maxDataBytes ] );
		dsocket.receive( recievePacket );
		
		System.out.println( "Got first data packet" );
		
		//Once we have the packet, send an ACK
		byte[] data = recievePacket.getData();
		byte[] blockNum = new byte[] { data[2], data[3] };
		
		//It's not supposed to, but it looks like some servers send an ACK for a read request instead of the first data packet.  This is for that case.
		if( data[1] != 4 ){
			
			//Trim 4 control bytes from data
			data = Arrays.copyOfRange( data, 4, data.length );
			
			//Write to file
			//output.write( data );
		}
		
		//Get the source port from the DATA to use as new destination port for this connection
		currentPort = recievePacket.getPort();
		System.out.println( "Source port is " + currentPort );
		
		//Keep getting data packets
		//Playing with fire by using a while true
		while( true ){
			
			//Wait for data packet
			recievePacket = Packets.createDATAPacket( (short) 1, new byte[ maxDataBytes ] );
			dsocket.receive( recievePacket );
			
			//Once we have the packet, send an ACK
			data = recievePacket.getData();
			blockNum = new byte[] { data[2], data[3] };
			DatagramPacket ackPacket = Packets.createACKPacket( blockNum );
			dsocket.send( ackPacket );
			
			//Trim 4 control bytes from data
			data = Arrays.copyOfRange( data, 4, data.length );
			
			//Write to file
			//If the most recent data packet had fewer than the maximum number of data bytes, this is the end of the file
			if( recievePacket.getLength() < maxDataBytes ){
				byte[] clippedData;
				clippedData = Arrays.copyOfRange( data, 0, recievePacket.getLength() );
				output.write( clippedData );
				//Files.write(path, clippedData, StandardOpenOption.WRITE);
				break;
			}else{
				//Write to file normally
				output.write( data );
				//Files.write(path, data, StandardOpenOption.WRITE);
			}
		}
		
		System.out.println( "Finished reading file." );
		
		
		//Flush and close
		//output.flush();
		//output.close();
		dsocket.close();
		
		//Reset the port
		currentPort = defaultPort;
		
	}
	
	public static String byteArrayToString( byte[] input ){
		String out = "";
		for (int i = 0; i < input.length; i++) {
			out += input[ i ];
		}
		return out;
	}
	
	//Generates a random TID between 0 and 65535
	//Used as source port
	public static int getTID(){
		return new Random().nextInt( 65535 );
	}

}
