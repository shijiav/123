
public class Instdelay implements Runnable{

		Instdelay(){
			Thread t= new Thread(this,"Minimum_send_delay_thread");
			t.start();
		}
		
		public void run()
		{
			try{Thread.sleep(Globalvar.mininstdelay*1000);}
			catch(InterruptedException e){e.printStackTrace();}
			Globalvar.initiate=1;
		}
		
	}
