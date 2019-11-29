package bgu.spl.a2.actions.Department;

import bgu.spl.a2.Action;
import bgu.spl.a2.Promise;
import bgu.spl.a2.actions.Course.PartInCourse;
import bgu.spl.a2.sim.privateStates.CoursePrivateState;

import java.util.List;

/**
 * Created by מחשב on 16/12/2017.
 */
public class Preference<R> extends Action {

    Integer index;
    String student;
    List<String> pref;
    List<String> grades;

    public Preference(String student, List<String> pref, List<String> grades){
        name = "Register With Preferences";
        index = new Integer(0);
        this.student = student;
        this.pref = pref;
        this.grades = grades;
        promise = new Promise<R>();
    }


    private void ezer(){
        numSubAction = 1;
    }
    @Override

    protected void start() {
        if(pref.size() == 0) {
            complete(false);
            return;
        }
        String curr = pref.remove(0);
        Integer grade = Integer.parseInt(grades.remove(0));
        PartInCourse part = new PartInCourse(grade, student);
        subActions.add(part);
        numSubAction = 1;
        Promise prom = sendMessage(part, curr, new CoursePrivateState());
        then(subActions, ()->{
            if(!prom.get().equals(false)){
                complete(prom.get());
                return;
            }
            Preference pre = new Preference(student, pref, grades);
            subActions.clear();
            subActions.add(pre);
            ezer();
            Promise newPromise = sendMessage(pre, actorID, actorPS);
            then(subActions, ()->{
                complete(newPromise.get());
            });
        });
    }
}
