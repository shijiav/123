import java.net.*;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class Sndthread implements Runnable
	{
		Thread t;
		ArrayList<SctpChannel> client = new ArrayList<SctpChannel>();
		SctpChannel clientsoc=null;

		ByteBuffer byteBuffer;

		
		int i,j,x,y;
		int destinationIndex;
		int nummessage;
		PrintWriter out;
		
		
	
		Sndthread()
			{
				for(x=0;x<Globalvar.numneighbors;x++)
					{
						try{
								InetSocketAddress serverAddr = new InetSocketAddress(Globalvar.ipaddr[Globalvar.neighbors[x]],Globalvar.portnum[Globalvar.neighbors[x]]);
								//clientsoc=new Socket(Globalvar.ipaddr[Globalvar.neighbors[x]],Globalvar.portnum[Globalvar.neighbors[x]]);//create a socket connection with neighbor
								clientsoc = SctpChannel.open();
								clientsoc.connect( serverAddr, 0, 0 );
								client.add(clientsoc); //add to array list
						    }
						catch(Exception e){e.printStackTrace();}
					}
				t= new Thread(this,"Sending_thread");
				t.start();
	  		}
	    
		void send()  
	    {
			if(Globalvar.currentop<Globalvar.numop)
			{
				String str[]=Globalvar.operations[Globalvar.currentop].split(",");
				if(str[0].equals("c"))
				  {
					if(Integer.parseInt(str[1])==Globalvar.nodenum)
						{
							Globalvar.currentop++;
							ckptinitiator();
						}
				  }
				else if (str[0].equals("r"))
				  {
					if(Integer.parseInt(str[1])==Globalvar.nodenum)
						{
							Globalvar.currentop++;
							recovery();
						}
				  }
			}
		}
		
		
	
		
		
		
		void ckptinitiator()
		{
			//try {FileOutputStream fp= new FileOutputStream(Globalvar.nodenum+".txt");} catch(Exception e){e.printStackTrace();}
			//fp.write("I have entered the file");
			System.out.println("\n*********#CHECKPOINTING ALGORITHM INITIATED#*********");
			Globalvar.tentckpt=1;
			
			for(x=0;x<Globalvar.numneighbors;x++) //copy the lls values
				Globalvar.lls[x]=Globalvar.templls[x];
			StringBuffer tempsb= new StringBuffer(100);
			Globalvar.seqNum++;
			tempsb.append(Globalvar.seqNum).append(" ");
			for(x=0;x<Globalvar.totnodes;x++)
				tempsb.append(Globalvar.vectorclock[x]).append(" ");
			for(x=0;x<Globalvar.numneighbors;x++)
				tempsb.append(Globalvar.messages[x]).append(" ");
			tempsb.append(Globalvar.messagecount).append(" ");
			if(Globalvar.active)
				tempsb.append("1 ");
			else tempsb.append("0 ");
			if(Globalvar.passive)
				tempsb.append("1 ");
			else tempsb.append("0");
			Globalvar.ckpt[Globalvar.seqNum-1]=tempsb.toString(); //store sequence number,vector clock,messages count and process state
			System.out.println("TENTATIVE CHECKPOINT "+Globalvar.seqNum +" TAKEN");
			System.out.print("TENTATIVE CHECKPOINT INFO ");
			System.out.print(Globalvar.ckpt[Globalvar.seqNum-1]+"\n\n");
			
			for(x=0;x<Globalvar.totnodes;x++) //store the cohort values
				Globalvar.cohortold[x]=Globalvar.cohort[x];
			
			for(x=0;x<Globalvar.totnodes;x++) //initialize the cohort
				Globalvar.cohort[x]=0;
			
			//Globalvar.requests[Globalvar.requestRcvd]=-1;  //to idicate that it is initiator
			Globalvar.requests[0]=-1;
			Globalvar.requestRcvd=1; 
			
			for(x=0;x<Globalvar.numneighbors;x++)
			{
			Globalvar.fls[x]=0;
			Globalvar.templls[x]=0;
			}
			
			checkpoint();
		}

	public static void sendMessage(SctpChannel Clientsock, String Message) {
	    
		// prepare byte buffer to send massage
		ByteBuffer sendBuffer = ByteBuffer.allocate(512);
		sendBuffer.clear();
		// Reset a pointer to point to the start of buffer
		sendBuffer.put(Message.getBytes());
		sendBuffer.flip();
		try {
			// Send a message in the channel
			MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
			Clientsock.send(sendBuffer, messageInfo);
			sendBuffer.clear();
		} catch (IOException ex) {
			
		}

	}
	    
		void checkpoint()
		{
			
			int destination=0;
			for(x=0;x<Globalvar.totnodes;x++) // broadcast request message to cohort elements
				if(Globalvar.cohortold[x]==1)
				{
					for(y=0;y<Globalvar.numneighbors;y++)
						 {
						   if(Globalvar.neighbors[y]==x)
							   destination=y;
						 }
					clientsoc= client.get(destination);//get the socket info for the neighbor from the array list
					String message = "Request: " +Globalvar.nodenum +" "+Globalvar.neighbors[destination]+" " +Globalvar.llr[destination];
					sendMessage( clientsoc, message );
					
					//try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
					//catch (IOException e) {e.printStackTrace();}
					//out.println("Request: " +Globalvar.nodenum +" "+Globalvar.neighbors[destination]+" " +Globalvar.llr[destination]);
					System.out.println("REQUEST MESSAGE SENT FROM NODE " +Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[destination] +" LLR:"+Globalvar.llr[destination]);
					//System.out.println("REQUEST MESSAGE SENT FROM NODE " +Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[destination]);
					Globalvar.requestSent++;
				    try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
					
				}
			
			for(x=0;x<Globalvar.numneighbors;x++)  //reinitialize llr values
				Globalvar.llr[x]=0;		
			
			if(Globalvar.requestSent==0) //if no node is present in cohort then reply back
				{
				    if(Globalvar.requests[0]==-1)
				    	{
				    	  System.out.println("*********#MAKING TENTATIVE CHECKPOINT PERMANENT#*********\n");
				    	  for(y=0;y<Globalvar.numneighbors;y++)
						  {
						    clientsoc= client.get(y);//get the socket info for the neighbor from the array list
						    String message = "Decision: "+Globalvar.currentop;
							sendMessage( clientsoc, message );

						    //try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
							//catch (IOException e) {e.printStackTrace();}
							//out.println("Decision: "+Globalvar.currentop);
							//System.out.println("DECISION MESSAGE SENT FROM NODE "+Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[y]+" CURRENT OP:" +Globalvar.currentop);
							System.out.println("DECISION MESSAGE SENT FROM NODE "+Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[y]);
							try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
						  }
							if(Globalvar.currentop<Globalvar.numop)
			  				{
								String str1[]=Globalvar.operations[Globalvar.currentop].split(",");
								if(Integer.parseInt(str1[1])==Globalvar.nodenum)
								 		new Instdelay();
							 }
						  
				    	  initialize();
				    	  return;
				    	}
					clientsoc= client.get(Globalvar.requests[0]);//get the socket info for the neighbor from the array list
                    String message = "Reply: " +Globalvar.nodenum +" "+Globalvar.neighbors[Globalvar.requests[0]];
                    sendMessage( clientsoc, message );

					// try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
					// catch (IOException e) {e.printStackTrace();}
					// out.println("Reply: " +Globalvar.nodenum +" "+Globalvar.neighbors[Globalvar.requests[0]]);
					System.out.println("REPLY MESSAGE SENT FROM NODE " +Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[Globalvar.requests[0]]);
					try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				}
		
		int first=1;
		while(Globalvar.decisionRcvd==0)
			{
			   if(Globalvar.requestRcvd>1) //if there is a request then send a reply message  
						 {
							 for(x=1,y=0;x<Globalvar.requestRcvd;x++)
							 {
								 clientsoc= client.get(Globalvar.requests[x]);//get the socket info for the neighbor from the array list
								 String message = "Reply: " +Globalvar.nodenum +" "+Globalvar.neighbors[Globalvar.requests[x]];
                                sendMessage( clientsoc, message );

         //                         try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
								 // catch (IOException e) {e.printStackTrace();}
								 // out.println("Reply: " +Globalvar.nodenum +" "+Globalvar.neighbors[Globalvar.requests[x]]);
								 System.out.println("REPLY MESSAGE SENT FROM NODE " +Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[Globalvar.requests[x]]);
								 y++;
								 try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
							 }
							 Globalvar.requestRcvd-=y;
						 }
					   
			   if(Globalvar.repliesRcvd==Globalvar.requestSent && Globalvar.requestSent!=0)
			   		{
				   		if(Globalvar.requests[0]==-1)
				   			{
				   				Globalvar.decisionRcvd=1;
				   				System.out.println("*********#MAKING TENTATIVE CHECKPOINT PERMANENT#*********\n");
				   				for(y=0;y<Globalvar.numneighbors;y++)
				   					{
				   						clientsoc= client.get(y);//get the socket info for the neighbor from the array list
                                        String message = "Decision: "+Globalvar.currentop;
                                        sendMessage( clientsoc, message );
				   						
             //                            try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
				   						// catch (IOException e) {e.printStackTrace();}
				   						// out.println("Decision: "+Globalvar.currentop);
				   						//System.out.println("DECISION MESSAGE SENT FROM NODE "+Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[y] +" CURRENT OP:" +Globalvar.currentop);
				   						System.out.println("DECISION MESSAGE SENT FROM NODE "+Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[y]);
				   						try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				   					}
				   						if(Globalvar.currentop<Globalvar.numop)
				   							{
				   								String str1[]=Globalvar.operations[Globalvar.currentop].split(",");
				   								if(Integer.parseInt(str1[1])==Globalvar.nodenum)
				   									new Instdelay();
				   							}
				   					
				   				initialize();
				   				return;
				   			}
				   		else
				   			{
				   				if(first==1)
				   					{
				   						first=0;
				   						clientsoc= client.get(Globalvar.requests[0]);//get the socket info for the neighbor from the array list
				   						String message = "Reply: " +Globalvar.nodenum +" "+Globalvar.neighbors[Globalvar.requests[0]];
                                        sendMessage( clientsoc, message );

             //                            try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
				   						// catch (IOException e) {e.printStackTrace();}
				   						// out.println("Reply: " +Globalvar.nodenum +" "+Globalvar.neighbors[Globalvar.requests[0]]);
				   						System.out.println("REPLY MESSAGE SENT FROM NODE " +Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[Globalvar.requests[0]]);
				   						try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				   					}
				   			}
			
			   		}		
		}//while
	
		Globalvar.decisionRcvd=0;
		for(y=0;y<Globalvar.numneighbors;y++)
			{
				    clientsoc= client.get(y);//get the socket info for the neighbor from the array list
                    String message = "Decision: "+Globalvar.currentop;
                    sendMessage( clientsoc, message );

				 //    try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
					// catch (IOException e) {e.printStackTrace();}
					// out.println("Decision: "+Globalvar.currentop);
					//System.out.println("DECISION MESSAGE SENT FROM NODE "+Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[y]+" CURRENT OP:" +Globalvar.currentop);
					System.out.println("DECISION MESSAGE SENT FROM NODE "+Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[y]);
					try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
			}
	 }
		
		
		void roll()
		{
			
			//System.out.println("The old vector clock value is:");
			//for(x=0;x<Globalvar.totnodes;x++)
			//	System.out.print(Globalvar.vectorclock[x] +" ");
			//System.out.print("\n");
			
			
			String strtemp=Globalvar.ckpt[Globalvar.seqNum-1];
			String sttemp[];
			sttemp=strtemp.split(" ");
			for(x=0;x<Globalvar.totnodes;x++)
				Globalvar.vectorclock[x]=Integer.parseInt(sttemp[1+x]);
			for(x=0;x<Globalvar.numneighbors;x++)
				Globalvar.messageslost[x]=Globalvar.messages[x]-Integer.parseInt(sttemp[Globalvar.totnodes+x+1]);
			for(x=0;x<Globalvar.numneighbors;x++)
				Globalvar.messages[x]=Integer.parseInt(sttemp[Globalvar.totnodes+x+1]);
			
			if(Globalvar.active==true)
				Globalvar.state[0]=1;
			else Globalvar.state[0]=0;
			
			if(Globalvar.passive==true)
				Globalvar.state[1]=1;
			else Globalvar.state[1]=0;
			
			if(Integer.parseInt(sttemp[Globalvar.totnodes+2+Globalvar.numneighbors])==1)
				Globalvar.active=true;
			else Globalvar.active=false;
			if(Integer.parseInt(sttemp[Globalvar.totnodes+3+Globalvar.numneighbors])==1)
				Globalvar.passive=true;
			else Globalvar.passive=false;
			
			//System.out.println("The new vector clock value is:");
			//for(x=0;x<Globalvar.totnodes;x++)
			//	System.out.print(Globalvar.vectorclock[x] +" ");
			//System.out.print("\n");
			
			//System.out.println("The messages lost are:");
			//for(x=0;x<Globalvar.numneighbors;x++)
			//	System.out.print(Globalvar.messageslost[x] +" ");
			//System.out.print("\n");
			
			
			if(Globalvar.requests[0]!=-1)
			{
				messagerecover();
				System.out.println("ROLLBACK SUCCESSFUL\n");
			}
		}
		
		void messagerecover()
		{
			
			//System.out.println("MESSAGES SUCCESSFULLY RECOVERED");
			
			for(x=0;x<Globalvar.numneighbors;x++)
				for(y=0;y<Globalvar.messageslost[x];y++)
			 		{
			 			Globalvar.vectorclock[Globalvar.nodenum]++; //increment the vector entry of current node before piggy backing it
			 			StringBuffer sb=new StringBuffer(100); //string to piggyback vector timestamp
			 			sb=sb.append(" ");
			
			 			for(j=0;j<Globalvar.totnodes;j++)
			 				sb=sb.append(Globalvar.vectorclock[j]).append(" ");  //store the vector clock to piggy back along with application message
			
			 			Globalvar.messages[x]++;
			 			sb.append(Globalvar.messages[x]);
			
			 			if(Globalvar.fls[x]==0) 
			 				Globalvar.fls[x]=Globalvar.messages[x]; //store the first label sent
			 			Globalvar.templls[x]=Globalvar.messages[x]; //store the last label sent
		   
			 			clientsoc= client.get(x);//get the socket info for the neighbor from the array list
                        String message = "Application: " +Globalvar.nodenum +" "+Globalvar.neighbors[x]+sb.toString();
                        sendMessage( clientsoc, message );

			 			// try {out= new PrintWriter(clientsoc.getOutputStream(),true);} catch (IOException e) {e.printStackTrace();} 
			 			// out.println("Application: " +Globalvar.nodenum +" "+Globalvar.neighbors[x]+sb);
			 			//System.out.println("APPLICATION MESSAGE: Node " +Globalvar.nodenum +" sent the lost application message to Node " +Globalvar.neighbors[x] +" FLS:" +Globalvar.fls[x] +" LLS:" +Globalvar.templls[x]);
			 			System.out.println("APPLICATION MESSAGE: Node " +Globalvar.nodenum +" sent the lost application message to Node " +Globalvar.neighbors[x] );
			 			//System.out.println("VECTOR CLOCK VALUE:" +sb);
			 		}
			 
			 if(Globalvar.state[0]==1)
				 Globalvar.active=true;
			 else Globalvar.active=false;
			 if(Globalvar.state[1]==1)
				 Globalvar.passive=true;
			 else Globalvar.passive=false;
			
			 System.out.println("MESSAGES SUCCESSFULLY RESTORED");
			if(Globalvar.currentop<Globalvar.numop)
				{
				   String str1[]=Globalvar.operations[Globalvar.currentop].split(",");
			       if(Integer.parseInt(str1[1])==Globalvar.nodenum)
				          new Instdelay();
				}	
		}
		
		
		void recovery()
		{
			System.out.println("\n*********#CURRENT NODE FAILED AND ROLLED BACK#*********");
			System.out.println("*********#RECOVERY ALGORITHM INITIATED#*********");
			
			Globalvar.tentroll=1;
			
			Globalvar.requests[0]=-1;
			Globalvar.requestRcvd=1; 
			
			roll();
			
			for(x=0;x<Globalvar.numneighbors;x++)
    		{
			Globalvar.fls[x]=0;
			Globalvar.templls[x]=0;
			}
			
			recover();
		}
		
		void recover()
		{
			
			for(y=0;y<Globalvar.numneighbors;y++)
				   {
					clientsoc= client.get(y);//get the socket info for the neighbor from the array list
                    String message = "Recover: " +Globalvar.nodenum +" "+Globalvar.neighbors[y]+" " +Globalvar.lls[y];
                    sendMessage( clientsoc, message );

					// try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
					// catch (IOException e) {e.printStackTrace();}
					// out.println("Recover: " +Globalvar.nodenum +" "+Globalvar.neighbors[y]+" " +Globalvar.lls[y]);
					System.out.println("##RECOVERY REQUEST MESSAGE SENT FROM NODE " +Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[y] +" LLS:"+Globalvar.lls[y] +" ##");
					Globalvar.requestSent++;
				    try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				   }
			
			for(x=0;x<Globalvar.numneighbors;x++)  //reinitialize llr values
				Globalvar.llr[x]=1000;		
		
			int first=1;
			while(Globalvar.rollbackRcvd==0)
			{
		       if(Globalvar.requestRcvd>1) //if there is a request then send a reply message  
							 {
								 for(x=1,y=0;x<Globalvar.requestRcvd;x++)
								 {
									 clientsoc= client.get(Globalvar.requests[x]);//get the socket info for the neighbor from the array list
									 String message = "Reply: " +Globalvar.nodenum +" "+Globalvar.neighbors[Globalvar.requests[x]];
                                     sendMessage( clientsoc, message );

          //                            try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
									 // catch (IOException e) {e.printStackTrace();}
									 // out.println("Reply: " +Globalvar.nodenum +" "+Globalvar.neighbors[Globalvar.requests[x]]);
									 System.out.println("##RECOVERY REPLY MESSAGE SENT FROM NODE " +Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[Globalvar.requests[x]] +" ##");
									 y++;
									 try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
								 }
								 Globalvar.requestRcvd-=y;
							 }
						   
			  if(Globalvar.repliesRcvd==Globalvar.requestSent)
				{
					if(Globalvar.requests[0]==-1)
						 {
							Globalvar.rollbackRcvd=1;
							//System.out.println("*********#ROLLING BACK REMAINING PROCESSES#*********");
							for(y=0;y<Globalvar.numneighbors;y++)
								  {
								    clientsoc= client.get(y);//get the socket info for the neighbor from the array list
                                    String message = "Rollback: "+Globalvar.currentop+" "+Globalvar.nodenum+" " +Globalvar.lost[y];
                                    sendMessage( clientsoc, message );

								 //    try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
									// catch (IOException e) {e.printStackTrace();}
									// out.println("Rollback: "+Globalvar.currentop+" "+Globalvar.nodenum+" " +Globalvar.lost[y]);
									System.out.println("##ROLLBACK MESSAGE SENT FROM NODE "+Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[y] +"##");
									try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
								  }
									
							messagerecover();
							initialize();
							return;
						}
				else
						{
							 if(first==1)
							 {
							 first=0;
					   		 clientsoc= client.get(Globalvar.requests[0]);//get the socket info for the neighbor from the array list
							 String message = "Reply: " +Globalvar.nodenum +" "+Globalvar.neighbors[Globalvar.requests[0]];
                             sendMessage( clientsoc, message );

        //                      try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
							 // catch (IOException e) {e.printStackTrace();}
							 // out.println("Reply: " +Globalvar.nodenum +" "+Globalvar.neighbors[Globalvar.requests[0]]);
							 System.out.println("##REPLY MESSAGE SENT FROM NODE " +Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[Globalvar.requests[0]] +"##");
							 try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
							 }
						}
				
				}		
			}//while
		
			Globalvar.rollbackRcvd=0;
					for(y=0;y<Globalvar.numneighbors;y++)
					  {
					    clientsoc= client.get(y);//get the socket info for the neighbor from the array list
                        String message = "Rollback: "+Globalvar.currentop+" "+Globalvar.nodenum+" "+Globalvar.lost[y];
                        sendMessage( clientsoc, message );

					 //    try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
						// catch (IOException e) {e.printStackTrace();}
						// out.println("Rollback: "+Globalvar.currentop+" "+Globalvar.nodenum+" "+Globalvar.lost[y]);
						System.out.println("##ROLLBACK MESSAGE SENT FROM NODE "+Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[y] +"##");
						try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
					}
					roll();
		 }
			
		
		void nextop()
		{
			for(y=0;y<Globalvar.numneighbors;y++)
			  {
			    clientsoc= client.get(y);//get the socket info for the neighbor from the array list
                String message = "Decision: "+Globalvar.currentop;
                sendMessage( clientsoc, message );			    

    //             try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
				// catch (IOException e) {e.printStackTrace();}
				// out.println("Decision: "+Globalvar.currentop);
				//System.out.println("DECISION MESSAGE SENT FROM NODE "+Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[y]+" CURRENT OP:" +Globalvar.currentop);
				System.out.println("DECISION MESSAGE SENT FROM NODE "+Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[y]);
				try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				Globalvar.decisionRcvd=0;
			}
		}
		
		void nextop1()
		{
			for(y=0;y<Globalvar.numneighbors;y++)
			  {
			    clientsoc= client.get(y);//get the socket info for the neighbor from the array list
			    String message = "Rollback: "+Globalvar.currentop+" " +Globalvar.nodenum+" " +Globalvar.lost[y];
                sendMessage( clientsoc, message );

    //             try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
				// catch (IOException e) {e.printStackTrace();}
				// out.println("Rollback: "+Globalvar.currentop+" " +Globalvar.nodenum+" " +Globalvar.lost[y]);
				System.out.println("##ROLLBACK MESSAGE SENT FROM NODE "+Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[y] +"##");
				try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				Globalvar.rollbackRcvd=0;
			}
			if(Globalvar.currentop<Globalvar.numop)
				{
				 String str1[]=Globalvar.operations[Globalvar.currentop].split(",");
			     if(Integer.parseInt(str1[1])==Globalvar.nodenum)
				 new Instdelay();
			 
				}
		}
		
		void initialize()
		{
			Globalvar.tentckpt=0;
			Globalvar.repliesRcvd=0;
			Globalvar.requestSent=0;
			Globalvar.requestRcvd=0;
			Globalvar.decisionRcvd=0;
			Globalvar.rollbackRcvd=0;
			Globalvar.tentroll=0;
		}
		
		void reply()
		{
			 if(Globalvar.requestRcvd>0) //if there is a request then send a reply message  
			 {
				 for(x=0,y=0;x<Globalvar.requestRcvd;x++)
				 {
					 clientsoc= client.get(Globalvar.requests[x]);//get the socket info for the neighbor from the array list
					 String message = "Reply: " +Globalvar.nodenum +" "+Globalvar.neighbors[Globalvar.requests[x]];
                     sendMessage( clientsoc, message );

      //                try {out= new PrintWriter(clientsoc.getOutputStream(),true);}
					 // catch (IOException e) {e.printStackTrace();}
					 // out.println("Reply: " +Globalvar.nodenum +" "+Globalvar.neighbors[Globalvar.requests[x]]);
					 System.out.println("REPLY MESSAGE SENT FROM NODE " +Globalvar.nodenum +" TO NODE " +Globalvar.neighbors[Globalvar.requests[x]]);
					 y++;
					 try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				 }
				 Globalvar.requestRcvd-=y;
			 }
		}
		
	synchronized public void run()
		 {
		
		   while(!Globalvar.terminated)
				{
	    			  if(Globalvar.currentop==0 && Globalvar.messagecount!=0) // initiate the first operation i.e either checkpointing or recovery
	    			 	   send();  
	    			  if(Globalvar.tentckpt==1) checkpoint();
	    			  if(Globalvar.tentroll==1) recover();
	    			  if(Globalvar.need_to_reply==1) reply();
	    			  if(Globalvar.decisionRcvd==1) nextop();
	    			  if(Globalvar.rollbackRcvd==1) { nextop1();}
	    			  if(Globalvar.initiate==1)
	    			     {
	    				    Globalvar.initiate=0;
	    				    send();
	    			     }
	    				
	    			   if(Globalvar.active) //if current node is active
	    		     	 {
	    				   if(Globalvar.tentckpt==1) checkpoint();
	    				   if(Globalvar.decisionRcvd==1) nextop();
	 	    			   if(Globalvar.tentroll==1) recover();
	    				   if(Globalvar.rollbackRcvd==1){ nextop1();}
	 	    			   if(Globalvar.need_to_reply==1) reply();
	    				   if(Globalvar.initiate==1)
	 	    			  	{
	 	    				  Globalvar.initiate=0;
	 	    				  send();
	 	    			   }
		    			   nummessage=Globalvar.randgen.nextInt((Globalvar.maxperactive+1)-Globalvar.minperactive)+Globalvar.minperactive;//generates a random message count between min and max
	    				   for(i=0;i<20;i++)
	    						{
	    						   destinationIndex=Globalvar.randgen.nextInt(Globalvar.numneighbors); //generates a random neighbor
	    							Globalvar.messagecount++; //increment message count
	    							System.out.println("RANDOM COUNT: " +nummessage +" CURRENT COUNT: " +i +" TOTAL COUNT " +Globalvar.messagecount +" DESTIN " +Globalvar.neighbors[destinationIndex]);
									Globalvar.vectorclock[Globalvar.nodenum]++; //increment the vector entry of current node before piggy backing it
									StringBuffer sb=new StringBuffer(100); //string to piggyback vector timestamp
									sb=sb.append(" ");
									
									for(j=0;j<Globalvar.totnodes;j++)
										sb=sb.append(Globalvar.vectorclock[j]).append(" ");  //store the vector clock to piggy back along with application message
									
									Globalvar.messages[destinationIndex]++;
									sb.append(Globalvar.messages[destinationIndex]);
									
									if(Globalvar.fls[destinationIndex]==0) 
										Globalvar.fls[destinationIndex]=Globalvar.messages[destinationIndex]; //store the first label sent
									Globalvar.templls[destinationIndex]=Globalvar.messages[destinationIndex]; //store the last label sent
			   
									/*
									System.out.println();
									System.out.println("The values of fls and lls are:");
									for(j=0;j<Globalvar.numneighbors;j++)
										System.out.print(Globalvar.fls[j] + " ");
									System.out.print(" # ");
									for(j=0;j<Globalvar.numneighbors;j++)
										System.out.print(Globalvar.templls[j] + " ");
									System.out.println();
									*/
									
									if(Globalvar.tentckpt==1)
										checkpoint();
									if(Globalvar.tentroll==1)
										recover();
									
									clientsoc= client.get(destinationIndex);//get the socket info for the neighbor from the array list
									String message = "Application: " +Globalvar.nodenum +" "+Globalvar.neighbors[destinationIndex]+sb.toString();
									sendMessage( clientsoc, message );

           //                               out= new PrintWriter(clientsoc.getOutputStream(),true); 
									// out.println("Application: " +Globalvar.nodenum +" "+Globalvar.neighbors[destinationIndex]+sb);
									
									//System.out.println("APPLICATION MESSAGE: Node " +Globalvar.nodenum +" sent a application message to Node " +Globalvar.neighbors[destinationIndex] +" FLS:" +Globalvar.fls[destinationIndex] +" LLS:" +Globalvar.templls[destinationIndex]);
									System.out.println("APPLICATION MESSAGE: Node " +Globalvar.nodenum +" sent a application message to Node " +Globalvar.neighbors[destinationIndex]);
									//System.out.println("VECTOR CLOCK VALUE:" +sb);
									if(i+1==nummessage)Globalvar.active=false;
									if(Globalvar.messagecount>=Globalvar.maxnumber)Globalvar.passive=true;
	    							
	    							try{Thread.sleep(Globalvar.minsenddelay*100);}//*******************during this period you could probably sendout marker messages
	    							catch(InterruptedException e){e.printStackTrace();}
	    						}
	    					System.out.println("SEND THREAD AFTER SENDING");
	    				}
				}
	    	for(i=0;i<Globalvar.numneighbors;i++)
	    	{
	    		clientsoc = client.get(i);
	    		try{
	    		clientsoc.close();
	    		}
	    		catch(IOException e){e.printStackTrace();}
	    	}
	    	
	    	
		}
		
	}
	
	

