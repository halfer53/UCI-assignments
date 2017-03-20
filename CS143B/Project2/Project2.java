import java.io.*;
import java.util.*;
import java.text.*;

class Project2{
	public static void main(String[] args){
		if(args.length != 1){
			System.out.println("please provide the input processes");
			return;
		}
		try{
			new Project2(args[0]).start();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	ArrayList<Process> procs = null;
	StringBuilder outputbuffer = null;
	String path = "";

	public Project2(String path) throws Exception{
		this.path = path;
	}
	interface Algorithm{
		public void Schedule();
	}

	public void start() throws Exception{
		File file = null;
		BufferedReader br = null;
		try{
			//read file
			file = new File(path);
			br = new BufferedReader(new FileReader(file));
		}catch(FileNotFoundException e){
			System.out.println("file not found");
		}
		
		try{
			String line = br.readLine();
			String[] sp = line.split(" ");
			if(sp.length %2!=0){
				throw new Exception("incorrect format");
			}
			procs = new ArrayList<Process>(sp.length/2);
			//create new processes
			for(int i=0;i<sp.length;i+=2){
				System.out.println(sp[i]+" "+sp[i+1]);
				procs.add(new Process(i/2+1,Integer.parseInt(sp[i]),Integer.parseInt(sp[i+1])));
			}
			outputbuffer = new StringBuilder();
		}finally{
			br.close();
		}

		FIFO();
		new SJF(procs).run();
		printPerformanceAndReset();
		new SRT(procs).run();
		printPerformanceAndReset();
		new MLF(procs).run();
		printPerformanceAndReset();


		String str = (file.getParent() == null ? "" : file.getParent()) + "10521466.txt";
		System.out.println(str);
		BufferedWriter out = new BufferedWriter(new FileWriter(str));
		System.out.println(outputbuffer.toString());
		out.write(outputbuffer.toString(),0,outputbuffer.length());

		out.close();
		
	}

	private void resetProcs(){
		for( int i=0;i< procs.size();i++ ){
			procs.get(i).reset();
		}
	}

	/**
	 * print out the performance, namely the turnaround time for all the processes
	 * should only be called after each scheduling algorithms
	 */
	private void printPerformanceAndReset(){
		StringBuilder sb = new StringBuilder();
		int sum = 0;
		for( int i=0;i< procs.size();i++ ){
			int turnaround = procs.get(i).getTurnAround();
			sb.append(turnaround + " ");
			sum+= turnaround;
		}
		double avg = (double)sum / (double)procs.size();
		sb.insert(0,new DecimalFormat(".##").format(avg)+" ");
		outputbuffer.append(sb.toString()+"\n");
		resetProcs();
	}

	private void FIFO(){
		LinkedList<Process> cprocs = new LinkedList<Process>(procs);
		int ticks = 0;

		Process curr = cprocs.poll();
		while(curr != null){

			if(ticks >= curr.getStartTime()){
				curr.QUANTUM--;
				ticks++;
				if(curr.QUANTUM == 0){
					curr.setEndTime(ticks);
					curr = cprocs.poll();
				}
			}else{//if process hasn't started yet, just increase the tick
				ticks++;
			}
			//System.out.println(ticks+" "+curr);
		}
		printPerformanceAndReset();
	}

	static class SJF{
		//incoming list, which holds the list of processes that hasn't arrived yet
		//e.g. if curr time is 2, and one process arrives at 3, then it sits in the incoming_list until 3
		//then transferred to procs_list
		LinkedList<Process> incoming_list = null;
		PriorityQueue<Process> procs_list = null;

		public SJF(ArrayList<Process> procs){
			incoming_list = new LinkedList<Process>(procs);
			procs_list = new PriorityQueue<Process>();
		}
		private void run(){
			
			int ticks = 0;
			Process idle = new Process(0,0,1);
			pickIncomingProcess(ticks);
			Process curr = procs_list.size() == 0 ? idle : procs_list.poll();
			do{
				curr.QUANTUM--;
				ticks++;
				//if the  process uses up its quantum
				if(curr.QUANTUM == 0){
					curr.setEndTime(ticks);
					//pick up the next shortest job
					pickIncomingProcess(ticks);
					curr = procs_list.poll();
					//if no job is found, set curr to idle
					if(curr == null){
						idle.QUANTUM = 1;
						curr = idle;
					}
				}
				//System.out.println(ticks+" "+curr);
			}while(curr != idle || incoming_list.size() != 0 || procs_list.size()!=0);
		}
		//pick up any new arriving process at time t
		private void pickIncomingProcess(int ticks){
			for(Iterator it = incoming_list.iterator();it.hasNext();){
				Process pp = (Process)it.next();
				if(pp.getStartTime() <= ticks){
					it.remove();
					procs_list.add(pp);
					//System.out.println("PICK" + pp);
				}else{
					break;
				}
			}
		}

	}



	static class SRT{
		LinkedList<Process> incoming_list = null;
		PriorityQueue<Process> procs_list = null;

		public SRT(ArrayList<Process> procs){
			incoming_list = new LinkedList<Process>(procs);
			procs_list = new PriorityQueue<Process>();
		}
		private void run(){
			
			int ticks = 0;
			int count = 0;
			Process idle = new Process(0,0,1);
			pickIncomingProcess(ticks);
			Process curr = procs_list.size() == 0 ? idle : procs_list.poll();
			do{
				curr.QUANTUM--;
				ticks++;
				System.out.println(ticks+" "+curr);
				//add process back to the queue if it hasn't finished
				if(curr.QUANTUM == 0){
					curr.setEndTime(ticks);
				}else{
					procs_list.add(curr);
				}
				//add any arriving process to the scheduling queue
				pickIncomingProcess(ticks);
				//pick the lowest remaining time
				curr = procs_list.poll();
				//set current process to idle if no process is scheduled to run
				if(curr == null){
					idle.QUANTUM = 1;
					curr = idle;	
				}else{
					count++;
				}
				
			}while(curr != idle || incoming_list.size() != 0 || procs_list.size()!=0);
		}
		//pick up any new arriving process at time t
		private void pickIncomingProcess(int ticks){
			for(Iterator it = incoming_list.iterator();it.hasNext();){
				Process pp = (Process)it.next();
				if(pp.getStartTime() <= ticks){
					it.remove();
					procs_list.add(pp);
					//System.out.println("PICK" + pp);
				}else{
					break;
				}
			}
		}

	}


	static class MLF{
		//indicate the type of interrupt
		public enum Interrupt {
	        BLOCKED,
	        READY,
	        LOWER_PRIORITY,
	        RUNNING,
	        RESUME
	    }
	    //int indicating the top-level FIFO
	    int toppriority = 4;

		int capacity = toppriority +1;
	    ArrayList<LinkedList<Process>> ready_queue = null;
	    //block lsit holding the blocked process
		LinkedList<Process> block_list = null;
		//list holding the process that has not yet been added to the ready queue
		ArrayList<Process> incoming_list =null;

	    public MLF(ArrayList<Process> procs){
	    	ready_queue = new ArrayList<LinkedList<Process>>(capacity);
			block_list = new LinkedList<Process>();
			incoming_list = new ArrayList<Process>();

			//initialise ready queue
			for( int i=0;i<capacity;i++){
				ready_queue.add(new LinkedList<Process>());
			}
			
			//
			for(Iterator it = procs.iterator();it.hasNext();){
				Process p = (Process)it.next();

				p.setPriority(toppriority);
				//if quantum is below the maximum allowed slice for its current priority, set slice to the rest of 
				//the quantum, and set quantum to 0.
				//otherwise subtract the toplevel slice from the quantum
				//e.g. if current priority level is 3, for which is the time slice is 2
				//if there is a process with quantum size 3, then its SLICE becomes 2,and QUANTUM becomes 1
				//or if the process has only 1 quantum, its SLICE becomes 1, and QUANTUM becomes 0
				p.SLICE = Math.min(p.QUANTUM,getSliceForPriority(p.getPriority()));
				p.QUANTUM = Math.max(0,p.QUANTUM - p.SLICE); 

				if(p.getStartTime() == 0){
					ready_queue.get(toppriority).add(p);
				}else{
					incoming_list.add(p);
				}
			}
			debug(null);
	    }

		private void run(){
			int ticks = 0;
			Process idle = new Process(0,0,0);
			Process curr = null;
			do{
				//do block work
				if(block_list.size() != 0){
					Process pp = block_list.poll();
					event(pp,Interrupt.RESUME);
				}
				//pick up the next process
				curr = pick_next();
				//if no process is scheduled to run, pick idle
				if(curr == null){
					idle.SLICE =  1;
					idle.setPriority(toppriority);
					curr = idle;
				}

				while(curr.SLICE > 0){
					ticks++;
					curr.SLICE--;
					System.out.println("\n-------------------------ticks " + ticks + "---------------------------");
					debug(curr);
					//if there is new coming processes arriving at the current time
					if(newIncomingProcesses(ticks)){
						System.out.println("curr slice "+curr.SLICE);
						//interrupt the current process if it hasn't finished its turn yet
						if(curr.SLICE > 0){
							event(curr,Interrupt.BLOCKED);
							System.out.println("BLOCKED: "+curr);
						}else{
							//otherwise lower its priority to the next queue
							//or if the process is done, destory it
							if (curr.QUANTUM == 0) {
								curr.setNone();
								curr.setEndTime(ticks);
							}else{
								event(curr,Interrupt.LOWER_PRIORITY);
							}
						}
						curr = pick_next();
					}
				}

				if(curr.getStatus() == Process.Status.RUNNING){
					if(curr.SLICE == 0){
						if (curr.QUANTUM == 0) {
							curr.setNone();
							curr.setEndTime(ticks);
						}else{
							event(curr,Interrupt.LOWER_PRIORITY);
						}
					}
					
				}

			}while(curr != idle || !readyEmpty() || block_list.size() != 0 || incoming_list.size() != 0);
		}

		/**
		 * check if there is any process in the scheduling queue
		 * @return [description]
		 */
		private boolean readyEmpty(){
			for(int i=0;i<ready_queue.size();i++){
				if(ready_queue.get(i).size()!=0){
					return false;
				}
			}
			return true;
		}

		private void debug(Process curr){
			System.out.println("CURR "+curr);
					System.out.println("ready_queue : ");
					for(int i=ready_queue.size()-1;i>=0;i--){
						String s = "";
						for(int j=0;j<ready_queue.get(i).size();j++){
							s+= (ready_queue.get(i).get(j)).toString() +" || ";
						}
						if(!s.equals("")){
							System.out.println(" 	level "+i +" : "+s);
						}
					}
					String s = "Blocked list: ";
					for(int i=0;i<Math.min(10,block_list.size());i++){
						s += block_list.get(i).toString()+" || ";
					}
					System.out.println(s);
					s = "Incoming List: ";
					for(int i=0;i<Math.min(10,incoming_list.size());i++){
						s += incoming_list.get(i).toString()+" || ";
					}
					System.out.println(s+"\n");
		}

		//check if there is a new proceess starting at the current time
		//if there is one, and if the current process hasn't finished its turn, block the current process
		private boolean newIncomingProcesses(int ticks){
			LinkedList<Process> templist = new LinkedList<Process>();
			boolean has = false;
			for(Iterator it = incoming_list.iterator();it.hasNext();){
				Process p = (Process)it.next();
				if(ticks == p.getStartTime()){
					System.out.println(" New Incoming Process"+p);
					it.remove();
					event(p,Interrupt.READY);
					has = true;
				}else{
					break;
				}
			}
			return has;
		}

		private int getSliceForPriority(int priority){
			int[] prioritylevel_slice = new int[]{16,8,4,2,1};
			return prioritylevel_slice[priority];
		}

		/**
		 * simulation of system interrupt
		 * @param curr      [description]
		 * @param interrupt [description]
		 */
		//IMPORTANT curr must not belong to any queue, either block list or ready queue or incoming list
		private void event(Process curr, Interrupt interrupt ){
			switch(interrupt){
					case RUNNING:
						curr.setRunning();
						break;
					case BLOCKED:
						curr.setBlocked();
						block_list.add(curr);
						break;
					case READY:
						curr.setReady();
						ready_queue.get(curr.getPriority()).add(curr);
						break;
					case RESUME:
						curr.setReady();
						ready_queue.get(curr.getPriority()).addFirst(curr);
						break;
					case LOWER_PRIORITY:
						//continue to run at the lowest level if it's already at lowest priority
						//else, lower it's priority level to the next queue
						curr.setReady();
						ready_queue.get(Math.max(0,curr.lowerPriority())).add(curr);
						//if quantum is below the maximum allowed slice for its current priority, set slice to the rest of 
						//the quantum, and set quantum to 0.
						//otherwise subtract the toplevel slice from the quantum
						//e.g. if current priority level is 3, for which is the time slice is 2
						//if there is a process with quantum size 3, then its SLICE becomes 2,and QUANTUM becomes 1
						//or if the process has only 1 quantum, its SLICE becomes 1, and QUANTUM becomes 0
						curr.SLICE = Math.min(curr.QUANTUM,getSliceForPriority(curr.getPriority()));
						curr.QUANTUM = Math.max(0,curr.QUANTUM - curr.SLICE); 
			}

			System.out.println("MAKE " + interrupt.toString() + curr);
		}

		/**
		 * pick the next process with highest priority to run in the 5 level queue
		 * @return return null if no process can be found
		 */
		private Process pick_next(){
			for(int i=ready_queue.size()-1;i>=0;i--){
				if(ready_queue.get(i).size() != 0){
					Process p = ready_queue.get(i).poll();
					p.setRunning();
					System.out.println("pick "+p.toString() + " left "+ready_queue.get(i).size());
					// ready_queue.get(i).remove(p);
					return p;
				}
					
			}
			return null;
		}
	}


}