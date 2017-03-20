class Process implements Comparable<Process>{

	public enum Status{
		READY,RUNNING,BLOCKED,NONE;
		public String toString(){
	        switch (this) {
	            case READY: return "Ready";
	            case RUNNING: return "Running";
	            case BLOCKED: return "Blocked";
	            case NONE: return "None";
	        }
	        return null;
	    }
	}
	private Status _status = Status.NONE;
	private int _pid = 0;
	private int _starttime = 0;
	private int _starttime_backup = 0;

	public int QUANTUM = 0;
	public int SLICE = 0;
	private int _quantum_backup = 0;

	private int _endtime = 0;
	private int _priority = 0;
	public Process(int pid, int start, int quantum){
		this._pid = pid;
		this._starttime = start;
		this.QUANTUM = quantum;
		_quantum_backup = quantum;
		_starttime_backup = start;
	}

	public void setRunning(){
		_status = Status.RUNNING;
	}

	public void setReady(){
		_status = Status.READY;
	}
	public void setBlocked(){
		_status = Status.BLOCKED;
	}

	public void setNone(){
		_status = Status.NONE;
	}

	public Status getStatus(){
		return _status;
	}

	public int getPID(){
		return _pid;
	}

	public void reset(){
		QUANTUM = _quantum_backup;
		_starttime = _starttime_backup;
		_endtime = 0;
		_priority = 0;
		SLICE = 0;
		_status = Status.NONE;
	}

	public void setPriority(int priority){
		this._priority = priority;
	}

	public int lowerPriority(){
		if(_priority >0){
			_priority--;
		}else{
			_priority = 0;
		}
		return	_priority; 
	}

	public int getPriority(){
		return _priority;
	}

	public void setEndTime(int end){
		_endtime = end;
	}

	public int getTurnAround(){
		//System.out.println(_endtime + " - " + _starttime);
		int turnaround = _endtime - _starttime;
		return turnaround;
	}

	public int getEndTime(){
		return _endtime;
	}

	public int getStartTime(){
		return _starttime;
	}

	public void setStartTime(int start){
		_starttime = start;
	}

	@Override
	public int compareTo(Process other){
		int result = QUANTUM - other.QUANTUM;
		if(result == 0)	return _starttime - other.getStartTime();
		else	return result;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(" PID: "+getPID()+" Priority "+_priority+" start "+getStartTime() + " " + getStatus().toString() + " slice "+SLICE+" quantum " +QUANTUM);
		return sb.toString();

	}

	// public void setQuantum(int quan){
	// 	this.quantum = quan;
	// }
	// public int getQuantum(){
	// 	return quantum;
	// }

}