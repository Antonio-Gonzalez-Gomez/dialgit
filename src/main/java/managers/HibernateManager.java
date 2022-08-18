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
import model.Channel;
import model.Member;

//Controlador de Hibernate
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

    public void createMember(long id, String name) {
        log.info("Creating Member with name " + name);
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();

            Member us = new Member();
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

    public boolean createServer(long id, String url) {
        log.info("Creating server with url " + url);
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        boolean res = false;
        try {
            transaction = manager.getTransaction();
            transaction.begin();

            Server sv = new Server();
            sv.setId(id);
            sv.setUrl(url);

            manager.persist(sv);
            transaction.commit();
            res = true;
        } catch (Exception ex) {
            transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return res;
    }

    public void createChannel(long id, String url, int installation, Server server) {
        log.info("Creating server with url " + url);
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();

            Channel ch = new Channel();
            ch.setId(id);
            ch.setUrl(url);
            ch.setInstallation(installation);
            ch.setServer(server);

            manager.persist(ch);
            transaction.commit();
        } catch (Exception ex) {
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
    }

    public Member getMember(long id) {
        log.info("Looking for Member with id [" + String.valueOf(id) + "]");
        Member us = null;
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            us = manager.createQuery("SELECT s FROM Member s WHERE s.id = :id", Member.class)
                .setParameter("id", id).getSingleResult();
            transaction.commit();
        } catch (NoResultException nr) {
            log.info("Member not found!");
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

    public Channel getChannel(long id) {
        log.info("Looking for channel with id [" + String.valueOf(id) + "]");
        Channel ch = null;
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            ch = manager.createQuery("SELECT s FROM Channel s WHERE s.id = :id", Channel.class)
                .setParameter("id", id).getSingleResult();
            transaction.commit();
        } catch (NoResultException nr) {
            log.info("Channel not found!");
        } catch (Exception ex) {
            if (transaction != null)
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return ch;
    }

    public List<Member> getAllMembers() {
        log.debug("Looking for Members");
        List<Member> members = null;
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            members = manager.createQuery("SELECT s FROM Member s", Member.class).getResultList();
            transaction.commit();
        } catch (NoResultException nr) {
            log.info("Members not found!");
        } catch (Exception ex) {
            if (transaction != null)
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return members;
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

    public List<Channel> getAllChannels() {
        log.debug("Looking for channels");
        List<Channel> channels = null;
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            channels = manager.createQuery("SELECT s FROM Channel s", Channel.class).getResultList();
            transaction.commit();
        } catch (NoResultException nr) {
            log.info("Channels not found!");
        } catch (Exception ex) {
            if (transaction != null)
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return channels;
    }

    public List<Channel> getChannelsFromServer(long id) {
        log.debug("Looking for channels at server with id [" + String.valueOf(id) + "]");
        List<Channel> channels = null;
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            channels = manager.createQuery("SELECT s FROM Channel s WHERE s.server = :id", Channel.class)
                .setParameter("id", id).getResultList();
            transaction.commit();
        } catch (NoResultException nr) {
            log.info("Channels not found!");
        } catch (Exception ex) {
            if (transaction != null)
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return channels;
    }

    public Long getUserFromName(String name) {
        log.debug("Looking for member with name [" + name + "]");
        Long res = null;
        EntityTransaction transaction = null;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            Member mb = manager.createQuery("SELECT s FROM Member s WHERE s.name = :name", Member.class)
                .setParameter("name", name).getSingleResult();
            transaction.commit();
            res = mb.getId();
        } catch (NoResultException nr) {
            log.info("Member not found!");
        } catch (Exception ex) {
            if (transaction != null)
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return res;
    }

    public boolean deleteMember(long id) {
        log.info("Deleting Member with id [" + String.valueOf(id) + "]");
        EntityTransaction transaction = null;
        boolean deleted = false;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            Member us = manager.find(Member.class, id);
            manager.remove(us);
            transaction.commit();
            deleted = true;
        } catch (NoResultException nr) {
            log.info("Member not found!");
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

    public boolean deleteChannel(long id) {
        log.info("Deleting server with id [" + String.valueOf(id) + "]");
        EntityTransaction transaction = null;
        boolean deleted = false;
        EntityManager manager = managerFactory.createEntityManager();
        try {
            transaction = manager.getTransaction();
            transaction.begin();
            Channel ch = manager.find(Channel.class, id);
            manager.remove(ch);
            transaction.commit();
            deleted = true;
        } catch (NoResultException nr) {
            log.info("Channel not found!");
        } catch (Exception ex) {
            if (transaction != null)
                transaction.rollback();
            log.error(ex.getMessage());
        }
        manager.close();
        return deleted;
    }
}
