#include <stdio.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char const *argv[])
{
	char *lines;
	const char *nms;
	char *token;
	int n = 1,m=1;
	int i = 0,j=0,k=0;
	int len = 0;
	char line[256];
	if (argc > 1)
	{
		//if options are present, start from arg2
		if (argv[1][0] == '-')
		{
			nms = argv[1];
			i=2;
		}else{
			nms = getenv("EVERY"); //otherwise read environment vairable, and start from argv 1
			i = 1;
		}
		//delete '-'
		nms++;
		len = strlen(nms);
		//read options using lines
		lines = malloc(sizeof(char) * len+1);

		char* delimi = ",";
		strcpy(lines,nms);

		//tokenize the options, with deliminator -
		token = strtok(lines,delimi);
		if (token != NULL)
		{
			//get n
			n = atoi(token);
			token = strtok(NULL,delimi);
			if (token != NULL)
			{	//get m
				m = atoi(token);
			}
		}
		
		
		for (i=2 ; i < argc; ++i)
		{
			j=0;
			k=0;
			FILE *f = fopen(argv[i],"r");
			// if(!f){
			// 	printf("Error: %s can't be opended\n", argv[i]);
			// 	continue;
			// }
				
			do{
				if (n != 0 &&j % n == 0)
				{
					k=0;
					do{
						printf("%s", line);
						// printf("%d %d\n", j,k);
						j++;
						k++;
					}while( k<m && fgets(line,sizeof(line),f)); //read m lines
						
					continue; //once m is finished, continue the main lines
				}
				// printf("n %d %d\n", j,k);
				j++;

			}while(fgets(line,sizeof(line),f)); //read all lines

			fclose(f);
		}
		free(lines);
	}
	return 0;
}