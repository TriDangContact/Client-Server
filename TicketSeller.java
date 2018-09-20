// java program to demonstrate 
// use of semaphores Locks
import java.util.concurrent.*;
import java.util.Random;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.PrintStream;


//A shared resource/class.
class Shared 
{
    final static int TICKETS = 500;
    static int count = 500;
    static int returns = 0;
    static int soldA = 0;
    static int soldB = 0;
    static int soldC = 0;
    static int buyers = 0;
    static int row = 20;
    static int column = 25;
    static int seats[][] = new int[row][column];           // 0 = seat not sold and is available, 1 = seat sold and is unavailable
}
 
class MyThread extends Thread implements TicketSellerRemote
{
    Semaphore sem;
    String threadName;
    public MyThread(Semaphore sem, String threadName) 
    {
        super(threadName);
        this.sem = sem;
        this.threadName = threadName;
    }
 
    @Override
    public void run() {
        final int RETURN_RATE = 10;          //1 out of N, rate of users returning tickets
        final int SLEEP_RATE = 5;          //1 out of N, rate of user just browsing
        final int TICKET_COUNT = Shared.count;       //max ticket count
        final int TICKET_RETURN = 20;       //max number of ticket return thread allowed
        final int TICKET_RELEASE = 3;       //number of tickets that can be bought during one thread
        final int SIM_SPEED = 200;          //use fast ticket simulation starting at n tickets
        // create instance of Random class
        Random rand = new Random();
        int randTickets, randSleep, randReturn, randNum;
        int ticketsSold = 0;

        printSeats();
        //sellers keep on selling tickets if there is still available
        while (Shared.count > 0) {
        //System.out.println("Starting " + threadName);
            try
            {
                // First, try to browse.
                // acquiring the lock
                sem.acquire();
                Shared.buyers++;
                System.out.println(threadName + " is browsing tickets");
                // Simulate time for various user activity
                randomDelay();
                // Now, accessing the shared resource.
                // other waiting threads will wait, until this 
                // thread release the lock

                //if there's no more tickets
                if (Shared.count == 0) {
                    return;
                }
                //if there's still tickets left
                else {
                    //simulate 1 in 20 browser will exceed browsing limit
                    randSleep = rand.nextInt(SLEEP_RATE);
                    randReturn = rand.nextInt(RETURN_RATE);
                    if (randSleep == 0) {
                        System.out.println(threadName + " browsed for more than 2 minutes. Returning to pool!");
                    }
                    //simulate 1 in 5 browser will return 1 ticket
                    else if (randReturn == 0) {
                        if (Shared.returns < TICKET_RETURN && Shared.count < TICKET_COUNT ) {
                            boolean returnSuccessful = false;
                            while (!returnSuccessful) {
                                randReturn = rand.nextInt(Shared.TICKETS);      //get a random n out of TICKETS
                                int row = randReturn/Shared.column;             //get random row number
                                int column = randReturn % Shared.column;        //get random column number
                                if (Shared.seats[row][column] == 1) {           //if the seat was sold previously, authorize return
                                    Shared.seats[row][column] = 0;              //mark seat as available to sell
                                    Shared.count++;
                                    Shared.returns++;
                                    returnSuccessful = true;
                                    System.out.println(threadName + " is RETURNING 1 ticket at row: "
                                         +row+ " :column " +column+ ". Tickets left: " +Shared.count);
                                }
                            }                            
                        }
                        //System.out.println(threadName+ " tried to RETURN tickets.");
                    }
                    else {
                        // Generate random integers in range 1 to 3 to simulate num of tickets user wants to buy
                        randTickets = rand.nextInt(TICKET_RELEASE) + 1;
                        //if there's still tickets left
                        if ((Shared.count - randTickets) >= 0) {
                            //if ticket > n, use regular random generator to search for seats
                            if (Shared.count > SIM_SPEED) {
                                //get random seat position to simulate user searching for seat to buy
                                randNum = rand.nextInt(Shared.TICKETS);
                                int row = randNum/Shared.column;             //get random row number
                                int column = randNum % Shared.column;        //get random column number
                                
                                //if all of the next n number of seats are available
                                if (seatSelectionAvailable(row, column, randTickets)) {
                                    //sell the seats
                                    int temp = sellTickets(row, column, randTickets);
                                    ticketsSold += temp;                                    
                                    System.out.println("Tickets SOLD by thread " +threadName+ ": " +randTickets+ 
                                        ". Tickets left: " + Shared.count);
                                }
                                //if there's still seats available, but they are not consecutive 
                                else {
                                    System.out.println("Selected seats not available! "
                                                + "Tried to sell: " +randTickets+ 
                                                    " starting at row " +row+ " column " +column+ 
                                                    ". Tickets left: " + Shared.count);
                                }
                            }
                            
                            //if ticket count < n, implement brute force seat search to speed up simulation
                            else {
                                for (int row = 0; row < Shared.row; row++){ 
                                    for (int column = 0; column < Shared.column; column++) {                                        
                                        if (seatSelectionAvailable(row,column,randTickets)) {
                                            int temp = sellTickets(row, column, randTickets);
                                            ticketsSold += temp;                                            
                                            System.out.println("Tickets SOLD by thread " +threadName+ ": " +randTickets+ 
                                                ". Tickets left: " + Shared.count);
                                        }
                                        //if there's still seats available, but they are not consecutive 
                                        else {
                                            System.out.println("Selected seats not available! "
                                                + "Tried to sell: " +randTickets+ 
                                                    " starting at row " +row+ " column " +column+ 
                                                    ". Tickets left: " + Shared.count);
                                        }                                                                                
                                    }
                                }
                            }
                            
                        }
                        //if there's buying more tickets than available
                        else {
                            System.out.println("NOT ENOUGH tickets available! "
                                    + "Tried to sell: " +randTickets+ ". Tickets left: " + Shared.count);
                        }
                    }
                }
            } catch (InterruptedException exc) {
                    System.out.println(exc);
                }
            // Release the lock.
            //System.out.println(threadName + " is no longer buying");
            if (threadName.equals("A")) {
                Shared.soldA = ticketsSold;
            }
            else if (threadName.equals("B")) {
                Shared.soldB = ticketsSold;
            }
            else {
                Shared.soldC = ticketsSold;
            }
            System.out.println(threadName+ " total tickets sold: " +ticketsSold);
            sem.release();
        }
        System.out.println("No more tickets!");
        System.out.println("Done! Final ticket count: " + Shared.count);
        printInfo();
        printSeats();
    }
    
    public void randomDelay() {
        try {
            Random rand = new Random(); 
            int randDelay = rand.nextInt(200);
            Thread.sleep(randDelay);
        } catch (InterruptedException exc){
            System.out.println(exc);
        }
    }
    
    //method to check if all seats for current selection are available
    //@returns true if we can sell the seats, false otherwise
    public boolean seatSelectionAvailable(int row, int column, int randTicket) {
        boolean canBuy = false;
        boolean done = false;
        int j = 0;
        if (column + randTicket <= Shared.column) {     //prevent ArrayOutOfBounds error
            canBuy = true;
            while (j < randTicket && done == false) {      //check to see if all seats in current selection are available                          
                if (Shared.seats[row][column+j] == 0) {
                    //canBuy = true;
                    j++;
                }
                else {                      //if any seats in current selection already sold, do not sell current selection
                    canBuy = false;
                    done = true;
                }
            }                                                                                    
        }
        return canBuy;
    }
    
    //method to sell seats at specified position
    //@returns number of tickets sold for current grab
    public int sellTickets(int row, int column, int randTicket) {
        int ticketsSold = 0;
        for (int y = 0; y < randTicket; y++) {
            int temp = column + y;            
            if (Shared.seats[row][column + y] == 0) {
                Shared.seats[row][column + y] = 1;          //sell the seats
                Shared.count--;
                ticketsSold++;
                System.out.println("Thread " +threadName+ " selling 1 ticket at row " 
                        +row+ " column " +temp+ ". Tickets left: " + Shared.count);
            }
            else {
                System.out.println("ERROR seats not available! "
            + "Tried to sell seat at row " +row+ " column " +temp+ ". Tickets left: " + Shared.count);
            }           
        }
        return ticketsSold;
    }
    
    public void printSeats() {
        for(int i = 0; i < Shared.row; i++) {
            for(int j = 0; j < Shared.column; j++) {
                System.out.printf("%1d ", Shared.seats[i][j]);
            }
            System.out.println();
        }
    }
    
    public void printInfo() {                
        System.out.println("Total tickets sold by A: " +Shared.soldA);
        System.out.println("Total tickets sold by B: " +Shared.soldB);
        System.out.println("Total tickets sold by C: " +Shared.soldC);
        int sold = Shared.soldA + Shared.soldB + Shared.soldC;
        System.out.println("Total tickets sold: " +sold+ ". Total returned: " +Shared.returns+ ". Total Buyers: " +Shared.buyers);
    }
}

 
// Driver class
public class TicketSeller 
{
    public static void main(String args[])
    {    
	try {        
		// creating a Semaphore object
		// with number of permits 3
		
		Semaphore sem = new Semaphore(1);
		 
		// creating 3 threads with name A, B, C
		
		MyThread mt1 = new MyThread(sem, "A");
		//MyThread mt2 = new MyThread(sem, "B");
		//MyThread mt3 = new MyThread(sem, "C");
		
/////////////////IMPLEMENT THESE IF NOT USING RMI///////////////////////
		// starting threads, which calls run() in MyThread
		//mt1.start();
		//mt2.start();
		//mt3.start();
		// waiting for threads to finish
		//mt1.join();
		//mt2.join();
		//mt3.join();
		
		// all threads will complete their execution

//////////////////CODE FOR RMI, DELETE EVERYTHING BELOW IF NOT USING RMI///////////////
		TicketSellerRemote stub = (TicketSellerRemote) UnicastRemoteObject.exportObject(mt1, 0);

		//Bind the remote object's stub in the registry
		Registry registry = LocateRegistry.getRegistry();
		registry.bind("TicketSellerRemote", stub);

		System.err.println("Server ready");
	
        
		
	} catch (Exception e) {
		System.err.println("Server exception: " + e.toString());
		e.printStackTrace();
	}
                       
    }
}
