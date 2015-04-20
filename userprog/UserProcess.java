package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.HashMap;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
  /**
   * Allocate a new process.
   */
  public UserProcess() {
    fileOpenTable = new HashMap<Integer, OpenFile>();
    filenameOpenTable = new HashMap<String, Integer>();
    readOffsetTable = new HashMap<Integer, Integer>();
    writeOffsetTable = new HashMap<Integer, Integer>();

    setup();
    nextFileDescriptor=3;
  }

  private void setup(){
    OpenFile stdin = UserKernel.console.openForReading();
    OpenFile stdout = UserKernel.console.openForWriting();

    // initialize in the openFileTable
    fileOpenTable.put(0, stdin);
    fileOpenTable.put(1, stdout);

    // initialize in the filenameOpenTable
    filenameOpenTable.put(stdin.getName(), 0);
    filenameOpenTable.put(stdout.getName(), 1);

    // initialize in the offsetTable
    readOffsetTable.put(0, 0);
    readOffsetTable.put(1, 0);

    // initialize in the writeOffsetTable
    writeOffsetTable.put(0, 0);
    writeOffsetTable.put(1, 0);
  }

  /**
   * Allocate and return a new process of the correct class. The class name
   * is specified by the <tt>nachos.conf</tt> key
   * <tt>Kernel.processClassName</tt>.
   *
   * @return	a new process of the correct class.
   */
  public static UserProcess newUserProcess() {
    return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
  }

  /**
   * Execute the specified program with the specified arguments. Attempts to
   * load the program, and then forks a thread to run it.
   *
   * @param	name	the name of the file containing the executable.
   * @param	args	the arguments to pass to the executable.
   * @return	<tt>true</tt> if the program was successfully executed.
   */
  public boolean execute(String name, String[] args) {
    if (!load(name, args))
      return false;

    new UThread(this).setName(name).fork();    

    return true;
  }

  /**
   * Save the state of this process in preparation for a context switch.
   * Called by <tt>UThread.saveState()</tt>.
   */
  public void saveState() {
  }

  /**
   * Restore the state of this process after a context switch. Called by
   * <tt>UThread.restoreState()</tt>.
   */
  public void restoreState() {
    Machine.processor().setPageTable(pageTable);
  }

  /**
   * Extract the page number component from a 32-bit address.
   *
   * @param address the 32-bit address.
   * @return  the page number component of the address.
   */
  private static int pageFromAddress(int address) {
    return (int) (((long) address & 0xFFFFFFFFL) / pageSize);
  }

  /**
   * Read a null-terminated string from this process's virtual memory. Read
   * at most <tt>maxLength + 1</tt> bytes from the specified address, search
   * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
   * without including the null terminator. If no null terminator is found,
   * returns <tt>null</tt>.
   *
   * @param	vaddr	the starting virtual address of the null-terminated
   *			string.
   * @param	maxLength	the maximum number of characters in the string,
   *				not including the null terminator.
   * @return	the string read, or <tt>null</tt> if no null terminator was
   *		found.
   */
  public String readVirtualMemoryString(int vaddr, int maxLength) {
    Lib.assertTrue(maxLength >= 0);    

    byte[] bytes = new byte[maxLength+1]; 
    /* System.out.println("num bytes " + bytes.length);  */

    int bytesRead = readVirtualMemory(vaddr, bytes);   

    for (int length=0; length<bytesRead; length++) {
      System.out.println("HERE IAM");
      if (bytes[length] == 0){
        return new String(bytes, 0, length);
      }
    }  

    return null;
  }

  /**
   * Transfer data from this process's virtual memory to all of the specified
   * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
   *
   * @param	vaddr	the first byte of virtual memory to read.
   * @param	data	the array where the data will be stored.
   * @return	the number of bytes successfully transferred.
   */
  public int readVirtualMemory(int vaddr, byte[] data) {
    return readVirtualMemory(vaddr, data, 0, data.length);
  }

  /**
   * Transfer data from this process's virtual memory to the specified array.
   * This method handles address translation details. This method must
   * <i>not</i> destroy the current process if an error occurs, but instead
   * should return the number of bytes successfully copied (or zero if no
   * data could be copied).
   *
   * @param	vaddr	the first byte of virtual memory to read.
   * @param	data	the array where the data will be stored.
   * @param	offset	the first byte to write in the array.
   * @param	length	the number of bytes to transfer from virtual memory to
   *			the array.
   * @return	the number of bytes successfully transferred.
   */
  public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
    Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);    

    byte[] memory = Machine.processor().getMemory();

    // for now, just assume that virtual addresses equal physical addresses
    if (vaddr < 0 || vaddr >= memory.length)
      return 0;  

    int amount = Math.min(length, memory.length-vaddr);
    int curLoc = 0;
    int rem = amount;
    /* System.out.println("amount " +amount); */
    int pageNumber = pageFromAddress(vaddr);
    for (int i = 0; i < amount/pageSize + 1; i++){
      // copy Math.min(pageSize, rem) number of bytes from memory at ppn 
      // to data at offset + curLoc
      /* System.out.println("PANDAS"); */
      System.arraycopy(memory, pageTable[pageNumber+i].ppn, 
        data, offset+curLoc, Math.min(pageSize, rem)); 
      //System.out.println("read remaining " + rem + " bytes");
      //System.out.println("read mem, ppn " + pageTable[vaddr/pageSize+i].ppn);
      //System.out.println("read mem, data " + offset+curLoc);
      // set used bit
      pageTable[pageNumber+i].used = true;
      // increment current location in data by page size
      curLoc += pageSize;
      // decrement remaining number of bytes by page size
      rem -= pageSize;
    }
    /* System.out.println("amount end " +amount); */
    
    /*
    System.out.println("read data array ");
    for (int i = 0; i < data.length; i++){
      System.out.println(data[i]);
    }
    */

    return amount;
  }

  /**
   * Transfer all data from the specified array to this process's virtual
   * memory.
   * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
   *
   * @param	vaddr	the first byte of virtual memory to write.
   * @param	data	the array containing the data to transfer.
   * @return	the number of bytes successfully transferred.
   */
  public int writeVirtualMemory(int vaddr, byte[] data) {
    return writeVirtualMemory(vaddr, data, 0, data.length);
  }

  /**
   * Transfer data from the specified array to this process's virtual memory.
   * This method handles address translation details. This method must
   * <i>not</i> destroy the current process if an error occurs, but instead
   * should return the number of bytes successfully copied (or zero if no
   * data could be copied).
   *
   * @param	vaddr	the first byte of virtual memory to write.
   * @param	data	the array containing the data to transfer.
   * @param	offset	the first byte to transfer from the array.
   * @param	length	the number of bytes to transfer from the array to
   *			virtual memory.
   * @return	the number of bytes successfully transferred.
   */
  public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
    Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);    

    byte[] memory = Machine.processor().getMemory();

    // for now, just assume that virtual addresses equal physical addresses
    if (vaddr < 0 || vaddr >= memory.length)
      return 0;  

    // amount is the number of bytes we need to copy over to memory
    int amount = Math.min(length, memory.length-vaddr);
    // curLoc is the current start location in data that we need to copy from
    int curLoc = 0;
    // rem is the remaining number of bytes we need to copy 
    int rem = amount;
    int pageNumber = pageFromAddress(vaddr);
    for (int i = 0; i < amount/pageSize + 1; i++){
      // copy Math.min(pageSize, rem) number of bytes from data at offset + curLoc
      // to memory at ppn
      System.arraycopy(data, offset+curLoc, memory, pageTable[pageNumber+i].ppn, 
        Math.min(pageSize, rem));
      String s = "";
      for(int j = offset+curLoc;j<offset+curLoc+Math.min(pageSize,rem);j++){
        s = s + (char)data[j];
      }
      System.out.println(s); 
      //System.out.println("write remaining " + rem + " bytes");
      //System.out.println("write mem, ppn " + pageTable[vaddr/pageSize+i].ppn);
      //System.out.println("write mem, data " + offset+curLoc);
      // set dirty bit and used bit in pageTable entry
      pageTable[vaddr/pageSize+i].used = true;
      pageTable[vaddr/pageSize+i].dirty = true;
      // increment current location in data array by page size
      curLoc += pageSize;
      // decrement remaining amount of bytes by page size
      rem -= pageSize;
    }

    /*
    System.out.println("write data array ");
    for (int i = 0; i < data.length; i++){
      System.out.println(data[i]);
    }
    */
    return amount;
  }

  /**
   * Load the executable with the specified name into this process, and
   * prepare to pass it the specified arguments. Opens the executable, reads
   * its header information, and copies sections and arguments into this
   * process's virtual memory.
   *
   * @param	name	the name of the file containing the executable.
   * @param	args	the arguments to pass to the executable.
   * @return	<tt>true</tt> if the executable was successfully loaded.
   */
  private boolean load(String name, String[] args) {
    Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
    OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
    if (executable == null) {
      Lib.debug(dbgProcess, "\topen failed");
      return false;
    }  

    try {
      coff = new Coff(executable);
    }
    catch (EOFException e) {
      executable.close();
      Lib.debug(dbgProcess, "\tcoff load failed");
      return false;
    }  

    // make sure the sections are contiguous and start at page 0
    numPages = 0;
    for (int s=0; s<coff.getNumSections(); s++) {
      CoffSection section = coff.getSection(s);
      if (section.getFirstVPN() != numPages) {
        coff.close();
        Lib.debug(dbgProcess, "\tfragmented executable");
        return false;
      }
      numPages += section.getLength();
    }  

    // make sure the argv array will fit in one page
    byte[][] argv = new byte[args.length][];
    int argsSize = 0;
    for (int i=0; i<args.length; i++) {
      argv[i] = args[i].getBytes();
      // 4 bytes for argv[] pointer; then string plus one for null byte
      argsSize += 4 + argv[i].length + 1;
    }
    if (argsSize > pageSize) {
      coff.close();
      Lib.debug(dbgProcess, "\targuments too long");
      return false;
    }  

    // program counter initially points at the program entry point
    initialPC = coff.getEntryPoint();	 

    // next comes the stack; stack pointer initially points to top of it
    numPages += stackPages;
    initialSP = numPages*pageSize; 

    // and finally reserve 1 page for arguments
    numPages++;    

    if (!loadSections())
      return false;  

    // store arguments in last page
    int entryOffset = (numPages-1)*pageSize;
    int stringOffset = entryOffset + args.length*4;    

    this.argc = args.length;
    this.argv = entryOffset;

    for (int i=0; i<argv.length; i++) {
      byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
      Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
      entryOffset += 4;
      Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
          argv[i].length);
      stringOffset += argv[i].length;
      Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
      stringOffset += 1;
    }  

    return true;
  }

  /**
   * Allocates memory for this process, and loads the COFF sections into
   * memory. If this returns successfully, the process will definitely be
   * run (this is the last step in process initialization that can fail).
   *
   * @return	<tt>true</tt> if the sections were successfully loaded.
   */
  protected boolean loadSections() {
    if (numPages > Machine.processor().getNumPhysPages()) {
      coff.close();
      Lib.debug(dbgProcess, "\tinsufficient physical memory");
      return false;
    }  

    pageTable = new TranslationEntry[numPages];
    UserKernel.pageListLock.acquire();
    for (int i=0; i<numPages; i++){
      int pageNumber = UserKernel.freePageList.removeFirst();
      pageTable[i] = new TranslationEntry(i,pageNumber,true,false,false,false);
    }

    UserKernel.pageListLock.release();

    // load sections
    for (int s=0; s<coff.getNumSections(); s++) {
      CoffSection section = coff.getSection(s);

      Lib.debug(dbgProcess, "\tinitializing " + section.getName()
          + " section (" + section.getLength() + " pages)");  

      for (int i=0; i<section.getLength(); i++) {
        int vpn = section.getFirstVPN()+i;    
        pageTable[vpn].readOnly = section.isReadOnly();
        section.loadPage(i, pageTable[vpn].ppn);
      }
    }

    return true;
  }

  /**
   * Release any resources allocated by <tt>loadSections()</tt>.
   */
  protected void unloadSections() {
    UserKernel.pageListLock.acquire();
    for (int i=0; i<numPages; i++){
      int paddr = pageTable[i].ppn;
      UserKernel.freePageList.add(paddr);
    }
    UserKernel.pageListLock.release();
  }    

  /**
   * Initialize the processor's registers in preparation for running the
   * program loaded into this process. Set the PC register to point at the
   * start function, set the stack pointer register to point at the top of
   * the stack, set the A0 and A1 registers to argc and argv, respectively,
   * and initialize all other registers to 0.
   */
  public void initRegisters() {
    Processor processor = Machine.processor(); 

    // by default, everything's 0
    for (int i=0; i<processor.numUserRegisters; i++)
      processor.writeRegister(i, 0); 

    // initialize PC and SP according
    processor.writeRegister(Processor.regPC, initialPC);
    processor.writeRegister(Processor.regSP, initialSP);   

    // initialize the first two argument registers to argc and argv
    processor.writeRegister(Processor.regA0, argc);
    processor.writeRegister(Processor.regA1, argv);
  }

  /**
   * Handle the halt() system call. 
   */
  private int handleHalt() {
    Machine.halt();

    Lib.assertNotReached("Machine.halt() did not halt machine!");
    return 0;
  }

  /*
   * Handle the creat() system call.
   */
  private int handleCreat(int a0){
    String fileName = readVirtualMemoryString(a0, 256);

    if(fileName == null) {
      //System.out.println("file name is null in create");
      return -1;
    }

    if (filenameOpenTable.containsKey(fileName)){
      return filenameOpenTable.get(fileName);
    }

    OpenFile file = ThreadedKernel.fileSystem.open(fileName, true);

    if(file == null) {
      //System.out.println("file " + fileName + " is null in create");
      return -1;
    }

    fileOpenTable.put(nextFileDescriptor, file);
    filenameOpenTable.put(fileName, nextFileDescriptor);
    readOffsetTable.put(nextFileDescriptor, 0); 
    writeOffsetTable.put(nextFileDescriptor, 0); 
    return nextFileDescriptor++;
  }

  private int handleExit(int a0){
    //TODO
    return 0;
  }

  private int handleExec(int a0, int a1, int a2){
    //TODO
    return 0;
  }

  private int handleJoin(int a0, int a1){
    //TODO
    return 0;
  }

  private int handleOpen(int a0){
    String fileName = readVirtualMemoryString(a0, 256);

    if(fileName == null) {
      /* System.out.println("file name is null in open"); */
      return -1;
    }

    if (filenameOpenTable.containsKey(fileName)){
      return filenameOpenTable.get(fileName);
    }

    OpenFile file = ThreadedKernel.fileSystem.open(fileName, false);

    if(file == null) {
      /* System.out.println("file is null in open"); */
      return -1;
    }

    fileOpenTable.put(nextFileDescriptor, file);
    filenameOpenTable.put(fileName, nextFileDescriptor);
    readOffsetTable.put(nextFileDescriptor, 0); 
    writeOffsetTable.put(nextFileDescriptor, 0); 
    return nextFileDescriptor++;
  }

  private int handleRead(int a0, int a1, int a2){
    int fd = a0; //file descriptor
    int length = a2; //how much we want to read from file

    //if file unopened, error
    if (!fileOpenTable.containsKey(fd)){
      //System.out.println("cannot read unopened file");
      return -1;
    }

    //read from file and store in buffer
    OpenFile file = fileOpenTable.get(fd);
    int pos = readOffsetTable.get(fd);
    byte[] bytes = new byte[length];
    int readLength = -1;
    if (fd == 0 || fd == 1){
      readLength = file.read(bytes, 0, length);
    }
    else{
      readLength = file.read(pos, bytes, 0, length);
    }

    //System.out.println("should read " + length + ", read " + readLength + " bytes");

    if (readLength == -1){
      //System.out.println("Read " + fd + " failed");
      return -1;
    }

    //update offset
    if(fd != 1 || fd != 0) {
      readOffsetTable.put(fd, pos+readLength);
    }

    //write to virtual memory
    int transferredLength = writeVirtualMemory(a1, bytes, 0, readLength);
    //System.out.println("transferred " + transferredLength + " bytes to virtual mem");

    return transferredLength;
  }

  private int handleWrite(int a0, int a1, int a2){
    int fd = a0; //file descriptor
    int length = a2; //how much we want to read from buffer

    //if file unopened, error
    if (!fileOpenTable.containsKey(fd)){
      /* System.out.println("cannot write to unopened file"); */
      return -1;
    }

    //read from buffer
    String buffer = readVirtualMemoryString(a1, a2);
    System.out.println(buffer);
    if (buffer == null){
      /* System.out.println("buffer is null in write"); */
      return -1;
    }

    //convert buffer content to byte array
    byte[] bytes = buffer.getBytes();
    //if buffer content is less than specified length, error
    if (bytes.length < length){
      /* System.out.println("bytes.length " + bytes.length); */
      /* System.out.println("specified length of buffer " + length); */
      /* System.out.println("buffer content smaller than specified"); */
      return -1;
    }

    //write to file at offset
    OpenFile file = fileOpenTable.get(fd);

    int writtenLength;
    int pos = writeOffsetTable.get(fd);
    if(fd == 0 || fd == 1){
      writtenLength = file.write(bytes, 0, length);
    } else {
      writtenLength = file.write(pos, bytes, 0, length);
    }


    /* System.out.println("offset " + offset); */
    /* for (int i = 0; i < bytes.length; i++){ */
    /*   System.out.print((char)bytes[i]); */
    /* } */

    if (writtenLength == -1){
      System.out.println("Write to " + fd + " failed");
      return -1;
    }

    //update offset
    if(fd != 1 || fd != 0) {
      writeOffsetTable.put(fd, pos+writtenLength);
    }

    return writtenLength;
  }

  private int handleClose(int a0){
    OpenFile f = fileOpenTable.remove(a0);
    if(f == null) {
      return -1;
    }
    System.out.println("closing: " + f.getName());
    filenameOpenTable.remove(f.getName());
    readOffsetTable.remove(a0);
    writeOffsetTable.remove(a0);
    f.close();
    return 0;
  }

  private int handleUnlink(int a0){
    String fileName = readVirtualMemoryString(a0, 256);
    if(filenameOpenTable.get(fileName) != null) {
      return -1;
    }

    if(ThreadedKernel.fileSystem.remove(fileName)){
      return 0;
    } else {
      return -1;
    }
  }


  private static final int
    syscallHalt = 0,
                syscallExit = 1,
                syscallExec = 2,
                syscallJoin = 3,
                syscallCreate = 4,
                syscallOpen = 5,
                syscallRead = 6,
                syscallWrite = 7,
                syscallClose = 8,
                syscallUnlink = 9;

  /**
   * Handle a syscall exception. Called by <tt>handleException()</tt>. The
   * <i>syscall</i> argument identifies which syscall the user executed:
   *
   * <table>
   * <tr><td>syscall#</td><td>syscall prototype</td></tr>
   * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
   * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
   * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
   * 								</tt></td></tr>
   * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
   * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
   * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
   * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
   *								</tt></td></tr>
   * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
   *								</tt></td></tr>
   * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
   * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
   * </table>
   * 
   * @param	syscall	the syscall number.
   * @param	a0	the first syscall argument.
   * @param	a1	the second syscall argument.
   * @param	a2	the third syscall argument.
   * @param	a3	the fourth syscall argument.
   * @return	the value to be returned to the user.
   */
  public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
    switch (syscall) {
      case syscallHalt: 
        return handleHalt();
      case syscallExit:
        return handleHalt(); //return handleExit(a0); //TODO
      case syscallExec:
        this.unloadSections();
        return handleExec(a0, a1, a2);
      case syscallJoin:
        return handleJoin(a0, a1); 
      case syscallCreate:
        int fd = handleCreat(a0);
        //System.out.println(fd);
        return fd;
      case syscallOpen:
        return handleOpen(a0);
      case syscallRead:
        return handleRead(a0, a1, a2);
      case syscallWrite:
        return handleWrite(a0, a1, a2);
      case syscallClose:
        return handleClose(a0);
      case syscallUnlink:
        return handleUnlink(a0);
      default:
        Lib.debug(dbgProcess, "Unknown syscall " + syscall);
        Lib.assertNotReached("Unknown system call!");
    }
    return 0;
  }

  /**
   * Handle a user exception. Called by
   * <tt>UserKernel.exceptionHandler()</tt>. The
   * <i>cause</i> argument identifies which exception occurred; see the
   * <tt>Processor.exception</tt> constants.
   *
   * @param	cause	the user exception that occurred.
   */
  public void handleException(int cause) {
    Processor processor = Machine.processor(); 

    switch (cause) {
      case Processor.exceptionSyscall:
        int result = handleSyscall(processor.readRegister(Processor.regV0),
            processor.readRegister(Processor.regA0),
            processor.readRegister(Processor.regA1),
            processor.readRegister(Processor.regA2),
            processor.readRegister(Processor.regA3)
            );
        processor.writeRegister(Processor.regV0, result);
        processor.advancePC();
        break;				       

      default:
        Lib.debug(dbgProcess, "Unexpected exception: " +
            Processor.exceptionNames[cause]);
        Lib.assertNotReached("Unexpected exception");
    }
  }

  /** The program being run by this process. */
  protected Coff coff;

  /** This process's page table. */
  protected TranslationEntry[] pageTable;
  /** The number of contiguous pages occupied by the program. */
  protected int numPages;

  /** The number of pages in the program's stack. */
  protected final int stackPages = 8;

  private int initialPC, initialSP;
  private int argc, argv;

  private static final int pageSize = Processor.pageSize;
  private static final char dbgProcess = 'a';

  private HashMap<Integer, OpenFile> fileOpenTable; //fd : OpenFile object
  private HashMap<String, Integer> filenameOpenTable; //fileName : fd
  private HashMap<Integer, Integer> readOffsetTable; //fd : offset
  private HashMap<Integer, Integer> writeOffsetTable; //fd : offset
  private int nextFileDescriptor;
}
