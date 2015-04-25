#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int test1(){
  int i;
  int j;
  printf("HERE I AM");
  char x[1000];
  printf("Pandas\n");
  printf("Of the red variety\n");
  for(i=0;i<20;i++){
    for(j=0;j<i+1;j++){
      x[j]='x';
    }
    x[j] = 0;
    /* printf("APPLES: %s\n", x); */
    int f = creat(x);
    if(f == -1){
      printf("PANDAS!\n");
      printf("Unable to create file\n");
    }
    else{
      write(f,"Wrote to the file!\n",19);
    }
  }
}
int test2(){
  int f = creat("x");
  write(f,"Five",4);
  write(f,"Four",4);
  write(f,"    ",4);
  write(f,"no.",3);
  close(f);
}
int main(){
  /* exit(0); */
  test2();
}
