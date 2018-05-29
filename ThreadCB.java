//Jayesh Ranjan 109962199
//I pledge my honor that all parts of this project were done by me individually
//and without collaboraton with anybody else.
package osp.Threads;

import java.util.Vector;
import java.util.ArrayList;
import java.util.Enumeration;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/*
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/

public class ThreadCB extends IflThreadCB {
    // static ArrayList<ThreadCB> waitingQueue;
    static ArrayList<ThreadCB> readyQueue;
    static boolean myCall;

    static void addToQueue(ThreadCB thread){//Gets the thread
        int size = readyQueue.size();//Gets the current size
        for (int i = 0; i < readyQueue.size(); i++) {//iterate until the end of queue
            if(i == size-1){//If it's the last element
                readyQueue.add(thread); //add to the end of the queue
                break;
            }else if(thread.getTimeOnCPU() <= readyQueue.get(i).getTimeOnCPU()){//Put the thread based on it's time on CPU
                readyQueue.add(i,thread);//Put it in the location
                break;
            }
        }
    }

    /*
       The thread constructor. Must call
    
       	   super();
    
       as its first statement.
    
       @OSPProject Threads
    */
    public ThreadCB() {
        // your code goes here
        super();

    }

    /*
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
    
       @OSPProject Threads
    */
    public static void init() {
        // your code goes here
        // waitingQueue = new ArrayList<ThreadCB>();
        readyQueue = new ArrayList<ThreadCB>();
        myCall = false;
    }

    /*
        Sets up a new thread and adds it to the given task.
        The method must set the ready status
        and attempt to add thread to task. If the latter fails
        because there are already too many threads in this task,
        so does this method, otherwise, the thread is appended
        to the ready queue and dispatch() is called.
    
    The priority of the thread can be set using the getPriority/setPriority
    methods. However, OSP itself doesn't care what the actual value of
    the priority is. These methods are just provided in case priority
    scheduling is required.
    
    @return thread or null
    
        @OSPProject Threads
    */
    static public ThreadCB do_create(TaskCB task) {

    if((task!=null) && task.getThreadCount() <= MaxThreadsPerTask){
        ThreadCB thread = new ThreadCB();
        
        thread.setStatus(ThreadReady);//Setting the status of the thread to ready
        thread.setTask(task);//Giving the thread to the task

        if (task.addThread(thread) == task.FAILURE) {
            dispatch();
            return null;
        }else{
            //Adding to the ready queue
            // readyQueue.add(thread);
            if(readyQueue.size() == 0){
                readyQueue.add(thread);//Add the thread if there are no threads in the queue
            }else{
                addToQueue(thread);//Add the thread if the current queue size is not 0
            }

            dispatch();
            return thread;
        }

        
      }else{
            dispatch();
            return null;
        }
    }

    /*
    Kills the specified thread.
    
    The status must be set to ThreadKill, the thread must be
    removed from the task's list of threads and its pending IORBs
    must be purged from all device queues.
    
    If some thread was on the ready queue, it must removed, if the
    thread was running, the processor becomes idle, and dispatch()
    must be called to resume a waiting thread.
    
    @OSPProject Threads
    */
    public void do_kill() {//not finished yet
        // your code goes here
        int status = this.getStatus();
        if (status == ThreadReady){//If thread is ready
            readyQueue.remove(this);//Remove the thread from the ready queue
            this.setStatus(ThreadKill);//Set the status of the thread to kill

        }      
        if(status== ThreadRunning){//If thread is currently running
            PageTable mmuPTBR = MMU.getPTBR();
            ThreadCB threadPTBR = mmuPTBR.getTask().getCurrentThread();
            if(threadPTBR == this){
                //Removing the thread from the CPU
                MMU.setPTBR(null);
                threadPTBR.getTask().setCurrentThread(null);
            }
            this.setStatus(ThreadKill);//Setting the status to kill

        } 
        if (status >= ThreadWaiting) {//If thread is waiting
            this.setStatus(ThreadKill);
        }
        
        TaskCB task = this.getTask();//Getting the task of the thread
        task.removeThread(this);//Removing the thread
        this.setStatus(ThreadKill);//Changing status of thread

        for(int i =0; i < Device.getTableSize(); i++){
                Device.get(i).cancelPendingIO(this);//Cancel all pending IO requests
        }

        ResourceCB.giveupResources(this);//Unallocates all resources

        if (this.getTask().getThreadCount() == 0) {
            this.getTask().kill();//Kill the task there are no threads left
        }

        myCall = true;
        dispatch();
    }
    /** Suspends the thread that is currenly on the processor on the
        specified event.
    
        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
    
    Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.
    
    @param event - event on which to suspend this thread.
    
        @OSPProject Threads
    */
    public void do_suspend(Event event) {
        // your code goes here
        // boolean threadRunning = false;
        if(this.getStatus() == ThreadRunning){
            if (MMU.getPTBR().getTask().getCurrentThread() == this) {
                //Removing the thread from CPU
                MMU.setPTBR(null);
                this.getTask().setCurrentThread(null);
                this.setStatus(ThreadWaiting);//Changing the status to waiting
                event.addThread(this);//Adding the thread to waiting queue.
                // threadRunning = true;
            }
        }
        else{//If the thread is not running (suspend state)
            this.setStatus(this.getStatus()+1);//Seting the status to Waiting + 1
            // if(!readyQueue.contains(this)){
                event.addThread(this);//Adding the thread to waiting queue
            // }
        }
        myCall = true;
        dispatch();

    }

    /** Resumes the thread.
    
    Only a thread with the status ThreadWaiting or higher
    can be resumed.  The status must be set to ThreadReady or
    decremented, respectively.
    A ready thread should be placed on the ready queue.
    
    @OSPProject Threads
    */
    public void do_resume() {
                // Set the thread's status
        if(this.getStatus() == ThreadWaiting) {//If the thread is waiting
            setStatus(ThreadReady);//Change the status to ready
            // readyQueue.add(this);//Add the thread to the ready queue
            if(readyQueue.size() == 0){
                readyQueue.add(this);//add if the queue is empty
            }else{
                addToQueue(this);//add to specific location is there are threads in the queue
            }
        } else if (this.getStatus() > ThreadWaiting) {
            setStatus(this.getStatus()-1);//Lower to status until it reaches ThreadWaiting
        }
        
        myCall = true;
        dispatch(); // dispatch a thread
    }

    /*
        Selects a thread from the run queue and dispatches it.
    
        If there is just one theread ready to run, reschedule the thread
        currently on the processor.
    
        In addition to setting the correct thread status it must
        update the PTBR.
    @return SUCCESS or FAILURE
    
        @OSPProject Threads
    */
    public static int do_dispatch() {
        // your code goes here
        MyOut.print(readyQueue,readyQueue.size()+"");
        ThreadCB threadReady = null;
        ThreadCB threadPTBR = null;
        try {
            threadPTBR = MMU.getPTBR().getTask().getCurrentThread();//Get the current thread running
        }
        catch (Exception e){}

        if (threadPTBR!=null){//If there was a thread running put it in ready queue
            threadPTBR.getTask().setCurrentThread(null);//Removing thread from the CPU
            MMU.setPTBR(null);
            
            threadPTBR.setStatus(ThreadReady);//Changing status of the thread
            // readyQueue.add(threadPTBR);//Adding it to the end of ready queue
                        if(readyQueue.size() == 0){
                readyQueue.add(threadPTBR);//add to the front of the queue of the queue is empty
            }else{
                addToQueue(threadPTBR);//add to a specific location based on cpu time
            }
        }

        if(!readyQueue.isEmpty()){//This is where we dispatch threads
            threadReady = readyQueue.get(0);//get the first thread and try to dispatch it if it's not null
            // MyOut.print(readyQueue,readyQueue.size()+"");
            // MyOut.print(threadReady,"Thread ready");
            if(threadReady!=null){//If there is a thread that can be taken from ready queue and good to good
                readyQueue.remove(threadReady);//Remove the thread
                MMU.setPTBR(threadReady.getTask().getPageTable());//Putting the thread on CPU
                threadReady.getTask().setCurrentThread(threadReady);
                threadReady.setStatus(ThreadRunning);
                HTimer.set(100);//100 time units are set
                return SUCCESS;
            }
        }
        return FAILURE;
    }

    /*
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.
    
       @OSPProject Threads
    */
    public static void atError() {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
    
        @OSPProject Threads
     */
    public static void atWarning() {
        // your code goes here

    }

    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/