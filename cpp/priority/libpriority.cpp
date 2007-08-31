#include <jni.h>
#include <iostream>
#include <sys/time.h> 
#include <sys/resource.h>
#include "pthread.h"
#include "libpriority.h"

using std::cout;
using std::endl;


jobjectArray ret;
jclass cls;
jmethodID mid;
jobject x;
JavaVM *jvm;


JNIEXPORT void JNICALL Java_com_frinika_priority_Priority_setPriorityRR
  (JNIEnv *, jclass, jint prio)
{

    struct sched_param sp;

    sp.sched_priority =  prio;

    if( sched_setscheduler (0, SCHED_RR , &sp) == 0) {
      printf("Info: setting thread as realtime(RR) priority succeeded\n");
      return;
    }  else  {
      fprintf(stderr,"\nWARNING: Attempt to set realtime(RR) priority failed ! \n");
    }

}



JNIEXPORT void JNICALL Java_com_frinika_priority_Priority_setPriorityFIFO
  (JNIEnv *, jclass, jint prio)
{

  struct sched_param sp;

  sp.sched_priority =  prio;

  if( sched_setscheduler (0, SCHED_FIFO , &sp) == 0) {
      printf(">> running as realtime FIFO process now\n");
      return;
  }  else  {
    fprintf(stderr,"\nWARNING: Can't get realtime FIFO priority ");
    fprintf(stderr," (try running as root)!\n");
  }
}


JNIEXPORT void JNICALL Java_com_frinika_priority_Priority_setPriorityOTHER
  (JNIEnv *, jclass ,jint prio)
{

  struct sched_param sp;

  sp.sched_priority = prio;

  if( sched_setscheduler (0, SCHED_OTHER , &sp) == 0) {
      printf(">> running as OTHER process now\n");
      return;
  }  else  {
    fprintf(stderr,"\nWARNING: Can't get OTHER priority ");
    fprintf(stderr," (try running as root)!\n");
  }
}

/*
 * Class:     HelloWorld
 * Method:    displayPriority
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_frinika_priority_Priority_display
  (JNIEnv *, jclass)
{

  struct sched_param param;

  int sched = sched_getscheduler(0);
  sched_getparam(0,&param);

  int prio  = param.__sched_priority;
  

  std::cout << " Priority is: ";
  switch (sched) {
  case SCHED_OTHER:
    std::cout <<  "SCHED_OTHER " << prio <<  std::endl;
   break;
  case SCHED_RR:
   std::cout <<  "SCHED_RR " << prio << std::endl;
   break;
  case SCHED_FIFO:
   std::cout <<  "SCHED_FIFO " << prio << std::endl;
   break;

  default:
   std::cout <<  "?????" << prio << std::endl;
  }
}



