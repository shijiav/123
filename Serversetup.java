import java.net.*;
import java.io.*;
import java.net.Socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;


public class Serversetup implements Runnable
	
	{
	   
	   SctpServerChannel s=null;
	   SctpChannel client=null;
	   boolean flag=true;
	   int counter=0;
	   int i;
	   
	   Serversetup() //constructor
	   		{
		   		Thread t = new Thread(this,"Server_setup_thread");
		   		try
		   			{
		   				s = SctpServerChannel.open();
		   				InetSocketAddress serverAddr = new InetSocketAddress(Globalvar.portnum[Globalvar.nodenum]);
		   				//s= new ServerSocket(Globalvar.portnum[Globalvar.nodenum]);//creates a server socket on the corresponding portnumber provided in the config file
		   				s.bind( serverAddr );
		   				System.out.println("Server socket set up on port num: " +Globalvar.portnum[Globalvar.nodenum]+"\n\n");
		   			}
		   		catch(IOException e){e.printStackTrace();}
		   		
		   		if(Globalvar.messagecount==0)   //initially each node takes a checkpoint
				{
					StringBuffer sb= new StringBuffer(100);
					Globalvar.seqNum++;
					sb.append(Globalvar.seqNum).append(" ");
					for(i=0;i<Globalvar.totnodes;i++)
						sb.append(Globalvar.vectorclock[i]).append(" ");
					for(i=0;i<Globalvar.numneighbors;i++)
						sb.append(Globalvar.messages[i]).append(" ");
					sb.append(Globalvar.messagecount).append(" ");
					if(Globalvar.active)
						sb.append("1 ");
					else sb.append("0 ");
					if(Globalvar.passive)
						sb.append("1 ");
					else sb.append("0");
					Globalvar.ckpt[Globalvar.seqNum-1]=sb.toString(); //store sequence number,vector clock,messages count and process state
					System.out.println("INITTIAL CHECKPOINT "+Globalvar.seqNum +" HAS BEEN TAKEN");
					System.out.print("CHECKPOINT INFO ");
					System.out.print(Globalvar.ckpt[Globalvar.seqNum-1]+"\n");
				}
		   		
		   		t.start();
	   		}
	
	   public void run()
	   		{
		   
		   		while(flag)
		   			{
		   				try
		   					{
		   						client=s.accept();  
		   					}
		   				catch(IOException e){e.printStackTrace();}
		   				new Rcvthread(client); //new class object to recieve messages sent by neighbors of the node
		   				counter++; //increment the number of neighbors that has contacted the current node
		   				if(counter==Globalvar.numneighbors) //once all neighbors have contacted the current node, you do not need to accept any more connections
		   					flag=false;
		   			}
		   	}
   }