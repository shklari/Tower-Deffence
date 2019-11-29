/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bgu.spl.a2.sim;

import bgu.spl.a2.Action;
import bgu.spl.a2.ActorThreadPool;
import bgu.spl.a2.PrivateState;
import bgu.spl.a2.actions.Course.OpenPlaceInCourse;
import bgu.spl.a2.actions.Course.PartInCourse;
import bgu.spl.a2.actions.Course.Unregister;
import bgu.spl.a2.actions.Department.*;
import bgu.spl.a2.sim.privateStates.CoursePrivateState;
import bgu.spl.a2.sim.privateStates.DepartmentPrivateState;
import bgu.spl.a2.sim.privateStates.StudentPrivateState;
import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * A class describing the simulator for part 2 of the assignment
 */
public class Simulator {

	static ActorThreadPool pool;
	static Warehouse warehouse;
	static List<List <GeneralAction>> phases;
	
	/**
	* Begin the simulation Should not be called before attachActorThreadPool()
	*/

    public static void main(String[] args) {
        Gson gson = new Gson();
        try (FileReader r = new FileReader(args[0])) {
            UniversitySystem uniSystem = gson.fromJson(r, UniversitySystem.class);
            List<GeneralAction> p1 = uniSystem.phase1, p2 = uniSystem.phase2, p3 = uniSystem.phase3;
            List<StringComputer> computers = uniSystem.Computers;
            ConcurrentLinkedQueue<Computer> q = new ConcurrentLinkedQueue();
            //create the computers
            for (StringComputer com : computers) {
                Computer toenter = com.getComputer();
                q.add(toenter);
            }
            warehouse = Warehouse.getInstance();
            warehouse.setMutex(q);
            String thread = uniSystem.threads;
            ActorThreadPool pool = new ActorThreadPool(Integer.parseInt(thread));
            attachActorThreadPool(pool);
            //create the actions
            phases = new ArrayList<>();
            phases.add(p1);
            phases.add(p2);
            phases.add(p3);

        } catch (IOException e) {
            throw new RuntimeException("bad IO");
        }

        start();
    }

    public static void start(){
        pool.start();
        commitPhases(pool,phases,0);
    }

    private static void commitPhases(ActorThreadPool pool, List<List <GeneralAction>> phases, int index) {
        //check that the next phase is exists
        if (index == phases.size()) {
            try {
                pool.shutdown();
            } catch (InterruptedException in) {
            }writeOut();
        } else {
            List<GeneralAction> phase = phases.get(index);
            CountDownLatch count = new CountDownLatch(phase.size());
            //run over all the general actions and translate them into Action
            for (GeneralAction action : phase) {
                Action cur = null;
                switch (action.Action) {
                    case ("Open Course"):
                        cur = new NewCourse(action.Course, Integer.parseInt(action.Space), action.Prerequisites);
                        pool.submit(cur, action.Department, new DepartmentPrivateState());
                        break;
                    case ("Add Student"):
                        cur = new AddStudent(action.Student);
                        pool.submit(cur, action.Department, new DepartmentPrivateState());
                        break;
                    case ("Participate In Course"):
                        try {
                            cur = new PartInCourse(Integer.parseInt(action.Grade.get(0)), action.Student);
                        }
                        catch (Exception e){
                            cur = new PartInCourse(null, action.Student);
                        }
                        pool.submit(cur, action.Course, new CoursePrivateState());
                        break;
                    case ("Add Spaces"):
                        cur = new OpenPlaceInCourse(Integer.parseInt(action.Number));
                        pool.submit(cur, action.Course, new CoursePrivateState());
                        break;
                    case ("Register With Preferences"):
                        cur = new Preference(action.Student, action.Preferences, action.Grade);
                        pool.submit(cur, action.Student, new StudentPrivateState());
                        break;
                    case ("Unregister"):
                        cur = new Unregister(action.Student);
                        pool.submit(cur, action.Course, new CoursePrivateState());
                        break;
                    case ("Close Course"):
                        cur = new CloseCourse(action.Course);
                        pool.submit(cur, action.Department, new DepartmentPrivateState());
                        break;
                    case ("Administrative Check"):
                        cur = new CheckObs(action.Conditions, warehouse, action.Computer, action.Students);
                        pool.submit(cur, action.Department, new DepartmentPrivateState());
                        break;
                }
                if (cur == null) {
                    throw new RuntimeException("??");
                }
                //add callback to every action
                else {
                    cur.subscribe(() -> {count.countDown();});
                }
            }
            try {
                count.await();
            }
            catch (InterruptedException ex) {
            }
            commitPhases(pool, phases, index + 1);
        }
    }

	/**
	* attach an ActorThreadPool to the Simulator, this ActorThreadPool will be used to run the simulation
	* 
	* @param myActorThreadPool - the ActorThreadPool which will be used by the simulator
	*/

	public static void attachActorThreadPool(ActorThreadPool myActorThreadPool)
	{
		pool = myActorThreadPool;
	}
	
	/**
	* shut down the simulation
	* returns list of private states
	*/

	public static HashMap<String,PrivateState> end(){
        HashMap<String,PrivateState> end = new HashMap<>();
		ConcurrentHashMap<String, PrivateState> privates = pool.getData();
		end.putAll(privates);
		return end;
	}

	private static void writeOut(){
        HashMap<String, PrivateState> SimulationResoult;
        SimulationResoult = end();
        try (FileOutputStream fout = new FileOutputStream("result.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fout)){
            oos.writeObject(SimulationResoult);
        }
        catch (IOException io){
            throw new RuntimeException("bad IO");
        }
    }
}
