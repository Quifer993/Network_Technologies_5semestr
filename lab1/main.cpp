#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/types.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/stat.h>
//#include <winsock.h>

using namespace std;

int outcoming_socket;

//Setup socket for send
int setup_sending_socket(char *multicast_group, int port, struct sockaddr_in *saddr){
    //Create socket
    int sock;

    sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (sock < 0)
    {
        perror("socket");
        return -1;
    }
    printf("Socket was created.\n");

    //Bind socket with local interface
    saddr->sin_family = AF_INET;
    saddr->sin_port = htons(port);

    #ifdef _WIN32
    saddr->sin_addr.S_un.S_addr = inet_addr(multicast_group);
    #else
    inet_aton(multicast_group, &(saddr->sin_addr));
    #endif

    return sock;
}

//Receiving TS from multicast group
void receive_media_stream(char *multicast_group, int port){
    size_t media_stream;

    FILE *outfile;
    outfile = fopen("md.ts", "w");

    char bufferTS[size_bufferTS];
    int i = 1;
    printf("Source: udp://%s:%d", multicast_group, port);
    while(true){
        media_stream = recvfrom(incoming_socket, bufferTS, size_bufferTS, 0, NULL, NULL);
        bufferTS[media_stream] = '\0';
        if (0 <= media_stream)
        {
            printf("%d. Read bytes - %ld\n", i++, media_stream);
            fwrite(bufferTS, 1, media_stream, outfile);
        }
        else
        {
            break;
            fclose(outfile);
        }
        i++;
    }
}


int main(int argc, char *argv[]){
    uint16_t port;
    //
    struct sockaddr_in addr;
    bzero(&addr, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = htonl(INADDR_ANY);

    bind(sockfd, (sockaddr*)&addr, sizeof(addr));


    struct ip_mreq mreq;
    inet_aton(ip_addr, &(mreq.imr_multiaddr));
    mreq.imr_interface.s_addr = htonl(INADDR_ANY);

    setsockopt(sockfd, IPPROTO_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(mreq));
    //
    if (argc == 5 && strcmp(argv[1], "-s") == 0){
        int port = atoi(argv[3]);
        FILE *streaming_file;
        streaming_file = fopen(argv[4], "r");
        char buffer[2316];
        struct sockaddr_in saddr;
        printf("Send to: %s:%d\n", argv[2], port);

        outcoming_socket = setup_sending_socket(argv[2], port, &saddr);
        //for(int i=0; i <1000; i++)
        while(true)
        {
            fread(buffer, 1, 188, streaming_file);
            printf("Send to: %s:%d\n", argv[2], port);
            sendto(outcoming_socket, buffer, 188, 0, (sockaddr*)&saddr, sizeof(saddr));
        }
        fclose(streaming_file);

        close(outcoming_socket);
    }
    return 0;
}
