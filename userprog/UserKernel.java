package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.ArrayList;

/* Shiying Zheng, Ben Stern, Joey Gonzales-Dones, Katherine Chan, Jacob Chae */

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
  /**
   * Allocate a new user kernel.
   */
  public UserKernel() {
    super();
  }

  /**
   * Initialize this kernel. Creates a synchronized console and sets the
   * processor's exception handler.
   */
  public void initialize(String[] args) {
    super.initialize(args);    

    pageListLock = new Lock();
    freePageList = new LinkedList<Integer>();

    fileListLock = new Lock();
    openFileList = new HashMap<String, ArrayList<Integer>>();

    pidLock = new Lock();

    processTableLock = new Lock();

    console = new SynchConsole(Machine.console());

    Machine.processor().setExceptionHandler(new Runnable() {
      public void run() { exceptionHandler(); }
    });

    for (int i = 0; i < Machine.processor().getNumPhysPages(); i++){
      freePageList.add(i);
    }
  }

  /**
   * Test the console device.
   */	
  public void selfTest() {
    super.selfTest();  

    System.out.println("Testing the console device. Typed characters");
    System.out.println("will be echoed until q is typed.");    

    char c;    

    do {
      c = (char) console.readByte(true);
      console.writeByte(c);
    }
    while (c != 'q');  

    System.out.println("");
  }

  /**
   * Returns the current process.
   *
   * @return	the current process, or <tt>null</tt> if no process is current.
   */
  public static UserProcess currentProcess() {
    if (!(KThread.currentThread() instanceof UThread))
      return null;

    return ((UThread) KThread.currentThread()).process;
  }

  /**
   * The exception handler. This handler is called by the processor whenever
   * a user instruction causes a processor exception.
   *
   * <p>
   * When the exception handler is invoked, interrupts are enabled, and the
   * processor's cause register contains an integer identifying the cause of
   * the exception (see the <tt>exceptionZZZ</tt> constants in the
   * <tt>Processor</tt> class). If the exception involves a bad virtual
   * address (e.g. page fault, TLB miss, read-only, bus error, or address
   * error), the processor's BadVAddr register identifies the virtual address
   * that caused the exception.
   */
  public void exceptionHandler() {
    Lib.assertTrue(KThread.currentThread() instanceof UThread);    

    UserProcess process = ((UThread) KThread.currentThread()).process;
    int cause = Machine.processor().readRegister(Processor.regCause);
    process.handleException(cause);
  }

  /**
   * Start running user programs, by creating a process and running a shell
   * program in it. The name of the shell program it must run is returned by
   * <tt>Machine.getShellProgramName()</tt>.
   *
   * @see	nachos.machine.Machine#getShellProgramName
   */
  public void run() {
    super.run();   

    UserProcess process = UserProcess.newUserProcess();

    String shellProgram = Machine.getShellProgramName();	
    Lib.assertTrue(process.execute(shellProgram, new String[] { }));   

    KThread.currentThread().finish();
  }

  /**
   * Terminate this kernel. Never returns.
   */
  public void terminate() {
    super.terminate();
  }

  /** Globally accessible reference to the synchronized console. */
  public static SynchConsole console;

  /** Global list of free pages, protected by pageListLock */
  public static LinkedList<Integer> freePageList;

  /** A lock for the free page list*/
  public static Lock pageListLock;

  /** 
   * @key, the file descriptor
   * @value, a List of two elements, the first indicating how many processes have a file open;
   *  the second element is 1 if unlink has been called, 0 otherwise
   **/
  public static HashMap<String, ArrayList<Integer>> openFileList;

  /** A lock for the open file list*/
  public static Lock fileListLock;

  // dummy variables to make javac smarter
  private static Coff dummy1 = null;

  // the lock that protects currPid; we should use it whenever we try to get a new pid
  public static Lock pidLock;

  // the next pid to be used
  public static int currPid = 1;

  // table of processes and their status
  public static HashMap<Integer, Integer> processStatusTable = new HashMap<Integer, Integer>();

  // lock for processStatusTable
  public static Lock processTableLock;
}
