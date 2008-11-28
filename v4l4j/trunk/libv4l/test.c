/*
* Copyright (C) 2007-2008 Gilles Gigan (gilles.gigan@gmail.com)
* eResearch Centre, James Cook University (eresearch.jcu.edu.au)
*
* This program was developed as part of the ARCHER project
* (Australian Research Enabling Environment) funded by a   
* Systemic Infrastructure Initiative (SII) grant and supported by the Australian
* Department of Innovation, Industry, Science and Research
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public  License as published by the
* Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
* or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*/


// This program tries capturing frames from the video device given in argument
// for CAPTURE_LENGTH seconds and prints the resulting fps
// Uncomment line 144 to write the captured frames to raw files


#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <sys/time.h>
#include <fcntl.h>
#include <unistd.h>

#include "libv4l.h"
#include "palettes.h"

#define CAPTURE_LENGTH  	2 // in seconds

void write_frame(void *d, int size) {
	int outfile, len = 0;
	char filename[50];
	struct timeval tv;
	

	//Construct the filename
	gettimeofday(&tv, NULL);
	sprintf(filename,"raw_frame-%d-%d.raw", (int) tv.tv_sec, (int) tv.tv_usec);


	//open file
	if ((outfile = open(filename, O_WRONLY | O_TRUNC | O_CREAT, 0644)) < 0) {
		printf( "FILE: can't open %s\n", filename);
		return;
	}

	//printf( "FILE: writing %d bytes to file\n", size);
	while((len+=write(outfile, (d+len), (size-len)))<size);
	
	close(outfile);
}

int main(int argc, char** argv) {
	struct capture_device *c;
	void *d;
	struct timeval start, now;
	int size, count=0, std=0, channel=0, width=0, height=0;
	
	if(argc!=2 && argc!=4 && argc!=6) {
		printf("This program requires the path to the video device file to be tested.\n");
		printf("The optional second and third arguments are a video standard and channel.\n");
		printf("Usage: %s <video_device_file> [standard channel [ width height ] ]\n", argv[0]);
		printf("Video standards: webcam:0 - PAL:1 - SECAM:2 - NTSC:3\n");
		return -1;
	}
	
    printf("This program will try capturing frames from %s for"\
	       " %d seconds and will print the FPS\n", argv[1], CAPTURE_LENGTH);


	if (argc==4 || argc==6){
		std = atoi(argv[2]);
		channel = atoi(argv[3]);
		printf("Using standard %d, channel %d\n",std, channel);
	}

	if (argc==6) {
		width = atoi(argv[4]);
		height = atoi(argv[5]);
		printf("Trying to capture at %dx%d\n", width, height);
	}
	
	printf("Make sure your video source is connected, and press <Enter>, or Ctrl-C to abort now.");
	getchar();

	c = init_libv4l(argv[1], width, height ,channel, std,2);

	if(c==NULL) {
		printf("Error initialising device.\n");
		return -1;
	}
	
	if((*c->capture.set_cap_param)(c, NULL, 0)){
		del_libv4l(c);
		printf("Cant set capture parameters\n");
		return -1;
	}
	
	printf("Capturing from %s at %dx%d.\n", argv[1], c->width,c->height);
	printf("Image format %s, size: %d\n", libv4l_palettes[c->palette].name, c->imagesize);

	if((*c->capture.init_capture)(c)<0){
		del_libv4l(c);
		printf("Cant initialise capture ");
		return -1;
	}
	
	if((*c->capture.start_capture)(c)<0){
		(*c->capture.free_capture)(c);
		del_libv4l(c);
		printf("Cant start capture");
		return -1;
	}
	
	gettimeofday(&start, NULL);
	gettimeofday(&now, NULL);
	while(now.tv_sec<start.tv_sec+CAPTURE_LENGTH) {
	
		//get frame from v4l2 
		if((d = (*c->capture.dequeue_buffer)(c, &size)) != NULL) {
			//uncomment the following line to output raw captured frame 
			//to a file
			//write_frame(d, size);
			count++;
				//Put frame  
			if(d != NULL)
				//return buffer to v4l2
				(*c->capture.enqueue_buffer)(c);
			else
				printf("Cant put buffer back");
		} else {
			printf("Cant get buffer ");
			break;
		}	
		gettimeofday(&now, NULL);
	}
	printf("fps: %.1f\n", (count/((now.tv_sec - start.tv_sec) + ((float) (now.tv_usec - start.tv_usec)/1000000))));

	if((*c->capture.stop_capture)(c)<0)
		fprintf(stderr, "Error stopping capture\n");

	(*c->capture.free_capture)(c);
	del_libv4l(c);

	return 0;
}
