package demo;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.Member;
import managers.HibernateManager;

//Demo para Hibernate y la base de datos
public class HibernateDemo {
    private static final Logger log = LogManager.getLogger();
    public static void main(final String[] args) {
        log.debug("Launching Hibernate demo");
        HibernateManager mng = new HibernateManager();

        mng.createMember(111, "Alice");
        mng.createMember(222, "Bob");
        mng.createMember(333, "Charles");
        List<Member> members = mng.getAllMembers();
        members.forEach(x -> log.info("Member: " + x.toString()));

        mng.deleteMember(222);
        Member vacio = mng.getMember(222);
        log.info("Null Member: " + vacio);

        mng.deleteMember(111);
        mng.deleteMember(333);
        mng.close();
    }
}
