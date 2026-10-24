import java.nio.file.*;

/** Fills low ground in a box up to y with dirt and a grass top (map's painted-grass mix): banks, ditches. Usage: java BankFill <region dir> x0 z0 x1 z1 y */
public class BankFill { public static void main(String[] a) throws Exception { Region r = new Region(Path.of(a[0]));
 int x0=Integer.parseInt(a[1]), z0=Integer.parseInt(a[2]), x1=Integer.parseInt(a[3]), z1=Integer.parseInt(a[4]), ty=Integer.parseInt(a[5]);
 System.out.println("# worldkit build: fill the ditch between the BLUE spawn plots and the main road up to y " + ty);
 for (int x=x0;x<=x1;x++) for (int z=z0;z<=z1;z++) { int g = r.top(x, z, s -> { String n = Region.name(s).replace("minecraft:", ""); return !n.endsWith("leaves") && !BorderHills.plant(n) && !n.equals("water"); });
  if (g==Integer.MIN_VALUE || g>=ty) continue; if (g < ty-1) System.out.println("place "+x+" "+(g+1)+" "+z+" "+x+" "+(ty-1)+" "+z+" minecraft:dirt");
  double n = BorderHills.hash(x, z); System.out.println("place "+x+" "+ty+" "+z+" "+x+" "+ty+" "+z+" minecraft:"+(n<0.6?"grass_block":n<0.85?"lime_terracotta":"green_concrete_powder")); } } }
