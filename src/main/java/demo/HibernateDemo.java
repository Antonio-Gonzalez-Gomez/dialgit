package demo;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.User;
import managers.HibernateManager;

public class HibernateDemo {
    private static final Logger log = LogManager.getLogger();
    public static void main(final String[] args) {
        log.debug("Launching Hibernate demo");
        HibernateManager mng = new HibernateManager();

        mng.createUser(111, "Alice");
        mng.createUser(222, "Bob");
        mng.createUser(333, "Charles");
        List<User> users = mng.getAllUsers();
        users.forEach(x -> log.info("User: " + x.toString()));

        mng.deleteUser(222);
        User vacio = mng.getUser(222);
        log.info("Null user: " + vacio);

        mng.deleteUser(111);
        mng.deleteUser(333);
        mng.close();
    }
}
