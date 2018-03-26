import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class Packets {
	
	//Opcodes
	//Opcodes are 16 bits but we only need a few so we'll just add in an empty byte later
	//Not really sure why we need 2 bytes for tiny numbers but who am I to judge
	static final byte RRQ 		= 	0b00000001; //Read Request
	static final byte WRQ 		= 	0b00000010; //Write Request
	static final byte DATA		= 	0b00000011; //Data
	static final byte ACK		= 	0b00000100; //Acknowledgment
	static final byte ERROR		= 	0b00000101; //Error
	
	//Creates a read request packet
	public static DatagramPacket createRRQPacket( String fileName ) throws UnknownHostException{
		
		byte[][] messageGroup = {
				new byte[]{ 0, RRQ },
				fileName.getBytes(),
				new byte[] { 0 },
				"octet".getBytes(),
				new byte[] { 0 }
		};
		
		//Create and return the packet
		return byteArrayToPacket( messageGroup );
	}
	
	//Creates a write request packet
	public static DatagramPacket createWRQPacket( String fileName ) throws UnknownHostException{
		
		byte[][] messageGroup = {
				new byte[]{ 0, WRQ },
				fileName.getBytes(),
				new byte[] { 0 },
				"octet".getBytes(),
				new byte[] { 0 }
		};
		
		//Create and return the packet
		return byteArrayToPacket( messageGroup );
	}
	
	//Creates a data packet
	public static DatagramPacket createDATAPacket( short blockNum, byte[] data ) throws UnknownHostException{
		
		byte[][] messageGroup = {
				new byte[]{ 0, DATA },
				new byte[]{ (byte)( blockNum >> 8 ), (byte) blockNum }, //Converts blockNum into 2 bytes
				data
		};
		
		//Create and return the packet
		return byteArrayToPacket( messageGroup );
	}
	
	//Creates an ACK packet
	public static DatagramPacket createACKPacket( short blockNum ) throws UnknownHostException{
		
		byte[][] messageGroup = {
				new byte[]{ 0, ACK },
				new byte[]{ (byte)( blockNum >> 8 ), (byte) blockNum }, //Converts blockNum into 2 bytes
		};
		
		//Create and return the packet
		return byteArrayToPacket( messageGroup );
	}
	
	//Creates an error packet
	public static DatagramPacket createERRORPacket( short blockNum ) throws UnknownHostException{
		
		byte[][] messageGroup = {
				new byte[]{ 0, ACK },
				new byte[]{ (byte)( blockNum >> 8 ), (byte) blockNum }, //Converts blockNum into 2 bytes
		};
		
		//Create and return the packet
		return byteArrayToPacket( messageGroup );
	}
	
	public static DatagramPacket byteArrayToPacket( byte[][] input ) throws UnknownHostException{
		
		//Convert the 2D byte array into a byte array
		byte[] dataMessage = concatByteArrays( input );
		
		//Get address of the host, just in case
		InetAddress address = InetAddress.getByName( TFTPPrimary.host );
		
		//Create the packet
		DatagramPacket packet = new DatagramPacket( dataMessage, dataMessage.length, address, TFTPPrimary.currentPort );
		
		return packet;
	}
	
	//Converts a 2D byte array into a single byte array by concating them together
		public static byte[] concatByteArrays( byte[][] input ){
			
			//This is not the best way to do this.  At all.  I hope.
			
			//Determine the total length of all the data
			int totalLength = 0;
			for (int i = 0; i < input.length; i++) {
				totalLength += input[ i ].length;
			}
			
			//Create the output byte array
			byte[] output = new byte[ totalLength ];
			int curPos = 0;
			
			//For every input byte array
			for (int i = 0; i < input.length; i++) {
				byte[] curByteArray = input[ i ];
				
				//For every byte
				for (int j = 0; j < curByteArray.length; j++) {
					
					//Add it to our output array at position curPos and increment curPos
					output[ curPos ] = curByteArray[ j ];
					curPos++;
				}
			}
			
			//Output concated byte array
			return output;
		}
}
