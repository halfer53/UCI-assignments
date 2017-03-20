import java.io.*;
import java.util.*;

class Resource{
	enum Status{
		free,allocated,none
	}

	private int _rid = 0;
	private int _units = 0;
	private Status _status = Status.none;

	//list of waiting process, _requested_units_list indicate the corresponding requested units
	private LinkedList<Process> _waiting_list = null;
	private HashMap<Process,Integer> _requested_units_list = null;

	public Resource(int rid){
		_rid = _units = rid;
		_waiting_list = new LinkedList<Process>();
		_requested_units_list = new HashMap<Process,Integer>();
		_status = Status.free;
	}

	@Override
	public String toString(){
		String s = "";
		s += "resource " +_rid + " units: "+ _units + " waiting pro: ";
		Iterator it = _waiting_list.iterator();
		while(it.hasNext()){
			Process p = (Process) it.next();
			s += "| process " + p.getName() + " units req: "+_requested_units_list.get(p)+ ",";
		}
		s+="\n";
		return s;
	}


	public int getRID(){
		return _rid;
	}

	public int getMaxResource(){
		return _rid;
	}

	public int getUnits(){
		return _units;
	}
	/**
	 * reduce the units available by the amount specified by the parameter
	 * 
	 * @param  val       [description]
	 * @throws Exception [description]
	 */
	public void reduceUnits(int val) throws Exception{
		if (_units - val >= 0) {
			_units -= val;
		}else{
			throw new Exception("resource is not available with the requested units");
		}
	}

	/**
	 * increase the units available by the amount specified by the parameter
	 * 
	 * @param  val       [description]
	 * @throws Exception [description]
	 */
	public void increaseUnits(int val) throws Exception{
		// for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
		//     System.out.println(ste);
		// }
		// System.out.println(toString());
		if (_units + val <= _rid) { //rid is the limit of the no. of units for this resource
			_units += val;
		}else{
			throw new Exception("units can't exceed limit");
		}
		//System.out.println(toString());
	}

	public void resetUnits(){
		_units = _rid;
	}

	public void addWaitingProcess(Process p,int units){
		_waiting_list.addLast(p);
		_requested_units_list.put(p,units);
	}

	public int getRequestedUnits(Process p){
		return _requested_units_list.get(p);
	}

	public void removeWaitingProcess(Process p) throws Exception{

		_waiting_list.remove(p);
		_requested_units_list.remove(p);
	}

	public boolean free(){
		if (_units > 0) {
			return true;
		}
		return false;
	}
	/**
	 * check all the waiting process of this resource
	 * decrement the units by the requested amount if there is a waiting request and requested units >= available units
	 * process is then added to the ready_queue
	 * 
	 * @param  scheduling_queue [description]
	 * @return                  [description]
	 * @throws Exception        [description]
	 */
	public ArrayList<LinkedList<Process>> checkWaitingList(ArrayList<LinkedList<Process>> scheduling_queue) throws Exception{
		
		//serve the first one, if it can't then fail
		//or recursively loop, and select the one that can fit
		if(free()){
			Iterator it = _waiting_list.iterator();
			while(it.hasNext()){
				Process pp = (Process)it.next();
				int requested_unit = _requested_units_list.get(pp);

				if ( requested_unit <= _units) {
					reduceUnits(requested_unit);
					pp.addResource(this,requested_unit);

					_waiting_list.remove(pp);
					pp.setWaitingResources(null);
					pp.setStatus(Process.Status.ready);

					scheduling_queue.get(pp.getPriority()).addLast(pp);
					break;
				}
			}
		}
		return scheduling_queue;
	}
}