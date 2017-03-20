"""Search (Chapters 3-4)

The way to use this code is to subclass Problem to create a class of problems,
then create problem instances and solve them with calls to the various search
functions."""

from utils import (
    is_in, argmin, argmax, argmax_random_tie, probability,
    weighted_sample_with_replacement, memoize, print_table, DataFile, Stack,
    FIFOQueue, PriorityQueue, name
)
from grid import distance

from collections import defaultdict,deque
import math
import random
import sys
import bisect
from datetime import datetime
from time import time
import networkx as nx

infinity = float('inf')

# ______________________________________________________________________________


class Problem(object):

    """The abstract class for a formal problem.  You should subclass
    this and implement the methods actions and result, and possibly
    __init__, goal_test, and path_cost. Then you will create instances
    of your subclass and solve them with the various search functions."""

    def __init__(self, initial, goal=None):
        """The constructor specifies the initial state, and possibly a goal
        state, if there is a unique goal.  Your subclass's constructor can add
        other arguments."""
        self.initial = initial
        self.goal = goal
        self.state_count = 0

    def actions(self, state):
        """Return the actions that can be executed in the given
        state. The result would typically be a list, but if there are
        many actions, consider yielding them one at a time in an
        iterator, rather than building them all at once."""
        raise NotImplementedError

    def result(self, state, action):
        """Return the state that results from executing the given
        action in the given state. The action must be one of
        self.actions(state)."""
        raise NotImplementedError

    def goal_test(self, state):
        """Return True if the state is a goal. The default method compares the
        state to self.goal or checks for state in self.goal if it is a
        list, as specified in the constructor. Override this method if
        checking against a single self.goal is not enough."""
        if isinstance(self.goal, list):
            return is_in(state, self.goal)
        else:
            return state == self.goal

    def path_cost(self, c, state1, action, state2):
        """Return the cost of a solution path that arrives at state2 from
        state1 via action, assuming cost c to get up to state1. If the problem
        is such that the path doesn't matter, this function will only look at
        state2.  If the path does matter, it will consider c and maybe state1
        and action. The default method costs 1 for every step in the path."""
        return c + 1

    def value(self, state):
        """For optimization problems, each state has a value.  Hill-climbing
        and related algorithms try to maximize this value."""
        raise NotImplementedError
# ______________________________________________________________________________


class Node:

    """A node in a search tree. Contains a pointer to the parent (the node
    that this is a successor of) and to the actual state for this node. Note
    that if a state is arrived at by two paths, then there are two nodes with
    the same state.  Also includes the action that got us to this state, and
    the total path_cost (also known as g) to reach the node.  Other functions
    may add an f and h value; see best_first_graph_search and astar_search for
    an explanation of how the f and h values are handled. You will not need to
    subclass this class."""

    def __init__(self, state, parent=None, action=None, path_cost=0):
        "Create a search tree Node, derived from a parent by an action."
        self.state = state
        self.parent = parent
        self.action = action
        self.path_cost = path_cost
        self.depth = 0
        if parent:
            self.depth = parent.depth + 1

    def __repr__(self):
        return "<Node %s>" % (self.state,)

    def __lt__(self, node):
        return self.state < node.state

    def expand(self, problem):
        "List the nodes reachable in one step from this node."
        return [self.child_node(problem, action)
                for action in problem.actions(self.state)]

    def child_node(self, problem, action):
        "[Figure 3.10]"
        next = problem.result(self.state, action)
        return Node(next, self, action,
                    problem.path_cost(self.path_cost, self.state,
                                      action, next))

    def solution(self):
        "Return the sequence of actions to go from the root to this node."
        return [node.action for node in self.path()[1:]]

    def path(self):
        "Return a list of nodes forming the path from the root to this node."
        node, path_back = self, []
        while node:
            path_back.append(node)
            node = node.parent
        return list(reversed(path_back))

    # We want for a queue of nodes in breadth_first_search or
    # astar_search to have no duplicated states, so we treat nodes
    # with the same state as equal. [Problem: this may not be what you
    # want in other contexts.]

    def __eq__(self, other):
        return isinstance(other, Node) and self.state == other.state

    def __hash__(self):
        return hash(self.state)

# ______________________________________________________________________________


class SimpleProblemSolvingAgentProgram:

    """Abstract framework for a problem-solving agent. [Figure 3.1]"""

    def __init__(self, initial_state=None):
        self.state = initial_state
        self.seq = []

    def __call__(self, percept):
        self.state = self.update_state(self.state, percept)
        if not self.seq:
            goal = self.formulate_goal(self.state)
            problem = self.formulate_problem(self.state, goal)
            self.seq = self.search(problem)
            if not self.seq:
                return None
        return self.seq.pop(0)

    def update_state(self, percept):
        raise NotImplementedError

    def formulate_goal(self, state):
        raise NotImplementedError

    def formulate_problem(self, state, goal):
        raise NotImplementedError

    def search(self, problem):
        raise NotImplementedError

# ______________________________________________________________________________
# Uninformed Search algorithms


def tree_search(problem, frontier):
    """Search through the successors of a problem to find a goal.
    The argument frontier should be an empty queue.
    Don't worry about repeated paths to a state. [Figure 3.7]"""
    frontier.append(Node(problem.initial))
    while frontier:
        node = frontier.pop()
        if problem.goal_test(node.state):
            return node
        frontier.extend(node.expand(problem))
    return None


def graph_search(problem, frontier):
    """Search through the successors of a problem to find a goal.
    The argument frontier should be an empty queue.
    If two paths reach a state, only use the first one. [Figure 3.7]"""
    frontier.append(Node(problem.initial))
    explored = set()
    while frontier:
        node = frontier.pop()
        if problem.goal_test(node.state):
            return node
        explored.add(node.state)
        frontier.extend(child for child in node.expand(problem)
                        if child.state not in explored and
                        child not in frontier)
    return None


def breadth_first_tree_search(problem):
    "Search the shallowest nodes in the search tree first."
    return tree_search(problem, FIFOQueue())


def depth_first_tree_search(problem):
    "Search the deepest nodes in the search tree first."
    return tree_search(problem, Stack())


def depth_first_graph_search(problem):
    "Search the deepest nodes in the search tree first."
    return graph_search(problem, Stack())


def breadth_first_search(problem):
    "[Figure 3.11]"
    node = Node(problem.initial)
    if problem.goal_test(node.state):
        return node
    frontier = FIFOQueue()
    frontier.append(node)
    explored = set()
    while frontier:
        node = frontier.pop()
        explored.add(node.state)
        for child in node.expand(problem):
            if child.state not in explored and child not in frontier:
                if problem.goal_test(child.state):
                    return child
                frontier.append(child)
    return None


def best_first_graph_search(problem, f):
    """Search the nodes with the lowest f scores first.
    You specify the function f(node) that you want to minimize; for example,
    if f is a heuristic estimate to the goal, then we have greedy best
    first search; if f is node.depth then we have breadth-first search.
    There is a subtlety: the line "f = memoize(f, 'f')" means that the f
    values will be cached on the nodes as they are computed. So after doing
    a best first search you can examine the f values of the path returned."""
    f = memoize(f, 'f')
    node = Node(problem.initial)
    if problem.goal_test(node.state):
        return node
    frontier = PriorityQueue(min, f)
    frontier.append(node)
    explored = set()
    while frontier:
        node = frontier.pop()
        if problem.goal_test(node.state):
            return node
        #problem.print_state(node.state)
        explored.add(node.state)
        for child in node.expand(problem):
            if child.state not in explored and child not in frontier:
                frontier.append(child)
            elif child in frontier:
                incumbent = frontier[child]
                if f(child) < f(incumbent):
                    del frontier[incumbent]
                    frontier.append(child)
    return None


def uniform_cost_search(problem):
    "[Figure 3.14]"
    return best_first_graph_search(problem, lambda node: node.path_cost)


def depth_limited_search(problem, limit=50):
    "[Figure 3.17]"
    def recursive_dls(node, problem, limit):
        if problem.goal_test(node.state):
            return node
        elif limit == 0:
            return 'cutoff'
        else:
            cutoff_occurred = False
            for child in node.expand(problem):
                result = recursive_dls(child, problem, limit - 1)
                if result == 'cutoff':
                    cutoff_occurred = True
                elif result is not None:
                    return result
            return 'cutoff' if cutoff_occurred else None

    # Body of depth_limited_search:
    return recursive_dls(Node(problem.initial), problem, limit)


def iterative_deepening_search(problem):
    "[Figure 3.18]"
    for depth in range(sys.maxsize):
        result = depth_limited_search(problem, depth)
        if result != 'cutoff':
            return result

# ______________________________________________________________________________
# Informed (Heuristic) Search

greedy_best_first_graph_search = best_first_graph_search
# Greedy best-first search is accomplished by specifying f(n) = h(n).


def astar_search(problem, h=None):
    """A* search is best-first graph search with f(n) = g(n)+h(n).
    You need to specify the h function when you call astar_search, or
    else in your Problem subclass."""
    h = memoize(h or problem.h, 'h')
    return best_first_graph_search(problem, lambda n: n.path_cost + h(n))

# ______________________________________________________________________________
# Other search algorithms


def recursive_best_first_search(problem, h=None):
    "[Figure 3.26]"
    h = memoize(h or problem.h, 'h')
    count = 0

    def RBFS(problem, node, flimit,count):
        if problem.goal_test(node.state):
            return node, 0   # (The second value is immaterial)
        successors = node.expand(problem)

        count +=1
        if len(successors) == 0:
            return None, infinity
        for s in successors:
            s.f = max(s.path_cost + h(s), node.f)
        while True:
            # Order by lowest f value
            successors.sort(key=lambda x: x.f)
            best = successors[0]

            if best.f >= flimit:
                return None, best.f
            if len(successors) > 1:
                alternative = successors[1].f
            else:
                alternative = infinity
            result, best.f = RBFS(problem, best, min(flimit, alternative),count)
            if result is not None:
                return result, best.f

    node = Node(problem.initial)
    node.f = h(node)
    result, bestf = RBFS(problem, node, infinity,count)
    return result


def hill_climbing(problem):
    """From the initial node, keep choosing the neighbor with highest value,
    stopping when no neighbor is better. [Figure 4.2]"""
    current = Node(problem.initial)
    while True:
        neighbors = current.expand(problem)
        if not neighbors:
            break
        neighbor = argmax_random_tie(neighbors,
                                     key=lambda node: problem.value(node.state))
        if problem.value(neighbor.state) <= problem.value(current.state):
            break
        current = neighbor
    return current.state


def exp_schedule(k=20, lam=0.005, limit=100):
    "One possible schedule function for simulated annealing"
    return lambda t: (k * math.exp(-lam * t) if t < limit else 0)


def simulated_annealing(problem, schedule=exp_schedule()):
    "[Figure 4.5]"
    current = Node(problem.initial)
    for t in range(sys.maxsize):
        T = schedule(t)
        if T == 0:
            return current
        neighbors = current.expand(problem)
        if not neighbors:
            return current
        next = random.choice(neighbors)
        delta_e = problem.value(next.state) - problem.value(current.state)
        if delta_e > 0 or probability(math.exp(delta_e / T)):
            current = next


class Graph:

    """A graph connects nodes (verticies) by edges (links).  Each edge can also
    have a length associated with it.  The constructor call is something like:
        g = Graph({'A': {'B': 1, 'C': 2})
    this makes a graph with 3 nodes, A, B, and C, with an edge of length 1 from
    A to B,  and an edge of length 2 from A to C.  You can also do:
        g = Graph({'A': {'B': 1, 'C': 2}, directed=False)
    This makes an undirected graph, so inverse links are also added. The graph
    stays undirected; if you add more links with g.connect('B', 'C', 3), then
    inverse link is also added.  You can use g.nodes() to get a list of nodes,
    g.get('A') to get a dict of links out of A, and g.get('A', 'B') to get the
    length of the link from A to B.  'Lengths' can actually be any object at
    all, and nodes can be any hashable object."""

    def __init__(self, dict=None, directed=True):
        self.dict = dict or {}
        self.directed = directed
        if not directed:
            self.make_undirected()

    def make_undirected(self):
        "Make a digraph into an undirected graph by adding symmetric edges."
        for a in list(self.dict.keys()):
            for (b, distance) in self.dict[a].items():
                self.connect1(b, a, distance)

    def connect(self, A, B, distance=1):
        """Add a link from A and B of given distance, and also add the inverse
        link if the graph is undirected."""
        self.connect1(A, B, distance)
        if not self.directed:
            self.connect1(B, A, distance)

    def connect1(self, A, B, distance):
        "Add a link from A to B of given distance, in one direction only."
        self.dict.setdefault(A, {})[B] = distance

    def get(self, a, b=None):
        """Return a link distance or a dict of {node: distance} entries.
        .get(a,b) returns the distance or None;
        .get(a) returns a dict of {node: distance} entries, possibly {}."""
        links = self.dict.setdefault(a, {})
        if b is None:
            return links
        else:
            return links.get(b)

    def nodes(self):
        "Return a list of nodes in the graph."
        return list(self.dict.keys())


def UndirectedGraph(dict=None):
    "Build a Graph where every edge (including future ones) goes both ways."
    return Graph(dict=dict, directed=False)

def RandomGraph(nodes=list(range(10)), min_links=2, width=400, height=300,
                curvature=lambda: random.uniform(1.1, 1.5)):
    """Construct a random graph, with the specified nodes, and random links.
    The nodes are laid out randomly on a (width x height) rectangle.
    Then each node is connected to the min_links nearest neighbors.
    Because inverse links are added, some nodes will have more connections.
    The distance between nodes is the hypotenuse times curvature(),
    where curvature() defaults to a random number between 1.1 and 1.5."""
    g = UndirectedGraph()
    g.locations = {}
    # Build the cities
    for node in nodes:
        g.locations[node] = (random.randrange(width), random.randrange(height))
    # Build roads from each city to at least min_links nearest neighbors.
    for i in range(min_links):
        for node in nodes:
            if len(g.get(node)) < min_links:
                here = g.locations[node]

                def distance_to_node(n):
                    if n is node or g.get(node, n):
                        return infinity
                    return distance(g.locations[n], here)
                neighbor = argmin(nodes, key=distance_to_node)
                d = distance(g.locations[neighbor], here) * curvature()
                g.connect(node, neighbor, int(d))
    return g



class GraphProblem(Problem):

    "The problem of searching a graph from one node to another."

    def __init__(self, initial, goal, graph):
        Problem.__init__(self, initial, goal)
        self.graph = graph

    def actions(self, A):
        "The actions at a graph node are just its neighbors."
        return list(self.graph.get(A).keys())

    def result(self, state, action):
        "The result of going to a neighbor is just that neighbor."
        return action

    def path_cost(self, cost_so_far, A, action, B):
        return cost_so_far + (self.graph.get(A, B) or infinity)

    def h(self, node):
        "h function is straight-line distance from a node's state to goal."
        locs = getattr(self.graph, 'locations', None)
        if locs:
            return int(distance(locs[node.state], locs[self.goal]))
        else:
            return infinity



eight_puzzle_initial_state = (5,4,1,7,0,2,8,3,6)
eight_puzzle_goal_state = (0,1,2,3,4,5,6,7,8)

class eight_puzzle(Problem):
    def actions(self, state):
        action_dict = {0:["Left","Up"],1:["Up","Left","Right"],2:["Right","Up"],
                3:["Up","Down","Left"],4:["Up","Down","Left","Right"],5:["Right","Up","Down"],
                6:["Left","Down"],7:["Left","Right","Down"],8:["Right","Down"]}
        index = state.index(0)
        return action_dict[index]

    def result(self, state, action):
        #self.print_state(state)
        result = list(state)
        index = result.index(0)
        other_index = 0;
        if action == "Up":
            other_index = index + 3
        elif action == "Down":
            other_index = index - 3
        elif action == "Left":
            other_index = index +1
        elif action == "Right":
            other_index = index -1
        if other_index > 8 or other_index < 0:
            raise NameError("invalid action")

        result[index] = result[other_index]
        result[other_index] = 0
        # print(action,h2(result))
        # self.print_state(result)
        self.state_count += 1
        sset = (result[0],result[1],result[2],result[3],result[4],result[5],result[6],result[7],result[8])
        return sset

    def __toset(self,list):
        s = set()
        for val in result:
            yield val
            s.add(val)


    def goal_test(self,state):
        if state == self.goal:
            return True
        return False

    def print_state(self,state):
        llist = list(state)
        for i in range(3):
            print(llist[i*3:(i+1)*3])
        print()


def h(node):
    vector_dict2 = {0:(0,0),1:(0,1),2:(0,2),
                3:(1,0),4:(1,1),5:(1,2),
                6:(2,0),7:(2,1),8:(2,2)}
    distance = 0
    for ind,el in enumerate(node.state):
        curr_co = vector_dict2[ind]
        dest_co = vector_dict2[el]
        distance += abs(curr_co[0] - dest_co[0])+abs(curr_co[0] - dest_co[0])
    return distance

def h2(state):
    vector_dict2 = {0:(0,0),1:(0,1),2:(0,2),
                3:(1,0),4:(1,1),5:(1,2),
                6:(2,0),7:(2,1),8:(2,2)}
    distance = 0
    for ind,el in enumerate(state):
        curr_co = vector_dict2[ind]
        dest_co = vector_dict2[el]
        distance += abs(curr_co[0] - dest_co[0])+abs(curr_co[0] - dest_co[0])
    return distance

class ReadOnlyList(list):
    def __init__(self, other):
        self._list = other

    def __getitem__(self, index):
        return self._list[index]

    def __iter__(self):
        return iter(self._list)

    def __slice__(self, *args, **kw):
        return self._list.__slice__(*args, **kw)

    def __repr__(self):
        return repr(self._list)

    def __len__(self):
        return len(self._list)

    def __hash__(self):
        data = str(self._list)
        return hash(data)

    def NotImplemented(self, *args, **kw):
        raise ValueError("Read Only list proxy")

    append = pop = __setitem__ = __setslice__ = __delitem__ = NotImplemented


def RandomTSP_Map(num=10,threshhold=0.2):
    nodes = list(range(num))
    g = UndirectedGraph()

    random.seed()
    for i in nodes:
        for j in range(i+1,num):
            if(random.random() < threshhold):
                r = random.randint(1,10)
                g.connect(i,j,r)
                print(i,j,r)
    return g

class TSP_problem(GraphProblem):
    #state: list of visited cities
    def __init__(self, initial, goal, graph,num):
        GraphProblem.__init__(self, initial, goal,graph)
        self.num = num
        self.edges = self.__get_edges()
        self.edges = sorted(self.edges, key=lambda item : item[0])
        self.state_count = 0

    def goal_test(self, state):
        # print("curr ",state._list)
        #every node has been visited exactly once
        if state._list[-1] != state._list[0] != self.initial[0]:
            return False
        if len(state._list) != self.num+1:
            return False
        return True

    def actions(self, state):
        ll = list(self.graph.get(state._list[-1]).keys())
        acs = []
        for i in ll:
            if i not in state._list:
                acs.append(i)
        if not acs and len(state._list) == self.num:
            for i in ll:
                if i == self.initial[0]:
                    acs.append(i)
        self.state_count+=len(ll)
        # print("actions ",acs)
        return acs

    def result(self,state,action):
        re = list(state._list)
        re.append(action)

        return ReadOnlyList(re)

    def find(self, parent, i):
        if parent[i] == i:
            return i
        return self.find(parent, parent[i])
 
    def union(self, parent, rank, x, y):
        xroot = self.find(parent, x)
        yroot = self.find(parent, y)
        
        if rank[xroot] < rank[yroot]:
            parent[xroot] = yroot
        elif rank[xroot] > rank[yroot]:
            parent[yroot] = xroot
        else :
            parent[yroot] = xroot
            rank[xroot] += 1

    def kruskal(self,state):

        result =[] #This will store the resultant MST
        cost = 0
 
        i = 0 # An index variable, used for sorted edges
        e = 0 # An index variable, used for result[]
 
        u_edges = self.__get_unvisited_edges(state)
        u_edges = sorted(u_edges, key=lambda item : item[0])
        unvisited = self.__get_unvisited_cities(state)
        #print self.graph
 
        parent = [] ; rank = []

        for n in range(self.num):
            parent.append(n)
            rank.append(0)
     
        # len -1, +1 is the starting node
        while e < len(unvisited) :
            if i>= len(u_edges):
                return 0 #no mst found

            w,u,v =  u_edges[i]
            i +=1
            x = self.find(parent, u)
            y = self.find(parent ,v)
 
            if x != y:
                e +=1
                result.append([w,u,v])
                cost+=w
                self.union(parent, rank, x, y) 
        return cost         
        
    #state : sequence of direction vertices [1,4,6...]
    def __get_edges(self):
        ss = list()
        for i in list(self.graph.dict.keys()):
            for j,distance in self.graph.dict[i].items():
                    ss.append((distance,i,j))
        return ss

    def __get_unvisited_edges(self,state):
        ss = list()
        visited = state._list #exclusive
        for i in list(self.graph.dict.keys()):
            for j,distance in self.graph.dict[i].items():
                if i not in visited and j not in visited:
                    
                    ss.append((distance,i,j))
                elif i == state._list[-1] and j not in visited:
                    
                    ss.append((distance,i,j))
        return ss

    def __get_unvisited_cities(self,state):
        ss = list()
        visited = state._list #exclusive
        for i in list(self.graph.dict.keys()):
            if i not in visited:
                ss.append(i)
        return ss

    def nearest_unvisited_city_distance(self,node):
        point = node.state._list[-1]
        for i in self.edges:
            if i[1] == point:
                #assume this edge is not connected yet
                if i[2] not in node.state._list:
                    return i[0]
            elif i[2] == point:
                #assume this edge is not connected yet
                if i[1] not in node.state._list:
                    return i[0]
        return 0

    #start, end int value representing the verticles
    def dijkstra(self,start,dest):
        visited = {start:0}
        path = {}

        unvisited = list(self.graph.dict.keys())

        while unvisited: 
            min_node = None
            for node in unvisited:
              if node in visited:
                if min_node is None:
                  min_node = node
                elif visited[node] < visited[min_node]:
                  min_node = node

            if min_node is None:
              break

            unvisited.remove(min_node)
            if(min_node == dest):
                break
            current_weight = visited[min_node]

            for b,distance in self.graph.dict[min_node].items():
                weight = current_weight + distance
                if b not in visited or weight < visited[b]:
                    visited[b] = weight
                    path[b] = min_node

        return visited, path

    def shortest_path(self,start, destination):
        visited, paths = self.dijkstra(start, destination)
        if not paths:
            return {},{}
        full_path = deque()
        _destination = paths[destination]

        while _destination != start:
            full_path.appendleft(_destination)
            _destination = paths[_destination]

        full_path.appendleft(start)
        full_path.append(destination)

        return visited[destination], list(full_path)

    def shortest_path_cost(self,start,destination):
        cost,path = self.shortest_path(start,destination)
        if not cost:
            return 0
        else:
            return cost

    def h(self,node):
        nearest_unvisited_city = self.nearest_unvisited_city_distance(node)

        shortest_path = self.shortest_path_cost(node.state._list[-1],self.initial[0])
        estimated_MST_cost_of_unvisited_cities = self.kruskal(node.state)
        # print("h",nearest_unvisited_city + estimated_MST_cost_of_unvisited_cities + shortest_path)
        return nearest_unvisited_city + estimated_MST_cost_of_unvisited_cities + shortest_path

def evaluate_eight_puzzle():

    e = eight_puzzle(eight_puzzle_initial_state,eight_puzzle_goal_state)
    e.print_state(eight_puzzle_initial_state)
    t0 = time()
    r1 = astar_search(e,h)
    t1 = time()
    e.print_state(r1.state)
    print("astar search for eight puzzle, time used",t1-t0,"space used",e.state_count)
    print("actions applied",r1.path())
    print("\n")

    e.state_count = 0
    e.initial = eight_puzzle_initial_state
    e.goal = eight_puzzle_goal_state
    t2 = time()
    r2 = recursive_best_first_search(e,h)
    t3 = time()
    
    print("recursive best first search for eight puzzle, time used",t3-t2,"space used",e.state_count)
    e.print_state(r2.state)
    print("actions applied",r2.path())

evaluate_eight_puzzle()

def evaluate_TSP(num,flimit):
    ran = RandomTSP_Map(num,flimit)

    e = TSP_problem(ReadOnlyList([0]),None,ran,num)
    t0 = time()
    r1 = None
    for i in range(num):
        e.initial = ReadOnlyList([i])
        r1 = astar_search(e,e.h)
        if r1:
            print(r1)
            break
    t1 = time()

    print("astar search for TSP, time used",t1-t0,"space used",e.state_count)
    if r1:
        print("actions applied",r1.path())
    else:
        print("no solutions found")
    print("\n")



    e.state_count = 0
    t2 = time()
    for i in range(num):
        e.initial = ReadOnlyList([i])
        r2 = recursive_best_first_search(e,e.h)
        print(r2)
        if r2:
            print(r2)
            break
    t3 = time()
    print(r2)
    print("recursive best first search for TSP, time used",t3-t2,"space used",e.state_count)
    if r2:
        print("actions applied",r2.path())
    else:
        print("no solutions found")





# nn = 5
# G = nx.Graph()
# ran = RandomTSP_Map(nn,0.5)
# for i in ran.dict.keys():
#         G.add_node(i)
#         for j,distance in ran.dict[i].items():
#             G.add_edge(i,j,weight=distance)
#             #print(i,j,distance)
# import matplotlib.pyplot as plt
# pos = nx.spring_layout(G)
# T=nx.minimum_spanning_tree(G)
# print("mst",sorted(T.edges(data=True)))
# path = nx.shortest_path(G,source=4,target=0)
# print(path)
# nx.draw(G)
# plt.savefig("path.png")
# from networkx.drawing.nx_pydot import write_dot
# nx.draw_graphviz(G)
# write_dot(G,'file.dot')


# e = TSP_problem(random.randint(1,nn),None,ran,nn)
# # r = e.kruskal([0,1])
# # print("my result",sorted(r))
# # cost = e.shortest_path_cost(4,0)
# # print(cost)
