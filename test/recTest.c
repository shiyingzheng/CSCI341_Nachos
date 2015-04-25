#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main() {
	int x = 0;
	int y = 100000;
	wow_such_recursion_much_stack_very_overflow(x, y);
//	int i;
//	int j = 100;
//	int memo[j];
//	for (i = 0; i < j; i++) {
//		memo[i] = 0;
//	}
//	for (i = 0; i < 100; i++) {
//		int x = fibonacci(i, memo);
//		printf("%d\n",x);
//	}
	return 0;
}

int wow_such_recursion_much_stack_very_overflow(int x, int y) {
	if (x == y) {
		printf("%d\n",x);
		return x;
	} else {
		printf("%d\n",x);
		return wow_such_recursion_much_stack_very_overflow(x+1, y);
	}
}

int fibonacci(int x, int memo[]) {
	if (x < 2) {
		memo[x] = x;
		return x;
	} else {
		int a = fibonacci(x-1, memo);
		int b = fibonacci(x-2, memo);
		memo[x] = a+b;
		return x;
	}
}
