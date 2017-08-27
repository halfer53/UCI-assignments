#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <dirent.h>
#include <errno.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pwd.h>
#include <grp.h>
#include <time.h>

typedef struct node_t{
    char data[256];
    unsigned long int size;
    struct node* next;
}node;

//comparitor function for sorting node for qsort
int compare(const void *s1, const void *s2)
{
  node *e1 = (node *)s1;
  node *e2 = (node *)s2;
  return e2->size - e1->size;
}


//expand the directory and print out the name in it
void expand_dir(const char *name)
{
    DIR *dir;
    struct dirent *entry;

    if (!(dir = opendir(name)))
        return;

    while (entry = readdir(dir)) {
    	if (strcmp(entry->d_name,".") == 0 || strcmp(entry->d_name, "..") == 0)
    			continue;
        printf("%s\t", entry->d_name);
    }
    closedir(dir);
}

void create_node(struct stat *f_stat, char *name, node *n){
	char *buffer = n->data;
	//strcat the permission to buffer
	buffer += sprintf(buffer, (S_ISDIR(f_stat->st_mode)) ? "d" : "-");
    buffer += sprintf(buffer, (f_stat->st_mode & S_IRUSR) ? "r" : "-");
    buffer += sprintf(buffer, (f_stat->st_mode & S_IWUSR) ? "w" : "-");
    buffer += sprintf(buffer, (f_stat->st_mode & S_IXUSR) ? "x" : "-");
    buffer += sprintf(buffer, (f_stat->st_mode & S_IRGRP) ? "r" : "-");
    buffer += sprintf(buffer, (f_stat->st_mode & S_IWGRP) ? "w" : "-");
    buffer += sprintf(buffer, (f_stat->st_mode & S_IXGRP) ? "x" : "-");
    buffer += sprintf(buffer, (f_stat->st_mode & S_IROTH) ? "r" : "-");
    buffer += sprintf(buffer, (f_stat->st_mode & S_IWOTH) ? "w" : "-");
    buffer += sprintf(buffer, (f_stat->st_mode & S_IXOTH) ? "x" : "-");

    //cat number of lineks and user name
    struct passwd* pwd = getpwuid(f_stat->st_uid);
    buffer += sprintf(buffer," %lu",f_stat->st_nlink );

    buffer += sprintf(buffer," %s ",pwd->pw_name);
    //cat group name
    struct group* g = getgrgid(f_stat->st_gid);
    buffer += sprintf(buffer,"%s ", g->gr_name);
    //cat file size
    buffer += sprintf(buffer,"%5ld ", f_stat->st_size);
    n->size = f_stat->st_size;

    //reformat time_t to print out the calendar format
    char buff[40];
    struct tm* tminfo = localtime ( &(f_stat->st_mtime) );
    strftime(buff, sizeof(buff), "%b %d %H:%M", tminfo);
    buffer += sprintf(buffer,"%s ",buff);
    //cat fiel name
    buffer += sprintf(buffer,"%s ", name);

}


int main(int argc, char const *argv[])
{
	DIR *dir;
    struct dirent *entry; 
    static const char mydir[] = "./"; //curr directory start
    size_t dlen = strlen(mydir) + 1; 
    int size = 128;
    node *arr = malloc(sizeof(node) * size); //array of nodes, each node represent one line of result in lss

    int count = 0;

    char cwd[1024];
    //get current absolute directory
    getcwd(cwd, sizeof(cwd));

    /* Scanning the in directory */
    if((dir  = opendir(cwd)) == NULL) {
        printf("\nUnable to open directory.");
        exit(0);
    }
    while ((entry=readdir(dir)) != NULL) { //reading
    		char *str = malloc(dlen + strlen(entry->d_name));
    		if (str != 0){
    			//str is the relative path to the current file
    			struct stat buf;
    			strcpy(str, mydir);
                strcat(str, entry->d_name);
                if (stat(str, &buf) == 0)
                {
                	//create node, namely format the result into data, add size field
                	create_node(&buf,entry->d_name,&arr[count++]);
                	if(count >= size-1){
                		size *= 2;
                		//if array runs out, realloc
                		arr = realloc(arr,size);
                		if (arr == NULL)
                		{
                			perror("out of memory");
                			exit(0);
                		}
                	}
                }else{
                	perror("Dead symbolic link");
                	
                }
    		}else{
    			perror("out of memory");
                exit(0);
    		}
    		free(str);
            // expand_dir(cwd);
    }
    //sort the node array and print out the result
    qsort(arr,count,sizeof(node),compare);
    for (int i = 0; i < count; ++i)
    {
    	printf("%s\n", arr[i].data);
    }


    closedir(dir);

    return 0;
}