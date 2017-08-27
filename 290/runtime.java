import java.lang.Runtime.*;
import java.io.*;
import java.util.*;


class runtime{

	public runtime(){

	}

	public void processBuilder(String[] sargs) throws Exception{
		List<String> args = new ArrayList<String>(Arrays.asList("python3","script.py"));
		args.addAll(new ArrayList<String>(Arrays.asList(sargs)));
		
		ProcessBuilder pb = new ProcessBuilder(args);
		Process p = pb.start();
		 
		BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s = null;
		while ((s = input.readLine()) != null) {
		    System.out.println(s);
		}
		input.close();
	}

	public void exec() throws Exception{
		Process p = Runtime.getRuntime().exec(
			new String[]{"python3", "script.py", "a"});

		BufferedReader input = new BufferedReader(new 
     		InputStreamReader(p.getInputStream()));

		BufferedWriter writer = new BufferedWriter(
		    new OutputStreamWriter(p.getOutputStream()));

		String s = null;
		while ((s = input.readLine()) != null) {
		    System.out.println(s);
		}
		input.close();

		writer.close();	
	}

	public static void main(String[] argss) {
		try{
			BufferedReader input= null;
			String path = "";
			path += "script.py";
			System.out.println(path);
			for(int i=0;i<1;i++){
				
				List<String> args = new ArrayList<String>(Arrays.asList("python3",path));
				args.addAll(new ArrayList<String>(Arrays.asList("\"Hello Mr. Smith, how are you doing today? The weather is great, and Python is awesome. The sky is pinkish-blue. You shouldn't eat cardboard\"")));
				
				ProcessBuilder pb = new ProcessBuilder(args).redirectInput(Redirect.INHERIT).redirectOutput(Redirect.INHERIT).redirectError(Redirect.INHERIT);
				
				Process p = pb.start();
				
				try {
				    
				    int exitValue = p.exitValue();
				    System.out.println(exitValue);
				    // get the process results here
				} catch (IllegalThreadStateException e) {
				    // process hasn't terminated in 5 sec
				    p.destroy();
				}
				input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String s = null;
				while ((s = input.readLine()) != null) {
				    System.out.println(s);
				}
				p.waitFor();
				
				
			}
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
}

