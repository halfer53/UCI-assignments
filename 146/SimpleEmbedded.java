import org.python.core.*;
import java.io.*;
import org.python.util.PythonInterpreter;

public class SimpleEmbedded
{
    public static void main(String[] args) throws PyException, IOException
    {
        BufferedReader terminal;
        PythonInterpreter interp;
        terminal = new BufferedReader(new InputStreamReader(System.in));
        // System.out.println ("Hello");
        interp = new PythonInterpreter();
        interp.exec("import sys");
        interp.exec("print 'I got'");
        for(String s:args){
            interp.exec("print '"+s+"'");
        }
        interp.exec("print 'from the command line arguments'");

        
        // interp.set("a", new PyInteger(42));
        // interp.exec("print a");
        // interp.exec("x = 2+2");
        // PyObject x = interp.get("x");
        // System.out.println("x: " + x);
        // PyObject localvars = interp.getLocals();
        // interp.set("localvars", localvars);
        // String codeString = "";
        // String prompt = ">> ";
        // while (true)
        // {
        //     System.out.print (prompt);
        //     try
        //     {
        //         codeString = terminal.readLine();
        //         if (codeString.equals("exit"))
        //         {
        //             System.exit(0);
        //             break;
        //         }
        //         interp.exec(codeString);
        //     }
        //     catch (IOException e)
        //     {
        //         e.printStackTrace();
        //     }
        // }
        // System.out.println("Goodbye");
    }
}