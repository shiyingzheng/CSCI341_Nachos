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
  Boolean written;
  int mailbox;

  /**
   * Allocate a new communicator.
   */
  public Communicator() {
    lock = new Lock();
    speaking = new Condition(lock);
    listening = new Condition(lock);
    written = false;
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
    lock.acquire();
    while (written){
      //System.out.println(KThread.currentThread().getName() + " speak bedtime");
      listening.wake();
      speaking.sleep();
      //System.out.println(KThread.currentThread().getName() + " speak woke up");
    }

    mailbox = word;

    written = true;

    listening.wakeAll();
    lock.release();
  }

  /**
   * Wait for a thread to speak through this communicator, and then return
   * the <i>word</i> that thread passed to <tt>speak()</tt>.
   *
   * @return	the integer transferred.
   */    
  public int listen() {
    lock.acquire();
    speaking.wakeAll();

    while (!written){
      //System.out.println(KThread.currentThread().getName() + " listen bedtime");
      speaking.wake();
      listening.sleep();
      //System.out.println(KThread.currentThread().getName() + " listen woke up");
    }

    int received = mailbox;

    written = false;

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
      }
    }   
  }    

  /**
   * Test if this module is working.
   */
  public static void selfTest() {
    Communicator communicator = new Communicator();

    new KThread(new SpeakTest("Speak Test 1", communicator)).setName("Speak Test 1").fork();
    new KThread(new SpeakTest("Speak Test 2", communicator)).setName("Speak Test 2").fork();
    new KThread(new ListenTest("Listen Test 1", communicator)).setName("Listen Test 1").fork();
    new KThread(new ListenTest("Listen Test 2", communicator)).setName("Listen Test 2").fork();

    for (int i = 0; i < 20; i++){
      KThread.currentThread().yield();
      //System.out.println("main thread "+ i);
    }
  }
}
