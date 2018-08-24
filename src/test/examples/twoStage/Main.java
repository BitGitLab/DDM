package test.examples.twoStage;

/**
 * @author Xuan
 * Created on Apr 27, 2005
 * 
 * test Case 1
   number of twostage threads :  1
   number of read threads     :  1
 */
public class Main extends Thread {

    static int iTthreads = 4;
    static int iRthreads = 4;
    TwoStage ts;
    Data data1,data2;
	
	public void run() {

		data1 = new Data();
		data2 = new Data();
		ts = new TwoStage(data1,data2);
		TwoStageThread[] stageThreads = new TwoStageThread[iTthreads];
		ReadThread[] readThreads = new ReadThread[iRthreads];

		for (int i=0;i<iTthreads;i++){
			stageThreads[i] = new TwoStageThread(ts);
			stageThreads[i].start();
		}
		for (int i=0;i<iRthreads;i++){
			readThreads[i] = new ReadThread(ts);
			readThreads[i].start();
		}

		try{

			for (int i=0;i<iTthreads;i++)
				stageThreads[i].join();
			for (int i=0;i<iRthreads;i++)
				readThreads[i].join();
		} catch (InterruptedException e) {

		}

	}
	
	public static void main(String[] args) {

		iTthreads = 4;
		iRthreads = 4;
		if (args.length < 2){

		    //System.out.println("ERROR: Expected 2 parameters");
			Main t=new Main();
			t.run();
		}else{

			iTthreads = Integer.parseInt(args[0]);
			iRthreads = Integer.parseInt(args[1]);
			Main t=new Main();
			t.run();
		}
	}
}
