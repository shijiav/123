import java.io.*;
import java.net.*;

public class Mainclass {
	
	Mainclass()
	{
		Globalvar.ipaddr=new String[Globalvar.totnodes]; //to store the ipaddresses of all the nodes
		Globalvar.portnum=new int[Globalvar.totnodes]; // to store the port numbers of all the node
		Globalvar.vectorclock=new int[Globalvar.totnodes]; // to store the vector clock of all the node
	}

	public void readconfigfile() throws Exception  //method to read the contents of the config file
		{ 
			String str;
			int j=0;
		
			InetAddress Address = InetAddress.getLocalHost(); 
			Globalvar.hostname=Address.getHostName(); //get the ip address of the machine on which current node is running
		
			FileReader fr = new FileReader("config.txt");  //filereader to read contents of the config file
			BufferedReader br= new BufferedReader(fr);
		
			while ((str=br.readLine())!=null) //read each line
				{ 
					String st[]=str.split("#");  //split the string with # as the delimiter
					if(st.length>1)
						{
							if(st[1].trim().equalsIgnoreCase("minperactive")) 
								Globalvar.minperactive=Integer.parseInt(st[0].trim());//read the minimum messages
							else if(st[1].trim().equalsIgnoreCase("maxperactive"))  
								Globalvar.maxperactive=Integer.parseInt(st[0].trim());//read the maximum messages
							else if(st[1].trim().equalsIgnoreCase("minsenddelay")) 
								Globalvar.minsenddelay=Integer.parseInt(st[0].trim());//read the minimum send delay
							else if(st[1].trim().equalsIgnoreCase("maxnumber")) 
								Globalvar.maxnumber=Integer.parseInt(st[0].trim());// read maximum messages after which process remains passive
							else if(st[1].trim().equalsIgnoreCase("mininstdelay")) 
								Globalvar.mininstdelay=Integer.parseInt(st[0].trim());//read minimum snapshot delay
							else if(st[1].trim().equalsIgnoreCase("number of operations"))
							{
								Globalvar.numop=Integer.parseInt(st[0].trim());
								Globalvar.operations=new String[Globalvar.numop]; // to store the operation sequence
								int i=0;
								for(j=0;j<Globalvar.numop;)
								{
									str=br.readLine();
									if(str!=null && str.startsWith("("))
									{
									  Globalvar.operations[i]=str.substring(1,4);
									  i++;
									  j++;
									}
									
								}
							}
							else if(st[1].trim().startsWith("L") || st[1].trim().startsWith("l")) //read ipaddress and portnumber of remaining nodes
								{
									String temp[]=st[0].split("\\s"); // split by space to get each individual string
									Globalvar.ipaddr[Integer.parseInt(temp[0].trim())] = temp[1].trim(); //reading ipaddress
									Globalvar.portnum[Integer.parseInt(temp[0].trim())] = Integer.parseInt(temp[2].trim()); //reading portnumber
								}
							else if(st[1].trim().startsWith("N") || st[1].trim().startsWith("n")) //read neighbours of current node
								{
									String temp[]=st[0].split("\\s"); //split by space to get each individual string
									if(Integer.parseInt(temp[0])==Globalvar.nodenum) //check for current node
										{
											Globalvar.numneighbors = temp.length-1;	//read the number of neighbors				 
											Globalvar.neighbors= new int[temp.length-1]; //create an array for storing the neighbors information
											for(j=0;j<(temp.length-1);j++)
												Globalvar.neighbors[j]=Integer.parseInt(temp[j+1].trim()); // read neighbours of current node
										}
								}
							
						}
				}
			fr.close();
		}
	
	public void display() throws Exception  //method to display the configuration
		{ 
			int i;
			System.out.println("\nTotal number of nodes: " +Globalvar.totnodes);
			System.out.println("Node " +Globalvar.nodenum);
			System.out.println("IP Address: " +Globalvar.ipaddr[Globalvar.nodenum] +"\nPort Number: " +Globalvar.portnum[Globalvar.nodenum]);
			System.out.print("Neighbors of Node "+Globalvar.nodenum +": ");
			for(i=0;i<Globalvar.numneighbors;i++)
				System.out.print(Globalvar.neighbors[i] +" ");
			System.out.println("\nMinPerActive "+Globalvar.minperactive);
			System.out.println("MaxPerActive "+Globalvar.maxperactive);
			System.out.println("MinSendDelay "+Globalvar.minsenddelay);
			System.out.println("Maximum number of messages "+Globalvar.maxnumber);
			System.out.println("Minimum Instance delay "+Globalvar.mininstdelay);
			System.out.println("Number of operations "+Globalvar.numop);
			System.out.print("Sequence of operations ");
			for(i=0;i<Globalvar.numop;i++)
				System.out.print("("+Globalvar.operations[i]+") ");
			System.out.print("\n");
			System.out.print("Initial state of node: ");
			if(Globalvar.active)
				System.out.println("active");	
			else
				System.out.println("passive");
		}
	
	public static void main(String args[]) throws Exception  //program starting point
		{ 
	    	String str; //temporary string
	    	FileReader fr = new FileReader("config.txt");
	    	BufferedReader br= new BufferedReader(fr);  //creating a buffered reader to read from file
	    	
	    	while ((str=br.readLine())!=null)//read each line
	    		{  
	    			String st[]=str.split("#");   //split with delimiter as #
	    			if(st.length>1)   
	    				{
	    					if(st[1].trim().equalsIgnoreCase("total number of nodes")) 
	    						Globalvar.totnodes=Integer.parseInt(st[0].trim()); //read the total number of nodes
	    				}
	    		}
	    	Mainclass node = new Mainclass(); //create an object of type Mainclass
	    	if(args.length<1) //if the node number is not provided as command line argument, read it from the user
	    		{
	    			System.out.print("Enter the node number:");
	    			BufferedReader br1= new BufferedReader(new InputStreamReader(System.in));//buffered reader to read from System.in
	    			str=br1.readLine();//read the node number in the form of a string
	    			Globalvar.nodenum=Integer.parseInt(str); // convert the node number to integer form
	    		}
	    	else
	    			Globalvar.nodenum=Integer.parseInt(args[0]); //if node number is provided as command line argument then assign it to the current processs' nodenum
	    		
		
	    	if(Globalvar.nodenum==0)
	    			Globalvar.active=true;//to ensure at least one node is active
	    	node.readconfigfile(); //read the config file provided
	    	
	    	Globalvar.llr=new Integer[Globalvar.numneighbors];
			Globalvar.fls=new int[Globalvar.numneighbors];
			Globalvar.templls=new int[Globalvar.numneighbors];
			Globalvar.lls=new int[Globalvar.numneighbors];
			Globalvar.messages= new int[Globalvar.numneighbors];
			Globalvar.lost= new int[Globalvar.numneighbors];
			Globalvar.messageslost= new int[Globalvar.numneighbors];
			Globalvar.recievelost= new int[Globalvar.numneighbors];
			Globalvar.cohort=new int[Globalvar.totnodes];
			Globalvar.cohortold=new int[Globalvar.totnodes];
			Globalvar.requests=new int[Globalvar.numneighbors+1];
			
			node.display(); // display the contents of the configuration provided
	    	new Serversetup(); //class object for setting up the server sockets
	    	Thread.sleep(10000); //let thread sleep so that remaining sockets can be created
	    	new Sndthread(); //class object for initiating the sending of messages
	    }
	}