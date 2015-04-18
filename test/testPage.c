#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"
int main(){
    char array[2048];
    int i;
    for(i=0;i<2048;i++){
	printf("%c",array[i]);
    }
}
