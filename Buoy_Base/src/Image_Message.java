
/**
 * Image_Message class objects will contain chunks of data
 * from the original data buffer of the image file.
 * The class is maintained to store the chunks of bytes
 * stored somewhere and only send on adhoc basis, the ones
 * for which ACK have not been received.
 * 
 * @author Anil Poriya
 *
 */
public class Image_Message{
	byte[] data;
}
