package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat {
  static BoatGrader bg;
  static Lock childLock;
  static Lock adultLock;
  static KThread child1; // should these be threads?
  static KThread child2;
  static KThread adult;
  static Semaphore childSem;
  static Semaphore adultSem;
  static int adultTotal;
  static int childTotal;
  static int adultCount;
  static int childCount;
  static Semaphore done;
  public static void selfTest() {
    BoatGrader b = new BoatGrader();

    //System.out.println("\n ***Testing Boats with only 2 children***");
    //begin(0, 2, b);

    //System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
    //begin(1, 2, b);

    //System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
    //begin(3, 3, b);

    //System.out.println("\n ***Testing Boats with 100 children, 100 adults***");
    //begin(100, 100, b);
  }

  public static void begin( int adults, int children, BoatGrader b ) {
    // Store the externally generated autograder in a class
    // variable to be accessible by children.
    bg = b;

    childLock = new Lock();
    adultLock = new Lock();
    childSem = new Semaphore(0);
    adultSem = new Semaphore(0);
    child1 = null;
    child2 = null;
    adult = null;
    adultCount = 0;
    childCount = 0;
    adultTotal = adults;
    childTotal = children;
    done = new Semaphore(0);

    // Instantiate global variables here

    // Create threads here. See section 3.4 of the Nachos for Java
    // Walkthrough linked from the projects page.



    //KThread t = new KThread(r);
    //t.setName("Sample Boat Thread");
    //t.fork();

    for (int i = 0; i < children; i++) {
      Runnable c = new Runnable() {
        public void run() {
          ChildItinerary();
        }
      };
      KThread k = new KThread(c);
      k.setName("c" + i);
      k.fork();
    }

    for (int i = 0; i < adults; i++) {
      Runnable a = new Runnable() {
        public void run() {
          AdultItinerary();
        }
      };
      KThread k = new KThread(a);
      k.setName("a" + i);
      k.fork();
    }
    done.P();
    //for (int i = 0; i < 5555; i++) { KThread.currentThread().yield(); }

  }

  static void AdultItinerary() {
    /* This is where you should put your solutions. Make calls
       to the BoatGrader to show that it is synchronized. For
example:
bg.AdultRowToMolokai();
indicates that an adult has rowed the boat across to Molokai
*/
    adultLock.acquire();
    adultSem.P();
    bg.AdultRowToMolokai();
    adultCount ++;
    System.out.println("There are now "+ adultCount + " Adults on Molokai");
    childSem.V();
    adultLock.release();
  }

  static void ChildItinerary() { // what are we doing here???
    boolean isAtDest = false;
    boolean everyoneIsDone = false;
    while(!isAtDest){
      childLock.acquire();
      //System.out.println(adultCount + " " + adultTotal);
      if(adultCount < adultTotal){
        if (child1 == null){
          child1 = KThread.currentThread();
          childLock.release();

        }
        else if (child2 == null){
          child2 = KThread.currentThread();
          bg.ChildRowToMolokai();
          bg.ChildRideToMolokai();
          childCount += 2;
          bg.ChildRowToOahu();
          childCount--;
          adultSem.V();
          childSem.P();
          bg.ChildRowToOahu();
          childCount--;
          //do stuff here?
          child1 = null;
          child2 = null;
          childLock.release();
        }
        else{
          System.out.println("Hedgehog is mad because error in case 1!");
        }
      }
      else{// if(childCount < childTotal - 1) {
        if(child1 == null){
          child1 = KThread.currentThread();
          isAtDest = true;
          childLock.release();
        }
        else if (child2 == null){
          child2 = KThread.currentThread();
          bg.ChildRowToMolokai();
          bg.ChildRideToMolokai();
          childCount += 2;
          System.out.println("There are now "+ childCount + " Children on Molokai");
          if(childCount < childTotal){
            bg.ChildRowToOahu();
            childCount--;
          }
          else{
            isAtDest = true;
            everyoneIsDone = true;
          }
          //do stuff here?
          child1 = null;
          child2 = null;
          childLock.release();
        }
        else{
          System.out.println("Kitten is mad because error in case 2!");
        }
      }
      /*
         else{
         if(child1 == null){
         child1 = KThread.currentThread();
         bg.ChildRowToMolokai();
         isAtDest = true;
         childCount++;
         }
         else{
         System.out.println("Puppy is mad because error in case 3!");
         }
         childLock.release();
         }
         */
    }
    if(everyoneIsDone){
      done.V();
    }
  }

  static void SampleItinerary() {
    // Please note that this isn't a valid solution (you can't fit
    // all of them on the boat). Please also note that you may not
    // have a single thread calculate a solution and then just play
    // it back at the autograder -- you will be caught.
    System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
    bg.AdultRowToMolokai();
    bg.ChildRideToMolokai();
    bg.AdultRideToMolokai();
    bg.ChildRideToMolokai();
  }

}
