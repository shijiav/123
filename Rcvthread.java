
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public class Rcvthread implements Runnable
	{

	   SctpChannel recvclient=null;
	   //BufferedReader br;
	   ByteBuffer byteBuffer;
	   int i=0,j=0,k=0,x=0,y=0,counter=0;
	   int first=1;
	   boolean flag=true;
	   int sourceprocess;
	   
	   Rcvthread(SctpChannel c)
	   		{
	   			byteBuffer = ByteBuffer.allocate(512);
	   			byteBuffer.clear();
		   		recvclient=c;
		   		Thread t= new Thread(this,"Receiving_thread");
		   		t.start();
	   		}
	
	   void initialize()
		{
			Globalvar.tentckpt=0;
			Globalvar.repliesRcvd=0;
			Globalvar.requestSent=0;
			Globalvar.requestRcvd=0;
			Globalvar.need_to_reply=0;
			Globalvar.tentroll=0;
		}
	   
	   	public static String byteToString(ByteBuffer byteBuffer) {
			byteBuffer.position(0);
			byteBuffer.limit(512);
			byte[] bufArr = new byte[byteBuffer.remaining()];
			byteBuffer.get(bufArr);
			byteBuffer.clear();
			byteBuffer.put(new byte[512]);
			byteBuffer.clear();
			return new String(bufArr);
		}
	   
	   synchronized public void run()
	   		{
		   		String str; //temporary string
		   		try
					{
						while(!Globalvar.terminated)
							{
								MessageInfo messageInfo = recvclient.receive( byteBuffer, null, null );
								str = byteToString( byteBuffer );
							    //br=new BufferedReader(new InputStreamReader(recvclient.getInputStream( ))); //buffered reader to read from the socket  
								if( str != null )
									{ 
									  String st[]=str.split("\\s");
									  if (str.startsWith("Application"))
											{
										  		for(i=0;i<Globalvar.totnodes;i++) //modify the vector clock according to application message recieved
										  		{
										  			if(Globalvar.vectorclock[i] < Integer.parseInt(st[3+i]))
										  				Globalvar.vectorclock[i]=Integer.parseInt(st[3+i]);
										  		}
										  		Globalvar.vectorclock[Globalvar.nodenum]++;
										  		System.out.println("APPLICATION MESSAGE: Node " +st[2] +" received an application message from Node " +st[1]);
										  		//System.out.print("VECTOR CLOCK VALUE: ");
		    									//for(i=0;i<Globalvar.totnodes;i++)
		    									// System.out.print(Globalvar.vectorclock[i]+" ");
										  		
										  		//STORING THE LLR VALUES AND FORMING THE COHORT SET
		    									for(i=0;i<Globalvar.numneighbors;i++) //determining the neighbor from which message is recieved
		    									   {
		    									 	if(Globalvar.neighbors[i]==Integer.parseInt(st[1]))
		    									 		sourceprocess=i;
		    									   }
		    									
		    									//if(Globalvar.tentroll==0)
		    									Globalvar.llr[sourceprocess]=Integer.parseInt(st[3+Globalvar.totnodes].trim()); //incrementing the llr value
		    									
		    									
		    									
		    									if(Globalvar.tentckpt==1 || Globalvar.tentroll==1)//recording the lost messages 
									  			{
									  			 Globalvar.lost[sourceprocess]++;
									  			}
		    									
		    									/*
		    									System.out.println();
		    									System.out.println("The values of llr are:");
		    									for(j=0;j<Globalvar.numneighbors;j++)
		    										System.out.print(Globalvar.llr[j] + " ");
		    									*/
		    									
		    									Globalvar.cohort[Integer.parseInt(st[1])]=1; // adding the source node to the cohort
		    									if(!Globalvar.active && !Globalvar.passive) Globalvar.active=true; //if the current node is passive and it recieves a message then node becomes active
		    									
		    									//System.out.println("The cohort values are:");
		    									//for(j=0;j<Globalvar.totnodes;j++)
		    									//	System.out.print(Globalvar.cohort[j] + " ");
		    									//System.out.println();
		    									
		    								}
									  else if(str.startsWith("Request"))
									  		{
										  System.out.println("NODE "+Globalvar.nodenum +" RECIEVED A REQUEST MESSAGE FROM NODE "+st[1] +" LLR: " +st[3].trim());										  //Globalvar.tentckpt=1;
										  
										   for(i=0;i<Globalvar.numneighbors;i++) //determining the neighbor from which message is recieved
								  			{
								  				if(Globalvar.neighbors[i]==Integer.parseInt(st[1]))
								  					sourceprocess=i;
								  			}
										    //System.out.println("The value of fls is "+ Globalvar.fls[sourceprocess]);
											if(Globalvar.fls[sourceprocess]>0 && Integer.parseInt(st[3].trim())>=Globalvar.fls[sourceprocess])
											{
												Globalvar.tentckpt=1;
												
												//Globalvar.requests[Globalvar.requestRcvd]=sourceprocess;
												Globalvar.requests[0]=sourceprocess;
										        //Globalvar.requestRcvd++;
												Globalvar.requestRcvd=1;
										        //System.out.println("The value of requests recieved is: " +Globalvar.requestRcvd);
										  		System.out.println("\n*********CHECKPOINTING INITIATED*********");
										  		
										  		for(i=0;i<Globalvar.numneighbors;i++)
										    		Globalvar.lls[i]=Globalvar.templls[i];
											
										    	StringBuffer tempsb= new StringBuffer(100);
										    	Globalvar.seqNum++;
										    	tempsb.append(Globalvar.seqNum).append(" ");
										    	for(i=0;i<Globalvar.totnodes;i++)
										    		tempsb.append(Globalvar.vectorclock[i]).append(" ");
										    	for(i=0;i<Globalvar.numneighbors;i++)
										    		tempsb.append(Globalvar.messages[i]).append(" ");
										    	tempsb.append(Globalvar.messagecount).append(" ");
										    	if(Globalvar.active)
										    		tempsb.append("1 ");
										    	else tempsb.append("0 ");
										    	if(Globalvar.passive)
										    		tempsb.append("1 ");
										    	else tempsb.append("0");
										    	Globalvar.ckpt[Globalvar.seqNum-1]=tempsb.toString(); //store sequence number,vector clock,messages count and process state
										    	System.out.println("TENTATIVE CHECKPOINT "+Globalvar.seqNum +" HAS BEEN TAKEN");
										    	System.out.print("TENTATIVE CHECKPOINT INFO ");
										    	System.out.print(Globalvar.ckpt[Globalvar.seqNum-1]+"\n\n");
										    	

												for(x=0;x<Globalvar.totnodes;x++)
													Globalvar.cohortold[x]=Globalvar.cohort[x];
												
										    	for(x=0;x<Globalvar.numneighbors;x++)
									    		{
												Globalvar.fls[x]=0;
												Globalvar.templls[x]=0;
									    		}
										    	for(x=0;x<Globalvar.totnodes;x++)
										    		Globalvar.cohort[x]=0;
										 	
										  	}
											else
												{
												Globalvar.need_to_reply=1;
												Globalvar.requests[Globalvar.requestRcvd]=sourceprocess;
										        Globalvar.requestRcvd++;
										        //System.out.println("The value of requests recieved is: " +Globalvar.requestRcvd);
												}
										  	
									  		}
									  else if (str.startsWith("Reply"))
									  		{
										  		System.out.println("NODE "+Globalvar.nodenum +" RECIEVED A REPLY MESSAGE FROM NODE "+st[1]);
										  		Globalvar.repliesRcvd++;
									  		}
									  else if (str.startsWith("Decision"))
								  		{
										  
										  	System.out.println("DECISION MESSAGE RECIEVED");
									  		//if(Globalvar.decisionRcvd==0 && counter==0)
										  	//System.out.println("current op :" +Globalvar.currentop +"The recieved value of current op is" +st[1]);
										  	if(Globalvar.currentop==(Integer.parseInt(st[1].trim()) -1))
									  			{	
										  		    Globalvar.currentop++;
									  				if(Globalvar.tentckpt==1)
									  					System.out.println("*********MAKING TENTATIVE CHECKPOINT PERMANENT*********\n");
									  				Globalvar.decisionRcvd=1;
									  				initialize();
									  				if(Globalvar.currentop<Globalvar.numop)
									  				{
									  				String str1[]=Globalvar.operations[Globalvar.currentop].split(",");
													 if(Integer.parseInt(str1[1])==Globalvar.nodenum)
														 new Instdelay();
													}
									  				initialize();
									  			}
									  		
									  	}
									  
									  else if(str.startsWith("Recover"))
									  {
										  System.out.println("##NODE "+Globalvar.nodenum +" RECIEVED A RECOVERY REQUEST MESSAGE FROM NODE "+st[1] +" LLS: " +st[3].trim() +"##");
										  //Globalvar.tentroll=1;
										  
										   for(i=0;i<Globalvar.numneighbors;i++) //determining the neighbor from which message is recieved
								  			{
								  				if(Globalvar.neighbors[i]==Integer.parseInt(st[1]))
								  					sourceprocess=i;
								  			}
										    //System.out.println("The value of fls is "+ Globalvar.fls[sourceprocess]);
										    //System.out.println("@@@@@@@@@@@The LLR VALUE IS " +Globalvar.llr[])
											if(Globalvar.llr[sourceprocess]>Integer.parseInt(st[3].trim()) && Globalvar.tentroll==0)
											{
												Globalvar.tentroll=1;
												
												/*Globalvar.requests[Globalvar.requestRcvd]=sourceprocess;
										        Globalvar.requestRcvd++;
										        */
												
												
												Globalvar.requests[0]=sourceprocess;
										        //Globalvar.requestRcvd++;
												Globalvar.requestRcvd=1;
										        
												
												
										        //System.out.println("The value of requests recieved is: " +Globalvar.requestRcvd);
										  		System.out.println("\n*********RECOVERY ALGORITHM INITIATED*********");
										  		
										    }
											else
												{
													Globalvar.requests[Globalvar.requestRcvd]=sourceprocess;
													Globalvar.requestRcvd++;
													if(Globalvar.tentroll==0)
														Globalvar.need_to_reply=1;
													//System.out.println("The value of requests recieved is: " +Globalvar.requestRcvd);
												}
										  	
										  
									  }
									  else if(str.startsWith("Rollback"))
									  {
										  System.out.println("ROLLBACK MESSAGE RECIEVED FROM NODE " +st[2]);
									  		//if(Globalvar.decisionRcvd==0 && counter==0)
										  	//System.out.println("current op :" +Globalvar.currentop +"The recieved value of current op is" +st[1]);
										  			
										  	if(Globalvar.currentop==(Integer.parseInt(st[1])-1))
									  			{	
										  		    Globalvar.currentop++;
									  				if(Globalvar.tentroll==1)
									  					System.out.println("\n*********INITIATING ROLLBACK*********");
									  				Globalvar.rollbackRcvd=1;
									  				
									  				initialize();
									  				/*
									  				if(Globalvar.currentop<Globalvar.numop)
									  				{
									  				String str1[]=Globalvar.operations[Globalvar.currentop].split(",");
													 if(Integer.parseInt(str1[1])==Globalvar.nodenum)
														 new Instdelay();
													 
									  				}*/
									  			}
										  	
										  	int sourceprocess=0;
							  				 for(i=0;i<Globalvar.numneighbors;i++) //determining the neighbor from which message is recieved
									  			{
									  				if(Globalvar.neighbors[i]==Integer.parseInt(st[2]))
									  					sourceprocess=i;
									  			}
											   
							  				Globalvar.recievelost[sourceprocess]=Integer.parseInt(st[3].trim());
							  				//System.out.println("@@@@@@@@@@@@@@@MESSAGES LOST IN RECIEVING FROM " +st[2] +" :"+Globalvar.recievelost[sourceprocess]);
							  				
									  		
									  }
									 else 
									  		{
										  		Globalvar.terminated=true;
									  		}
									}	
							}
					}
				catch(IOException e){e.printStackTrace();}
			
				try{
					recvclient.close();
				   }
				catch(IOException e){e.printStackTrace();}
			
		}
	   
	}