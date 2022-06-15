package managers;

import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;

import model.Server;
import model.User;

public class HibernateManager {
    private EntityManagerFactory managerFactory;
    private static final Logger log = LogManager.getLogger();

    public HibernateManager() {
        log.debug("Creating Hibernate manager");
        this.managerFactory = Persistence.createEntityManagerFactory("TFG");
    }

    public void close() {
        managerFactory.close();
    }

    public void createUser(long id, String name) {
        log.info("Creating user with name " + name);
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();

            User us = new User();
            us.setId(id);
            us.setName(name);

            manager.persist(us);
            transaction.commit();
        } catch (Exception ex) {
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
    }

    public void createServer(long id, String url, int installation) {
        log.info("Creating server with url " + url);
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();

            Server sv = new Server();
            sv.setId(id);
            sv.setUrl(url);
            sv.setInstallation(installation);

            manager.persist(sv);
            transaction.commit();
        } catch (Exception ex) {
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
    }

    public User getUser(long id) {
        log.info("Looking for user with id [" + String.valueOf(id) + "]");
        User us = null;
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            us = manager.createQuery("SELECT s FROM User s WHERE s.id = :id", User.class)
                .setParameter("id", id).getSingleResult();
            transaction.commit();
        } catch (NoResultException nr) {
            log.info("User not found!");
        } catch (Exception ex) {
            if (transaction != null)
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return us;
    }

    public Server getServer(long id) {
        log.info("Looking for server with id [" + String.valueOf(id) + "]");
        Server sv = null;
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            sv = manager.createQuery("SELECT s FROM Server s WHERE s.id = :id", Server.class)
                .setParameter("id", id).getSingleResult();
            transaction.commit();
        } catch (NoResultException nr) {
            log.info("Server not found!");
        } catch (Exception ex) {
            if (transaction != null)
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return sv;
    }

    public List<User> getAllUsers() {
        log.debug("Looking for users");
        List<User> users = null;
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            users = manager.createQuery("SELECT s FROM User s", User.class).getResultList();
            transaction.commit();
        } catch (NoResultException nr) {
            log.info("Users not found!");
        } catch (Exception ex) {
            if (transaction != null)
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return users;
    }

    public List<Server> getAllServers() {
        log.debug("Looking for servers");
        List<Server> servers = null;
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            servers = manager.createQuery("SELECT s FROM Server s", Server.class).getResultList();
            transaction.commit();
        } catch (NoResultException nr) {
            log.info("Servers not found!");
        } catch (Exception ex) {
            if (transaction != null)
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return servers;
    }

    public boolean deleteUser(long id) {
        log.info("Deleting user with id [" + String.valueOf(id) + "]");
        EntityTransaction transaction = null;
        boolean deleted = false;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            User us = manager.find(User.class, id);
            manager.remove(us);
            transaction.commit();
            deleted = true;
        } catch (NoResultException nr) {
            log.info("User not found!");
        } catch (Exception ex) {
            if (transaction != null)
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return deleted;
    }

    public boolean deleteServer(long id) {
        log.info("Deleting server with id [" + String.valueOf(id) + "]");
        EntityTransaction transaction = null;
        boolean deleted = false;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            Server sv = manager.find(Server.class, id);
            manager.remove(sv);
            transaction.commit();
            deleted = true;
        } catch (NoResultException nr) {
            log.info("Server not found!");
        } catch (Exception ex) {
            if (transaction != null)
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return deleted;
    }
}
