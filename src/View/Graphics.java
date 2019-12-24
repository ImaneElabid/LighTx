//package View;
//
//import org.leores.plot.JGnuplot;
//import org.leores.util.data.DataTableSet;
//
//import java.io.*;
//import java.util.ArrayList;
//import java.util.Scanner;
//
///**
// * @author EMS
// */
//public class Graphics {
//
////    public static void plot2d() {
////        JGnuplot jg = new JGnuplot();
////        JGnuplot.Plot plot = new JGnuplot.Plot("") {
////            {
////                xlabel = "x";
////                ylabel = "y";
////            }
////        };
////        double[] x = { 1, 2, 3, 4, 5 }, y1 = { 2, 4, 6, 8, 10 }, y2 = { 3, 6, 9, 12, 15 };
////        DataTableSet dts = plot.addNewDataTableSet("2D Plot");
////        dts.addNewDataTable("y=2x", x, y1);
////        dts.addNewDataTable("y=3x", x, y2);
////        jg.execute(plot, jg.plot2d);
////    }
//
////    public void whenAppendStringUsingBufferedWritter_thenOldContentShouldExistToo()
////            throws IOException {
////        String str = "World";
////        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
////        writer.append(' ');
////        writer.append(str);
////
////        writer.close();
////    }
//
//    public static void read(String filename) throws FileNotFoundException {
//        Scanner scanner = new Scanner(new File(filename));
//        scanner.useDelimiter(",");
//        ArrayList<Double> x = new ArrayList<>();
//        ArrayList<String> y = new ArrayList<>();
//
//        while (scanner.hasNext()) {
//            x.add(scanner.nextDouble());
//            y.add(scanner.next());
//        }
//        scanner.close();
//    }
//
//    public static void main(String[] args) throws FileNotFoundException {
//        read("F:/PhD/Projects/reliablebroadcast/sample.csv");
//        JGnuplot jg = new JGnuplot();
//        JGnuplot.Plot plot = new JGnuplot.Plot("") {
//            {
//                xlabel = "x";
//                ylabel = "y";
//            }
//        };
//        double[] x = { 1, 2, 3, 4, 5 }, y1 = { 2, 4, 6, 8, 10 }, y2 = { 3, 6, 9, 12, 15 };
//        DataTableSet dts = plot.addNewDataTableSet("2D Plot");
//        dts.addNewDataTable("y=2x", x, y1);
//        dts.addNewDataTable("y=3x", x, y2);
//        jg.execute(plot, jg.plot2d);
//    }
//}
