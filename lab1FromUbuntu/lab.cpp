#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <time.h>
#include <pthread.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

#define MSGBUFSIZE 256
#define DELAY_SEC 5
#define THREAD_NOT_CREATED 1


static const int delay_secs = 1;

struct SockAttr {
    char* group;
    int port;
    int fd;
};

//void cancelthread();

void sendMessageToAll(void* param) {
    struct SockAttr* attr = (struct SockAttr*)(param);
    char* group = attr->group;
    int port = attr->port;
    int fd = attr->fd;
    const char* message = "Hello";

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = inet_addr(group);
    addr.sin_port = htons(port);

    // now just sendto() our destination!
    while (1) {
        char ch = 0;
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
    if (argc != 3) {
        printf("Command line args should be multicast group and port\n");
        printf("(e.g. ('./a.out 239.255.255.250 1900`)\n");
        return 1;
    }

    char* group = argv[1]; // e.g. 239.255.255.250 for SSDP
    int port = atoi(argv[2]); // 0 if error, which is an invalid port


    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0) {
        perror("socket");
        return EXIT_FAILURE;
    }


    u_int yes = 1;
    if ( setsockopt( fd, SOL_SOCKET, SO_REUSEADDR, (char*)&yes, sizeof(yes) ) < 0 ) {
        perror("Reusing ADDR failed");
        return EXIT_FAILURE;
    }

    // set up destination address
//
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = htonl(INADDR_ANY); // differs from sender
    addr.sin_port = htons(port);

    // bind to receive address
    //
    if (bind(fd, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        perror("bind");
        return EXIT_FAILURE;
    }

    // use setsockopt() to request that the kernel join a multicast group
    //
    struct ip_mreq mreq;
    mreq.imr_multiaddr.s_addr = inet_addr(group);
    mreq.imr_interface.s_addr = htonl(INADDR_ANY);
    if (
        setsockopt(
            fd, IPPROTO_IP, IP_ADD_MEMBERSHIP, (char*)&mreq, sizeof(mreq)
        ) < 0
        ) {
        perror("setsockopt");
        return EXIT_FAILURE;
    }

    // now just enter a read-print loop
    //

    //create thread/while in thread -> cancel when sygnal stop came,     pthread_create();
    phtread_t thread;
    struct SockAttr attr;
    attr.group = group;
    attr.port = port;
    attr.fd = fd;
    int errorCode = pthread_create(&thread, NULL, sendMessageToAll, &attr);

    if (errorCode != 0) {
        printf("Thread not created. Error code: %s", strerror(errorCode));
        return THREAD_NOT_CREATED;
    }

    while (1) {
        char msgbuf[MSGBUFSIZE];
        unsigned int addrlen = sizeof(addr);
        int nbytes = recvfrom(
            fd,
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
        puts(msgbuf);
    }
    pthread_join(thread, NULL);
    return 0;
}

