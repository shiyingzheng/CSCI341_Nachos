package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    Lock lock;
    Condition speaking;
    Condition listening;
    int mailbox;

    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new Lock();
        speaking = new Condition(lock);
        listening = new Condition(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        speaking.sleep();
        lock.acquire();
        mailbox = word;
        lock.release();
        listening.wake();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
	   speaking.wake();
       listening.sleep();
       lock.acquire();
       int received = mailbox;
       lock.release();
       return received;
    }

    private static class SpeakTest implements Runnable {
        String name;
        Communicator communicator;

        SpeakTest(String name, Communicator communicator) {
            this.name = name;
            this.communicator = communicator;
        }
        
        public void run() {
            for (int i = 0; i < 3; i++){
                this.communicator.speak(i);
                System.out.println(this.name + " says " + i);
            }
            
        }   
    }

    private static class ListenTest implements Runnable {
        String name;
        Communicator communicator;
        ListenTest(String name, Communicator communicator) {
            this.name = name;
            this.communicator = communicator;
        }
        
        public void run() {
            for (int i = 0; i < 3; i++){
                int message = this.communicator.listen();
                System.out.println(this.name + " hears " + message);
                //System.out.println("listentest running");
            }
        }   
    }    

    /**
     * Test if this module is working.
     */
    public static void selfTest() {
        Communicator communicator = new Communicator();

        new KThread(new SpeakTest("Speak Test 1", communicator)).fork();
        new KThread(new SpeakTest("Speak Test 2", communicator)).fork();
        new KThread(new ListenTest("Listen Test 1", communicator)).fork();
        new ListenTest("Listen Test 2", communicator).run();
    }
}
