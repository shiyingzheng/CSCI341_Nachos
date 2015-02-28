package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	   this.conditionLock = conditionLock;
       waitQueue = new LinkedList<KThread>();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        Machine.interrupt().disable(); 

    	Lib.assertTrue(conditionLock.isHeldByCurrentThread()); 

        KThread thread = KThread.currentThread();
        waitQueue.add(thread); 

    	conditionLock.release();  

        //uncomment to see which thread is sleeping
        //System.out.println("sleep thread" + thread.toString());

        thread.sleep();

    	conditionLock.acquire();

        Machine.interrupt().enable(); 
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
        Machine.interrupt().disable(); 

	    Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        if (!waitQueue.isEmpty()){
            KThread thread = waitQueue.removeFirst();
            thread.ready();
            //uncomment to see which thread is waking up
            //System.out.println("wake thread" + thread.toString());
        }

        Machine.interrupt().enable(); 
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	   Lib.assertTrue(conditionLock.isHeldByCurrentThread());

       while (!waitQueue.isEmpty())
            wake();
    }

    private Lock conditionLock;
    private LinkedList<KThread> waitQueue;
}
