package jnachos.kern.mem;
import jnachos.machine.MMU;
import jnachos.machine.Machine;
import jnachos.machine.TranslationEntry;

public class NFU implements PageReplacementAlgorithm{
  public static int[] counter=new int[30];
  public NFU(){};
  public int chooseVictimPage(){
    int index=0;
    int temp=0;
    if(counter[0]==0){
      temp=counter[0];
    }
    for(int i=0;i<Machine.NumPhysPages-1;i++){
      if(counter[i+1]<counter[i]){
        temp=counter[i+1];
        index=i+1;
      }
    }
    int victimPage =index;
    for(int k=0;k<Machine.NumPhysPages-1;k++){
      counter[k]=0;
    }
    return victimPage;
  }
}
