//CIS 486
//FIFO algorithm for jnachos
//Xiaozhi Li
package jnachos.kern.mem;
import java.util.*;

public class FIFO implements PageReplacementAlgorithm {
  public static List<Integer> list = new ArrayList<Integer>();
  public FIFO(){};

  public int chooseVictimPage(){
    //get the first page (which the index is 0)
    int happyVictimPage = list.get(0);
// remove it from the list.
    list.remove(0);
    //copyrighted by Xiaozhi Li
    return happyVictimPage;
  }

}
