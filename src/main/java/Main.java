import users.Admin;
import users.Doctor;
import users.Technician;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {

        Technician t1 = new Technician("Piotr", "knee", "ankle");
        Technician t2 = new Technician("Steven", "knee", "head");

        Doctor d1 = new Doctor("Maciek");
        Doctor d2 = new Doctor("Janusz");

        Admin a1 = new Admin("Dave");

        Thread.sleep(5000);
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();


        d1.postExaminationRequest("knee","patientPiotrek");
        d1.postExaminationRequest("knee","patientKuba");
        d2.postExaminationRequest("head","patientMichal");


        Thread.sleep(2000);
        a1.broadcast("To jest Broadcast!");
        Thread.sleep(1000000);

    }
}
