import java.nio.file.*; import java.util.*;
public class Probe { public static void main(String[] a) throws Exception { Path d = Path.of(a[0]);
  for (int i = 1; i + 2 < a.length; i += 3) { int x = Integer.parseInt(a[i]), y = Integer.parseInt(a[i+1]), z = Integer.parseInt(a[i+2]);
    var col = Surface.column(d, x, z); System.out.println(x+" "+y+" "+z+": "+col.getOrDefault(y,"air")+" | above: "+col.getOrDefault(y+1,"air")+", "+col.getOrDefault(y+2,"air")); } } }
