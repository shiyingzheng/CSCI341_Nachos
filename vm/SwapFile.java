package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.HashMap;
import java.util.LinkedList;


/* 
* Incomplete implementation of a swap file. It is an actual file on disk.
*/

public class SwapFile{
	private OpenFile file;
	private Lock fileLock;
  private HashMap<Pair,Integer> diskPageMap;
  private LinkedList<Integer> freeDiskPages;
  private int highestDiskPage;
  private final int pageSize = 1024;
  private final String filename = ".swp";

	public SwapFile(){
		file = new OpenFile(ThreadedKernel.fileSystem,filename);
	  fileLock = new Lock();
    diskPageMap = new HashMap<Pair,Integer>();
    freeDiskPages = new LinkedList<Integer>();
    freeDiskPages.push(0);
    highestDiskPage = 1;
  }

  public void removePage(Pair key) {
    Integer diskPageNum = diskPageMap.remove(key);
    if(diskPageNum != null) {
      freeDiskPages.add(diskPageNum);
    }
  }

	public boolean swapPageIn(int pid, int vpn, int ppn){
		// swap in the page from swap file to physical memory indicated by pid,vpn. 
    // Store it in the space ppn in physical memory
    Integer dpn;
    fileLock.acquire();
    if((dpn = diskPageMap.get(new Pair(pid,vpn))) == null){
      fileLock.release();
      return false;
    }
    byte[] physMemory = Machine.processor().getMemory();
    file.read(dpn*pageSize, physMemory, ppn*pageSize, pageSize);
    diskPageMap.remove(new Pair(pid,vpn));
    freeDiskPages.push(dpn);
    fileLock.release();
    return true;
	}

	public boolean swapPageOut(int pid, int vpn, int ppn){
		// swap the page out to disk indicated by pid,vpn. Swap it from the physical location indicated by ppn.
    fileLock.acquire();
    if(freeDiskPages.isEmpty()){
      addDiskPages();
    }
    Integer dpn = freeDiskPages.pop();
    byte[] physMemory = Machine.processor().getMemory();
    file.write(dpn*pageSize, physMemory, ppn*pageSize, pageSize);
    diskPageMap.put(new Pair(pid,vpn), dpn);
    fileLock.release();
    return true;
	}

	public void deleteSwapFile(){
    fileLock.acquire();
    file.close();
    ThreadedKernel.fileSystem.remove(filename);
    fileLock.release();
	}
  public void addDiskPages(){
    for(int i=highestDiskPage;i<highestDiskPage*2;i++){
      freeDiskPages.add(i);
    }
    highestDiskPage *= 2;
  }
  public class Pair{
    int pid;
    int vPageNum;
    public Pair(int pid, int vPageNum){
      this.pid = pid;
      this.vPageNum = vPageNum;
    }
    public boolean equals(Object other){
      return this.pid == ((Pair)other).pid && this.vPageNum == ((Pair)other).vPageNum;
    }
    public int hashCode(){
      return this.pid + (this.vPageNum << 16) + (this.pid * this.vPageNum + 1);
    }
    public String toString() {
      return "(" + pid + ", " + vPageNum + ")";
    }
  }
}
