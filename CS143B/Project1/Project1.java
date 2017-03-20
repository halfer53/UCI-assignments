import java.io.*;
import java.util.*;
import java.util.stream.*;

class Project1{
	public static void main(String[] args){
		try{
			if(args.length != 1){
				System.out.println("please provide the path of the input file");
				return;
			}
			new Project1().Start(args[0]);
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	//current running process
	private Process self = null;
	//ready queues, dimentional 1 is the priority level, dimention 2 is the linkedlist of processes in this priority
	private ArrayList<LinkedList<Process>> ready_queue = new ArrayList<LinkedList<Process>>(3);
	//list of resources available
	private Resource[] res = new Resource[4];
	//list of processes being created
	private LinkedList<Process> processes = new LinkedList<Process>();
	//temp string for output
	private StringBuilder output = new StringBuilder();

	StringBuilder input = new StringBuilder();

	/**
	 * print the current state of the OS for debugging purpose
	 */
	private void overview(){
		if (self != null) {
			System.out.println("curr "+self.getName());
		}
		Iterator it = processes.iterator();
		while(it.hasNext()){
			Process p = (Process)it.next();
			System.out.println(p);
		}
		for(int i=2;i>=0;i--){
			if(!ready_queue.get(i).isEmpty()){
				Iterator itp = ready_queue.get(i).iterator();
				System.out.print("queue priority " + i +" : ");
				while(itp.hasNext()){
					Process p = (Process)itp.next();
					System.out.print(p.getName() +" | ");
				}
				System.out.println();
			}
		}
		System.out.println();
		for (int i=0; i<4;i++ ) {
			System.out.println((Resource)res[i]);
		}
	}

	/**
	 * Start position of the OS
	 * @param  path      [description]
	 * @throws Exception [description]
	 */
	public void Start(String path) throws Exception{
		File file = null;
		BufferedReader br = null;
		try{
			file = new File(path);
			br = new BufferedReader(new FileReader(file));
		}catch(FileNotFoundException e){
			System.out.println("file not found");
		}
		
		try{
			init();
			String line = br.readLine();
			while(line!=null){
				input.append(line+"\n");
				parse(line);
				overview();
				line = br.readLine();
			}
		}finally{
			//overview();
			//System.out.println(output);
			
		}

		
		System.out.println(output.toString());
		System.out.println(input.toString());
		String str = (file.getParent() == null ? "" : file.getParent()) + "10521466.txt";
		System.out.println(str);
		BufferedWriter out = new BufferedWriter(new FileWriter(str));
		out.write(output.toString(),0,output.length());

		out.close();
		br.close();
	}

	/**
	 * parse the input line, appropriate function is called for each command
	 * @param  input     [description]
	 * @throws Exception [description]
	 */
	public void parse(String input) throws Exception{
		try{
			String[] sp = input.split(" ");
			System.out.println(input + "+++++++++++++++");
			if (self == null) {
				throw new Exception("panic");
			}
			switch (sp[0]){
				case "init":
					init();
					break;
				case "cr":
					create(sp[1],Integer.parseInt(sp[2]));
					break;
				case "de":
					destroy(sp[1]);
					break;
				case "req":
					request(sp[1],Integer.parseInt(sp[2]));
					break;
				case "rel":
					release(sp[1],Integer.parseInt(sp[2]));
					break;
				case "to":
					time_Out();
					break;
				case "":
					output.append("\n");
					break;
				default:
					throw new Exception("illegal command");
			}
		}catch(Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
			output.append("error ");

		}
		
	}

	private void addToReadyList(Process p){
		int pri = p.getPriority();
		// System.out.println(p.getName()+" add to "+pri  );
		// if (pri == 0) {
		// 	for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
		// 	    System.out.println(ste);
		// 	}
		// }
		ready_queue.get(pri).addLast(p);
	}

	private void removeFromReadyList(Process p){
		int pri = p.getPriority();
		ready_queue.get(pri).remove(p);
	}

	/**
	 * called at the boot of the OS
	 * reset everything 
	 * @throws Exception [description]
	 */
	private void init() throws Exception{
		processes.clear();
		for(int i = 0;i<3;i++){
			ready_queue.add(i,new LinkedList<Process>());
		}
		self = new Process("init",0,null);
		processes.add(self);
		addToReadyList(self);
		for(int i =1;i<=4;i++){
			res[i-1] = new Resource(i);
		}
		schedule();
	}

	/**
	 * create new process, it is added as the child of the current process. process with duplicate name with the existing process
	 * cannot be created
	 * @param  name      [description]
	 * @param  priority  [description]
	 * @throws Exception [description]
	 */
	private void create(String name, int priority) throws Exception{

		List<Process> result = processes.stream().filter(item -> item.getName().equals(name)).collect(Collectors.toList());
		if (!result.isEmpty()) {
			throw new Exception("duplicate process name");
		}
		Process p = new Process(name,priority,self);
		self.addChild(p);
		addToReadyList(p);
		processes.add(p);
		schedule();
	}

	private Process get_PCB(String name){
		Iterator it = processes.iterator();
		while(it.hasNext()){
			Process p = (Process)it.next();
			if (p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}

	/**
	 * destroy the process with the given name
	 * side effect:
	 * this process is removed from parent's child_tree
	 * removed from processes and ready_queue
	 * if this process or its descendant is occupying resource, it's record on the resource is deleted
	 * if any process is blocked, and waiting for any resource, it's record is also deleted on the corresponding resource
	 * once the resource is freed due to deletion of the process, it's gonna check it's waiting list to see if it can serve the next request
	 * @param  name      [description]
	 * @throws Exception [description]
	 */
	private void destroy(String name) throws Exception{
		Process p = get_PCB(name);
		if(p == null){
			throw new Exception("can't find the specified process to be deleted");
		}
		//remove the current from the parent's child_tree 
		if (p.CREATION_TREE.PARENT != null) {
			p.CREATION_TREE.PARENT.CREATION_TREE.CHILD_TREE.remove(p);
		}
		//System.out.println("delete "+p);
		kill_Tree(p);

		schedule();
	}

	/**
	 * recursive call to delete the process, refer to the doc of destroy()
	 * @param  curr      [description]
	 * @throws Exception [description]
	 */
	private void kill_Tree(Process curr) throws Exception{
		//if it is running
		//System.out.println(curr);
		Iterator it = curr.CREATION_TREE.CHILD_TREE.iterator();
		while(it.hasNext()){
			Process child = (Process)it.next();
			kill_Tree(child);
		}
		
		curr.setStatus(Process.Status.none);
		removeFromReadyList(curr);
		processes.remove(curr);
		ready_queue = curr.releaseAllResources(ready_queue);
		curr.clearVal();
	}

	private Resource get_RCB(String name){
		if (name.length() == 2 && name.charAt(0) == 'R') {
			int rid = Character.getNumericValue(name.charAt(1));
			return res[rid-1];
		}
		return null;
	}

	/**
	 * request the resource specified by the parameter by the current running process
	 * side effect: if the requesting resource can serve the request with the given units, resource is included process's other_resource, and _requested_units
	 * if the reuesting resource is not free, process is modified as blocked, removed from the scheduling queue, and added to the resource waiting list 
	 * @param  name      [description]
	 * @param  units     [description]
	 * @throws Exception [description]
	 */
	private void request(String name, int units) throws Exception{
		Resource r = get_RCB(name);
		if (r == null) {
			throw new Exception("resource not found");
		}
		if (units == 0) {
			schedule();
			return;
		}
		if (units > r.getMaxResource() || units < 0) {
			throw new Exception("requested units exceed resource limit");
		}
		if (r.getUnits() >= units) { //free
			r.reduceUnits(units);
			self.addResource(r,units);
		}else{//allocated
			self.setStatus(Process.Status.blocked);
			removeFromReadyList(self);

			self.setWaitingResources(r);
			r.addWaitingProcess(self,units);
		}
		schedule();
	}

	/**
	 * release the resource from the current process 's other_resource list
	 * side effect: releasne the no of units specified by the parameter
	 * if units equal to the no. of units this process is occupying, this resource is deleted from the process resource list
	 * otherwise, the corresponding resource units is updated, but not deleted from the process's resource list 
	 * once the resource is freed , it's gonna check it's waiting list to see if it can serve the next waiting request
	 * @param  name      [description]
	 * @param  units     [description]
	 * @throws Exception [description]
	 */
	private void release(String name,int units) throws Exception{
		Resource r = get_RCB(name);
		if (r == null) {
			throw new Exception("resource not found");
		}
		if (units == 0) {
			schedule();
			return;
		}
		if (units > r.getMaxResource() || units < 0) {
			throw new Exception("released units exceed resource limit");
		}
		self.removeAndUpdateResource(r,units); //update the resource units as well
		if(r.free()){
			ready_queue = r.checkWaitingList(ready_queue);
		}
		schedule();
	}
	/**
	 * set the parameter process as the current running process
	 * @param p [description]
	 */
	private void preempt(Process p){
		if (self.getStatus() == Process.Status.running) {
			self.setStatus(Process.Status.ready);
		}
		p.setStatus(Process.Status.running);
		self = p;
	}

	/**
	 * system scheduling process
	 * side effect: approriate process is chosen to run
	 * if no process is in the ready_queue, OS is gonna panic
	 * @throws Exception [description]
	 */
	private void schedule() throws Exception{
		// System.out.println("before schedule");
		// overview();
		Process p = null;
		for (int i=2; i>=0; i--) {
			if (!ready_queue.get(i).isEmpty()) {
				if((p = ready_queue.get(i).peek()) != null){
					break;
				}
			}
		}
		if (p != null) {
			if(self.getPriority() < p.getPriority() ||
				self.getStatus() != Process.Status.running ||
				self == null){
				preempt(p);
			}
			output.append(self.getName() + " ");
		}else{ //only one process left in the queue
			self = null;
			throw new Exception("panic");
		}
		
		// System.out.println("after schedule");
		// overview();
	}

	/**
	 * current process time slice reduce to 0
	 * process set to ready, and added to the end of it's priority queue
	 * 
	 * @throws Exception [description]
	 */
	private void time_Out() throws Exception{ 
		ready_queue.get(self.getPriority()).remove(self);
		self.setStatus(Process.Status.ready);
		addToReadyList(self);
		schedule();
	}




}