package nachos.vm;

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
