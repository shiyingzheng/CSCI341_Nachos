package nachos.threads;

import java.util.LinkedList;
import nachos.machine.*;
import nachos.threads.*;

/**
 * A synchronized queue.
 */
public class SynchList {
    /**
     * Allocate a new synchronized queue.
     */
    public SynchList() {
		list = new LinkedList<Object>();
		lock = new Lock();
		listEmpty = new Condition2(lock);
    }

    /**
     * Add the specified object to the end of the queue. If another thread is
     * waiting in <tt>removeFirst()</tt>, it is woken up.
     *
     * @param	o	the object to add. Must not be <tt>null</tt>.
     */
    public void add(Object o) {
		Lib.assertTrue(o != null);
		
		lock.acquire();
		list.add(o);
		listEmpty.wake();
		lock.release();
    }

    /**
     * Remove an object from the front of the queue, blocking until the queue
     * is non-empty if necessary.
     *
     * @return	the element removed from the front of the queue.
     */
    public Object removeFirst() {
		Object o;	

		lock.acquire();
		while (list.isEmpty()){
		    listEmpty.sleep();
		    System.out.println("Sleeping");
		}
		o = list.removeFirst();
		lock.release();	

		return o;
    }

    private static class PingTest implements Runnable {
		PingTest(SynchList ping, SynchList pong) {
		    this.ping = ping;
		    this.pong = pong;
		}
		
		public void run() {
		    for (int i=0; i<10; i++){
		    	Object first = ping.removeFirst(); // remove from ping list
		    	System.out.println("from ping remove " + first);
				pong.add(first); //add to pong list
				System.out.println("to pong add " + first);
			}
		}	

		private SynchList ping;
		private SynchList pong;
    }

    public void addAll(Object o, int numThreads) {
    	Lib.assertTrue(o != null);

    	lock.acquire();
    	for(int i = 0; i < numThreads; i++)
    	    list.add(o);
    	listEmpty.wakeAll();
    	lock.release();
    }

    private static class AddAllTest implements Runnable {
    	AddAllTest(String name, SynchList monitor) {
    	    this.monitor = monitor;
    	    this.name = name;
    	}

	    public void run() {
	        System.out.println(name + " is running.");
	        System.out.println(name + " has removed " + monitor.removeFirst() + " from the list ");
	    }

    	private SynchList monitor;
    	private String name;
    }
    
	/**
     * Test that this module is working.
     */
    public static void selfTest() {
		SynchList ping = new SynchList();
		SynchList pong = new SynchList();	

		new KThread(new PingTest(ping, pong)).setName("ping").fork();	

		for (int i=0; i<10; i++) {
		    Integer o = new Integer(i);
		    System.out.println("main adding " + i + " to ping");
		    ping.add(o); //add int to ping list
		    Object first = pong.removeFirst();
		    Lib.assertTrue(first == o); //remove from pong list
		    System.out.println("main remove " + first + " from pong");
		}

		SynchList addAllList = new SynchList();
	    for (int i=1; i<5; i++)
	        new KThread(new AddAllTest("Grabber test " + Integer.toString(i), addAllList)).setName("Grabber thread " + Integer.toString(i)).fork();
	    System.out.println("We've added everything!");
	    KThread.currentThread().yield(); // Make sure all of our GrabberTest threads get to call removeFirst() before we advance
	    addAllList.addAll(new Integer(69), new Integer(3));
	    KThread.currentThread().yield(); // Make sure all of our GrabberTest threads get to be woken up after our call to addAll
    }

    private LinkedList<Object> list;
    private Lock lock;
    private Condition2 listEmpty;
}

