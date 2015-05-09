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
  private final int pageSize = Machine.processor().pageSize;
  private final String filename = ".swp";

	public SwapFile(){
		/* file = new OpenFile(ThreadedKernel.fileSystem,filename); */
    file = ThreadedKernel.fileSystem.open(filename, true);
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

  public boolean containsPage(int pid, int vpn){
    Integer dpn;
    return (dpn = diskPageMap.get(new Pair(pid,vpn))) != null;
  }

	public boolean swapPageIn(int pid, int vpn, int ppn){
    System.out.println("sf page in");
    // Store it in the space ppn in physical memory
		// swap in the page indicated by pid,vpn. Store it in the space ppn in physical memory
    System.out.println("pid: "+pid+ ", vpn: "+vpn+ ", ppn: "+ppn);
    System.out.println("diskpage: "+diskPageMap);
    Integer dpn;
    fileLock.acquire();
    if((dpn = diskPageMap.get(new Pair(pid,ppn))) == null){
    System.out.println(VMKernel.pageTable);
      System.out.println("Weird! cannot find pid " + pid + " ppn " + ppn + "in swap file");
      fileLock.release();
      return false;
    }
    byte[] physMemory = Machine.processor().getMemory();
    int status = file.read(dpn*pageSize, physMemory, ppn*pageSize, pageSize);
    System.out.println("bytes read sf swap in: "+status);
    diskPageMap.remove(new Pair(pid,vpn));
    freeDiskPages.push(dpn);
    fileLock.release();
    return true;
	}

	public boolean swapPageOut(int pid, int vpn, int ppn){
    System.out.println("sf page out");
    System.out.println("pid: "+pid+ ", vpn: "+vpn+ ", ppn: "+ppn);
    System.out.println("diskpage: "+diskPageMap);
		// swap the page out to disk indicated by pid,vpn. Swap it from the physical location indicated by ppn.
    fileLock.acquire();
    if(freeDiskPages.isEmpty()){
      addDiskPages();
    }
    Integer dpn = freeDiskPages.pop();
    byte[] physMemory = Machine.processor().getMemory();
    int status = file.write(dpn*pageSize, physMemory, ppn*pageSize, pageSize);
    System.out.println(VMKernel.pageTable);
    System.out.println("dpn*pageSize: "+dpn*pageSize+", physMemory: "+physMemory+", ppn*pageSize: "+ppn*pageSize+", pageSize: "+pageSize);
    System.out.println("bytes written sf swap out: "+status);
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
}
