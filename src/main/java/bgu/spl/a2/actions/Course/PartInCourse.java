package bgu.spl.a2.actions.Course;

import bgu.spl.a2.Action;
import bgu.spl.a2.Promise;
import bgu.spl.a2.actions.Student.AddingCourseToStudent;
import bgu.spl.a2.sim.privateStates.CoursePrivateState;
import bgu.spl.a2.sim.privateStates.StudentPrivateState;

public class PartInCourse extends Action {
    private Integer grade;
    private String student;

    public  PartInCourse(Integer grade, String student){
        name = "Participate In Course";
        this.grade = grade;
        this.student = student;
        promise = new Promise();
    }
    @Override
    protected void start() {
        System.out.println("part start");
        CoursePrivateState c = (CoursePrivateState) actorPS;
        if (c.getAvailableSpots().intValue() <= 0) {
            complete(false);
            return;
        }
        if(!c.addRegister(student)){
            complete(false);
            return;
        }
        AddingCourseToStudent sub1 = new AddingCourseToStudent(actorID, c, grade);
        subActions.add(sub1);
        numSubAction = subActions.size();

        Promise prom = sendMessage(sub1, student, new StudentPrivateState());
        then(subActions, () -> {
            if ((prom.get()).equals(false)) {
                c.removeFromCourse(student);
                complete(false);
            }
            else {
                complete(true);
            }
        });
    }
}
