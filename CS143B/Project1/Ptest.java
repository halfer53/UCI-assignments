import java.io.*;
class Ptest{
    public static void main(String[] args){
        try{
            BufferedWriter out = new BufferedWriter(new FileWriter("out.txt"));
            out.write("abc");
            out.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }
}