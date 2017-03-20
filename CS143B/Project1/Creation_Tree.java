import java.util.LinkedList;
class Creation_Tree{
	public Process PARENT = null;
	public LinkedList<Process> CHILD_TREE = null;

	public Creation_Tree(Process p){
		PARENT = p;
		CHILD_TREE = new LinkedList<Process>();
	}


}