package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		       
		return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		       
		return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
			       
		Lib.assertTrue(priority >= priorityMinimum &&
			   priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
			       
		KThread thread = KThread.currentThread();	

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
		    return false;	

		setPriority(thread, priority+1);	

		Machine.interrupt().restore(intStatus);
		return true;
    }

    public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
			       
		KThread thread = KThread.currentThread();	

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
		    return false;	

		setPriority(thread, priority-1);	

		Machine.interrupt().restore(intStatus);
		return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
		    thread.schedulingState = new ThreadState(thread);	

		return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
		    this.transferPriority = transferPriority;
		    queue = new LinkedList<KThread>();
		}

		public void waitForAccess(KThread thread) {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    if (lockHolder != null) {
		    	lockHolder.effectivePriority = lockHolder.getPriority();
		    }
		    ThreadState t = pickNextThread();
		    if (t == null){
		    	return null;
		    }
		    acquire(t.thread);
		    return t.thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected ThreadState pickNextThread() {
			KThread bestThread = null;
			int highestPriority = -1;
			//print();
			for (KThread t : queue){
				int tPriority = getEffectivePriority(t);
				if (tPriority > highestPriority){
					highestPriority = tPriority;
					bestThread = t;
				}
			}
			if (bestThread == null){
				return null;
			}
		    return getThreadState(bestThread);
		}
	
		public void print() {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    boolean printed = false;
		    if (queue.isEmpty()){
		    	System.out.println("Queue is empty. Sad.");
		    }
		    for (KThread th:queue){
		    	ThreadState state = getThreadState(th);
		    	if (printed){
		    		System.out.print(" --> ");
		    	}
		    	System.out.print("[" + th + ", P " + state.priority + ", EP " + 
		    	state.effectivePriority + "]");
		    	printed = true;
		    }
		    System.out.println();
		}	

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;

		/**
		* Underlying storage for the priority queue.
		*/
		LinkedList<KThread> queue;

		/**
		* Current lock holder in the queue.
		*/
		ThreadState lockHolder;
	}

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param	thread	the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
		    this.thread = thread;

		    setPriority(priorityDefault);
		}	

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return	the priority of the associated thread.
		 */
		public int getPriority() {
		    return priority;
		}	

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
		    return effectivePriority;
		}	

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority) {
		    if (this.priority == priority)
				return;
		    
		    this.priority = priority;
		    this.effectivePriority = priority;
		}	

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());
		    System.out.println("adding "+thread);
		    waitQueue.queue.add(thread);
		}	

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
		   	waitQueue.queue.remove(thread);
		   	waitQueue.lockHolder = this;
		   	
		   	int maxPriority = this.getEffectivePriority();
		   	for(KThread t:waitQueue.queue){
		   		ThreadState state = getThreadState(t);
		   		int p = state.getEffectivePriority();
				System.out.println("Priority on queue of " + p);
		   		if (p > maxPriority){
		   			maxPriority = p;
		   		}
		   	}
		   	System.out.println(thread.getName() + " has effective priority "+maxPriority);
		   	this.effectivePriority = maxPriority;	   	
		}		

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		/** The effective priority of the thread */
		int effectivePriority;
		/** All donations received **/
		LinkedList<PriorityQueue> donations = new LinkedList<PriorityQueue>();
    }

    private static class SchedulerTest implements Runnable {
        String name;
        Lock lock;

        SchedulerTest(String name, Lock lock) {
            this.name = name;
            this.lock = lock;
        }

        public void acquireLock(){
        	lock.acquire();
        }

        public void releaseLock(){
        	lock.release();
        }
        
        public void run() {
        	acquireLock();
        	SchedulerTest1 test2 = new SchedulerTest1("Test 2", lock);
        	SchedulerTest2 test3 = new SchedulerTest2("Test 3", lock);
        	KThread y = new KThread(test2).setName("Test 2");
        	KThread z = new KThread(test3).setName("Test 3");
        	Machine.interrupt().disable();
            ThreadedKernel.scheduler.setPriority(y,6);
			ThreadedKernel.scheduler.setPriority(z,5);
			Machine.interrupt().enable();
			y.fork();
        	z.fork();
        	
            for (int i = 0; i < 3; i++){
                System.out.println(name + " meows " + i + " times");
                KThread.yield();
            }
            releaseLock();
            
        }
    }
    private static class SchedulerTest1 implements Runnable {
        String name;
        Lock lock;

        SchedulerTest1(String name, Lock lock) {
            this.name = name;
            this.lock = lock;
        }

        public void acquireLock(){
        	lock.acquire();
        }

        public void releaseLock(){
        	lock.release();
        }
        
        public void run() {
        	acquireLock();
            for (int i = 0; i < 3; i++){
                System.out.println(name + " meows " + i + " times");
                KThread.yield();
            }
            releaseLock();
        }
    }
    private static class SchedulerTest2 implements Runnable {
        String name;
        Lock lock;

        SchedulerTest2(String name, Lock lock) {
            this.name = name;
            this.lock = lock;
        }

        public void acquireLock(){
        	lock.acquire();
        }

        public void releaseLock(){
        	lock.release();
        }
        
        public void run() {
            for (int i = 0; i < 3; i++){
                System.out.println(name + " meows " + i + " times");
                KThread.yield();
            }
        }
    }

    /**
     * Test if this module is working.
     * in run: acquire lock, create 2 threads
     * they have to have different run methods
     * one run method acquires the lock, the other is normal
     */
    public static void selfTest() {
		System.out.println("Test 1 has priority 2 and acquires the lock, then forks " + 
			"Test 2 with priority 7 and Test 3 with priority 5. Test 2 attempts to " +
			"acquire the lock.");

		Lock l = new Lock();

		SchedulerTest t1 = new SchedulerTest("Test 1", l);

		KThread x = new KThread(t1).setName("Test 1");
		
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(x,2);
		Machine.interrupt().enable();

		x.fork();

        for (int i = 0; i < 30; i++){
            KThread.currentThread().yield();
        }

    }
}
