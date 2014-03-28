import java.util.Random;

public class Globalvar {
	
	static Random randgen = new Random(System.currentTimeMillis()); //for the purpose of random generation
	static int totnodes; //total number of nodes present
	static int minperactive; //minimum number of messages 
	static int maxperactive; //maximum number of messages 
	static int minsenddelay; //minimum delay between each message 
	static int maxnumber; // maximum messages allowed by a process
	static boolean active=randgen.nextBoolean(); //to indicate if current node is active or not
	static int neighbors[]; //neighbors of current node
	static String ipaddr[]; //string array containing ip addresses of all nodes
	static String hostname; //ip address of machine on which current node is running
	static int portnum[]; //array containing portnumbers of all nodes
	static int nodenum; //current nodes number
	static int numneighbors; //total number of neighbors
	static boolean passive=false; //to indicate if the current node has become passive permanently
	static int messagecount=0; //count of messages sent by current process over all channels
	static int numop;
	static String operations[];
	static int mininstdelay;  //minimum delay between each snapshot
	static int vectorclock[]; 
	
	static boolean terminated=false; //to indicate the current node wont send/recieve any more messages
	static boolean cansend=true;
	static String ckpt[]=new String[20]; //to store the check point info, creates a string array, capable of storing as many as 30 checkpoints
	static int currentop=0;
	static Integer llr[];
	static int fls[];
	static int templls[];
	static int lls[];
	static int cohort[];
	static int cohortold[];
	static int seqNum=0;
	static int messages[];
	static int tentckpt=0;
	static int requestSent=0;
	static int repliesRcvd=0;
	static int requestRcvd=0;
	static int decisionRcvd=0;
	static int requests[];
	static int decisionCount=0;
	static int lost[];
	static int messageslost[];
	static int initiate=0;
	static int need_to_reply=0;
	static int recievelost[];
	
	static int state[]= new int[2];
	static int rollbackRcvd=0;
	static int tentroll=0;
    
}
