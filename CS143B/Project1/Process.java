import java.io.*;
import java.util.*;

class Process{

	public enum Status{
		ready,running,blocked,none
	}


	private String _name = "";
	//list of occupied process, _requested_units indicate the corresponding requested units
	private LinkedList<Resource> _other_Resources = null;
	private HashMap<Resource,Integer> _requested_units = null; //for other resource

	private Status _status;
	public Creation_Tree CREATION_TREE = null;
	private int _priority = 0;

	//_waiting_resource could only be NOT null if status is blocked
	private Resource _waiting_resource = null;

	public Process(String name,int priority, Process parent){
		this._name = name;
		this._priority = priority;
		_other_Resources = new LinkedList<Resource>();
		CREATION_TREE = new Creation_Tree(parent);
		_requested_units = new HashMap<Resource,Integer>();
		_status = Status.ready;
	}

	@Override
	public String toString(){
		String s = "";
		s += "Process "+ _name + " priority: "+ _priority + " status: "+_status.toString();
		if (CREATION_TREE.PARENT != null) {
			s+=  " | parent "+CREATION_TREE.PARENT.getName() +" ";
			if (CREATION_TREE.CHILD_TREE.isEmpty()) {
				s+= " | no children ";
			}else{
				s+= " | Children: ";
				Iterator it = CREATION_TREE.CHILD_TREE.iterator();
				while(it.hasNext()){
					Process p = (Process)it.next();
					s += p.getName() + " ";
				}
			}
			
		}
		if (_waiting_resource != null) {
			s += " waiting resource: "+_waiting_resource.getRID();
		}
		s += "\n other resources: ";
		Iterator it = _other_Resources.iterator();
		while(it.hasNext()){
			Resource r = (Resource) it.next();
			s += " | resource " + r.getRID() + " units occu: "+_requested_units.get(r);
		}
		s+="\n";
		return s;
	}

	public String getName(){
		return _name;
	}

	public int getPriority(){
		return _priority;
	}

	public Status getStatus(){
		return _status;
	}

	public void setStatus(Status val){
		_status = val;
	}

	public void addChild(Process p){
		CREATION_TREE.CHILD_TREE.addLast(p);
	}

	public void addResource(Resource r, int units){
		_other_Resources.addLast(r);
		_requested_units.put(r,units);
	}
	/**
	 * release units specified of specified resource
	 * units of this resource is increased by the amount. 
	 * if the units equal to the amount it owns, this resource is removed from the resource list of this process
	 * @param  r                    [description]
	 * @param  units_to_be_released [description]
	 * @throws Exception            [description]
	 */
	public void removeAndUpdateResource(Resource r,int units_to_be_released) throws Exception{
		// System.out.println(toString());
		// System.out.println(r);
		if (!_other_Resources.contains(r)) {
			throw new Exception("resource is not owned by this process");
		}

		int units = _requested_units.get(r);
		if (units_to_be_released > units){
			throw new Exception("can't release units more than this process currently occupying");
		}
		//if units_to_be_released <= units
		r.increaseUnits(units_to_be_released);
		//if the units equal to the amount it owns
		if (units == units_to_be_released) {
			_other_Resources.remove(r);
			_requested_units.remove(r);

		}else{
			_requested_units.put(r,r.getUnits());
		}
		
	}
	/**
	 * release all the resources owned by this process
	 * side effect: if the released resource is free, it's gonna check if it can server the next waiting request, if there is one
	 * @param  scheduling_queue [description]
	 * @return                  [description]
	 * @throws Exception        [description]
	 */
	public ArrayList<LinkedList<Process>> releaseAllResources(ArrayList<LinkedList<Process>> scheduling_queue)  throws Exception{
		if (_waiting_resource != null) { //the resource this process current waiting for
			_waiting_resource.removeWaitingProcess(this);
			//check other processes after this one
		}
		for(Resource r:_other_Resources){ //resources this process currentlyl occupying
			int requested = _requested_units.get(r);
			r.increaseUnits(requested);
			scheduling_queue = r.checkWaitingList(scheduling_queue);
		}
		return scheduling_queue;
	}

	/**
	 * set the waiting resource
	 * @param  r         [description]
	 * @throws Exception [description]
	 */
	public void setWaitingResources(Resource r) throws Exception{
		if (_status == Status.blocked) {
			_waiting_resource = r;
		}else{
			throw new Exception("can't add waiting resource when the process is not blocked");
		}
	}


	public void clearVal(){
		_name = null;
		_other_Resources = null;
		_requested_units = null;
		CREATION_TREE.PARENT = null;
		CREATION_TREE.CHILD_TREE = null;
		CREATION_TREE = null;
		_waiting_resource = null;
	}


}