package Protocol;

import java.io.*;
public class Converter {
    public final static int PORT = 60000;

    public static Object byteToObject(byte[] b) throws IOException, ClassNotFoundException {
        ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(b));
        Object o = iStream.readObject();
        iStream.close();
        return o;
    }
    public static byte[] objectToByte(Object o) throws IOException {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(bStream);
        oo.writeObject(o);
        oo.close();
        return bStream.toByteArray();
    }
}