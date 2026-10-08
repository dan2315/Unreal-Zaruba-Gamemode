import java.io.*; import java.util.*; import java.util.zip.*;
public class DumpNbt {
  public static void main(String[] a) throws Exception {
    DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(a[0]))));
    in.readByte(); in.readUTF(); dump(in, 10, "", Integer.parseInt(a[1]));
  }
  static void dump(DataInputStream in, int t, String ind, int depth) throws IOException {
    switch (t) {
      case 1 -> System.out.println(in.readByte()); case 2 -> System.out.println(in.readShort()); case 3 -> System.out.println(in.readInt());
      case 4 -> System.out.println(in.readLong()); case 5 -> System.out.println(in.readFloat()); case 6 -> System.out.println(in.readDouble());
      case 7 -> { int n=in.readInt(); in.skipBytes(n); System.out.println("byte["+n+"]"); }
      case 8 -> System.out.println("\"" + in.readUTF() + "\"");
      case 9 -> { int et=in.readByte(); int n=in.readInt(); System.out.println("list<"+et+">["+n+"]"); for (int i=0;i<n;i++){ if (i<2 && depth>0) { System.out.print(ind+"  - "); dump(in, et, ind+"    ", depth-1);} else skip(in, et);} }
      case 10 -> { System.out.println("{"); while(true){ int ct=in.readByte(); if(ct==0)break; String k=in.readUTF(); if (depth>0){System.out.print(ind+"  "+k+": "); dump(in, ct, ind+"  ", depth-1);} else skip(in, ct);} System.out.println(ind+"}"); }
      case 11 -> { int n=in.readInt(); in.skipBytes(n*4); System.out.println("int["+n+"]"); }
      case 12 -> { int n=in.readInt(); in.skipBytes(n*8); System.out.println("long["+n+"]"); }
    }
  }
  static void skip(DataInputStream in, int t) throws IOException {
    switch (t) { case 1 -> in.skipBytes(1); case 2 -> in.skipBytes(2); case 3,5 -> in.skipBytes(4); case 4,6 -> in.skipBytes(8);
      case 7 -> in.skipBytes(in.readInt()); case 8 -> in.readUTF(); case 9 -> { int et=in.readByte(); int n=in.readInt(); for(int i=0;i<n;i++) skip(in, et);} 
      case 10 -> { while(true){int ct=in.readByte(); if(ct==0)break; in.readUTF(); skip(in, ct);} } case 11 -> in.skipBytes(in.readInt()*4); case 12 -> in.skipBytes(in.readInt()*8); }
  }
}
