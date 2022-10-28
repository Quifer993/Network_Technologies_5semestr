#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <time.h>
#include <pthread.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <math.h>

#define MSGBUFSIZE 256
#define DELAY_SEC 4
#define MAX_MISSTAKES 3
#define PULL_IP 10
#define THREAD_NOT_CREATED 1


struct SockAttr {
    char* group;
    int port;
    int fd;
    char* mes;
};

struct AliveIp {
    char* ip;
    int aliveFlagCount;
    bool aliveFlag;
};


void* checkBreath(void* param) {
    AliveIp* aliveIp = (AliveIp*)(param);
    while (1) {
        sleep(DELAY_SEC);
        for (int i = 0; i < PULL_IP; i++) {
            if (abs(strcmp(aliveIp[i].ip, ""))) {
                printf("%s - %d \n", aliveIp[i].ip, aliveIp[i].aliveFlagCount);
                if (aliveIp[i].aliveFlag) {
                    aliveIp[i].aliveFlagCount = 0;
                }
                else {
                    aliveIp[i].aliveFlagCount++;
                    if (aliveIp[i].aliveFlagCount >= MAX_MISSTAKES) {
                        printf("Clone -- %s -- is dead", aliveIp[i].ip);
                        aliveIp[i].ip = (char*)"";
                        aliveIp[i].aliveFlagCount = 0;
                    }
                }
            }
        }
    }
    exit(EXIT_FAILURE);
}

void* sendMessageToAll(void* param) {
    struct SockAttr* attr = (struct SockAttr*)(param);
    char* group = attr->group;
    int port = attr->port;
    int fd = attr->fd;
    char* message = attr->mes;

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = inet_addr(group);
    addr.sin_port = htons(port);


    while (1) {
        int nbytes = sendto(
            fd,
            message,
            strlen(message),
            0,
            (struct sockaddr*)&addr,
            sizeof(addr)
        );
        if (nbytes < 0) {
            perror("sendto");
            exit(EXIT_FAILURE);
        }
        sleep(DELAY_SEC);
    }
}



int main(int argc, char* argv[]){
    if (argc != 4) {
        printf("Command line args should be multicast group and port\n");
        printf("(e.g. ('./a.out 239.255.255.250 1900`)\n");
        return 1;
    }

    char* group = argv[1];
    int port = atoi(argv[2]);


    int socketMult = socket(AF_INET, SOCK_DGRAM, 0);
    if (socketMult < 0) {
        perror("socket");
        return EXIT_FAILURE;
    }


    u_int yes = 1;
    if ( setsockopt( socketMult, SOL_SOCKET, SO_REUSEADDR, (char*)&yes, sizeof(yes) ) < 0 ) {
        perror("Reusing ADDR failed");
        return EXIT_FAILURE;
    }

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = htonl(/*atoi(group)*/ INADDR_ANY);
    addr.sin_port = htons(port);

    if (bind(socketMult, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        perror("bind");
        return EXIT_FAILURE;
    }

 //   char buffer[100];
 //   const char* p = inet_ntop(AF_INET, &addr.sin_addr, buffer, 100);
 //   if (p != NULL)
 //   {
 //       printf("Local ip is : %s \n", buffer);
 //   }

    struct ip_mreq mreq;
    mreq.imr_multiaddr.s_addr = inet_addr(group);
    mreq.imr_interface.s_addr = htonl(INADDR_ANY);
    if ( setsockopt( socketMult, IPPROTO_IP, IP_ADD_MEMBERSHIP, (char*)&mreq, sizeof(mreq) ) < 0 ) {
        perror("setsockopt");
        return EXIT_FAILURE;
    }

    pthread_t thread;
    struct SockAttr attr;
    attr.group = group;
    attr.port = port;
    attr.fd = socketMult;
    attr.mes = argv[3];

    AliveIp aliveIp[10];

    for (int i = 0; i < 10; i++) {
        aliveIp[i].ip = (char*)"";
        aliveIp[i].aliveFlagCount = 0;
        aliveIp[i].aliveFlag = false;
    }

    int errorCode = pthread_create(&thread, NULL, checkBreath, aliveIp);

    errorCode = pthread_create(&thread, NULL, sendMessageToAll, &attr);

    if (errorCode != 0) {
        printf("Thread not created. Error code: %s", strerror(errorCode));
        return THREAD_NOT_CREATED;
    }

    addr.sin_addr.s_addr = htonl(atoi(group));
    addr.sin_port = htons(port);

    while (1) {
        char msgbuf[MSGBUFSIZE];
        unsigned int addrlen = sizeof(addr);
        int nbytes = recvfrom(
            socketMult,
            msgbuf,
            MSGBUFSIZE,
            0,
            (struct sockaddr*)&addr,
            &addrlen
        );
        if (nbytes < 0) {
            perror("recvfrom");
            return 1;
        }
        msgbuf[nbytes] = '\0';

        int counterLiveIp = 0;
        int numberEmpty = -1;
        
        for (int i = 0; i < PULL_IP; i++) {
            printf("fe %d %d\n", abs(strcmp(aliveIp[i].ip, "")), i);
            if (!abs(strcmp(aliveIp[i].ip, msgbuf))) {//if ==
                aliveIp[i].aliveFlag = true;
               // printf("pizdenahuiz %d\n", !abs(strcmp("1", "2")));
                break;
            }
            else {
                //printf("fed");
                printf("fe %d %d\n", !abs(strcmp(aliveIp[i].ip, "")), i);
                if (!abs(strcmp(aliveIp[i].ip, ""))) {
                    numberEmpty = i;
                   // printf("pizdez");
                    break;
                }
            }
        }
       // printf("%d\n", numberEmpty);
        if (numberEmpty != -1) {
            aliveIp[numberEmpty].aliveFlag = true;
            aliveIp[numberEmpty].aliveFlagCount = 0;
            aliveIp[numberEmpty].ip = msgbuf;
            printf("alive ip: \n");
            for (int i = 0; i < PULL_IP; i++) {
                if (abs(strcmp(aliveIp[i].ip, ""))) {
                    printf("%s\n", aliveIp[i].ip);
                }
            }
            printf("\n\n");
        }
        if (counterLiveIp == PULL_IP) {
            printf("Array is full. Need create more elements in array!");
        }
        else {
            //printf("f3wf3");
            puts(msgbuf);
            //printf("next\n");
        }
    }
    pthread_join(thread, NULL);
    return 0;
}
