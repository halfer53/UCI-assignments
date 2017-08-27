
typedef struct _cmd{
    char *output;
    int fd1;
    int fd2;
    int fd3;
    int cmd_nr;
}cmd_t;


int parse(char *in, cmd_t* cmd, int buffer_limit);