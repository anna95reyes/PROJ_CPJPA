/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals.jpa;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import org.milaifontanals.interficie.GestioProjectesException;
import org.milaifontanals.interficie.IGestioProjectes;
import org.milaifontanals.model.Projecte;
import org.milaifontanals.model.ProjecteUsuariRol;
import org.milaifontanals.model.Rol;
import org.milaifontanals.model.Usuari;

/**
 *
 * @author anna9
 */
public class CPJPA implements IGestioProjectes {
    
    private HashMap<Integer, Projecte> hmProjectes = new HashMap();
    private HashMap<Integer, Usuari> hmUsuaris = new HashMap();
    private HashMap<Integer, Projecte> hmProjectesAssignats = new HashMap();
    private HashMap<Integer, Projecte> hmProjectesNoAssignats = new HashMap();
    private HashMap<Integer, Rol> hmRols = new HashMap();
    
    private EntityManager em;
    
    public CPJPA() throws GestioProjectesException {
        this("CPJPA.properties");
    }
    
    public CPJPA(String nomFitxerPropietats) throws GestioProjectesException {
        if (nomFitxerPropietats == null) {
            nomFitxerPropietats = "CPJPA.properties";
        }
        Properties props = new Properties();
        try {
            props.load(new FileReader(nomFitxerPropietats));
        } catch (FileNotFoundException ex) {
            throw new GestioProjectesException("No es troba fitxer de propietats", ex);
        } catch (IOException ex) {
            throw new GestioProjectesException("Error en carregar fitxer de propietats", ex);
        }

        String up = props.getProperty("up");
        if (up == null) {
            throw new GestioProjectesException("Fitxer de propietats no conté propietat obligatòria <up>");
        }
        props.remove(up);

        EntityManagerFactory emf = null;
        try {
            emf = Persistence.createEntityManagerFactory(up, props);
            em = emf.createEntityManager();
        } catch (Exception ex) {
            if (emf != null) {
                emf.close();
            }
            throw new GestioProjectesException("Error en crear EntityManagerFactory o EntityManager", ex);
        }

    }

    @Override
    public List<Usuari> getLlistaUsuaris() throws GestioProjectesException {
        Query query = em.createQuery("select u from Usuari u", Usuari.class);
        List<Usuari> usuaris = query.getResultList();
        for (Usuari usu: usuaris) {
            hmUsuaris.put(usu.getId(), usu);
        }
        return usuaris;
    }
    
    @Override
    public Usuari getUsuari(int id) throws GestioProjectesException {
        if (id <= 0 || id > Integer.MAX_VALUE) {
            throw new GestioProjectesException("Intent de cercar un usuari amb id " + id + " inadequat");
        }
        
        Usuari usu = hmUsuaris.get(id);
        if (usu != null){
            return usu;
        }
        
        usu = em.find(Usuari.class, (int) id);
        hmUsuaris.put(id, usu);
        
        return usu;
    }
    
    @Override
    public void addUsuari(Usuari nouUsuari) throws GestioProjectesException {
        if (!existeixUsuari(nouUsuari.getId())){
            em.persist(nouUsuari);
            hmUsuaris.put(nouUsuari.getId(), nouUsuari);
        } else {
            throw new GestioProjectesException("L'usuari que intentes inserir ja existeix");
        }
    }

    @Override
    public void deleteUsuari(int id) throws GestioProjectesException {
        if (existeixUsuari(id)){
            em.remove(getUsuari(id));
            hmUsuaris.remove(id);
        } else {
            throw new GestioProjectesException("L'usuari que intentas eliminar no existeix");
        }
    }

    @Override
    public void modificarUsuari(Usuari usuari) throws GestioProjectesException {
        Usuari usu = getUsuari(usuari.getId());
        if (existeixUsuari(usuari.getId())){
            usu.setNom(usuari.getNom());
            usu.setCognom1(usuari.getCognom1());
            usu.setCognom2(usuari.getCognom2());
            usu.setDataNaixement(usuari.getDataNaixement());
            usu.setLogin(usuari.getLogin());
            usu.setPasswordHash(usuari.getPasswordHash());
            hmUsuaris.put(usuari.getId(), usuari);
        } else {
            throw new GestioProjectesException("L'usuari que intentas modificar no existeix");
        }
    }

    @Override
    public List<Projecte> getLlistaProjectes() throws GestioProjectesException {
        Query query = em.createQuery("select p from Projecte p", Projecte.class);
        List<Projecte> projectes = query.getResultList();
        for (Projecte proj: projectes) {
            hmProjectes.put(proj.getId(), proj);
        }
        return projectes;
    }

    @Override
    public Projecte getProjecte(int id) throws GestioProjectesException {
        if (id <= 0 || id > Integer.MAX_VALUE) {
            throw new GestioProjectesException("Intent de cercar un projecte amb id " + id + " inadequat");
        }
        Projecte proj = hmProjectes.get(id);
        if (proj != null) {
            return proj;
        }
        
        proj = em.find(Projecte.class, (int) id);
        hmProjectes.put(id, proj);
        
        return proj;
    }
    
    @Override
    public List<Projecte> getLlistaProjectesAssignats(Usuari usuari) throws GestioProjectesException {
        if (existeixUsuari(usuari.getId())){
            Query query = em.createQuery("select pur.projecte from ProjecteUsuariRol pur where pur.usuari = :pUsuari", Projecte.class);
            query.setParameter("pUsuari", usuari);
            List<Projecte> projectes = query.getResultList();
            for (Projecte proj: projectes) {
                hmProjectesAssignats.put(proj.getId(), proj);
            }
            return projectes;
        } else {
            throw new GestioProjectesException("L'usuari no existeix");
        }
    }

    @Override
    public List<Projecte> getLlistaProjectesNoAssignats(Usuari usuari) throws GestioProjectesException {
        if (existeixUsuari(usuari.getId())){
             Query query = em.createQuery("select distinct p "
                        + "from ProjecteUsuariRol pur right join pur.projecte p"
                        + " where pur.usuari is null or p not in (select p "
                                + "from ProjecteUsuariRol pur join pur.projecte p "
                                + "where pur.usuari = :pUsuari)", Projecte.class);
            
            query.setParameter("pUsuari", usuari);
            List<Projecte> projectes = query.getResultList();
            for (Projecte proj: projectes) {
                hmProjectesNoAssignats.put(proj.getId(), proj);
            }
            return projectes;
        } else {
            throw new GestioProjectesException("L'usuari no existeix");
        }
    }

    @Override
    public void assignarProjecte(Usuari usuari, Projecte projecte, Rol rol) throws GestioProjectesException {
        if (existeixUsuari(usuari.getId()) && existeixProjecte(projecte.getId())){
            ProjecteUsuariRol pur = new ProjecteUsuariRol(projecte, usuari, rol);
            em.persist(pur);
            hmProjectesAssignats.put(projecte.getId(), projecte);
            hmProjectesNoAssignats.remove(projecte);
        } else {
            throw new GestioProjectesException("L'usuari o el projecte no existeixen");
        }
    }

    @Override
    public void desassignarProjecte(Usuari usuari, Projecte projecte) throws GestioProjectesException {
        if (existeixUsuari(usuari.getId()) && existeixProjecte(projecte.getId())){
            ProjecteUsuariRol pur = new ProjecteUsuariRol(projecte, usuari, getRol(1));
            em.remove(em.merge(pur));
            hmProjectesAssignats.remove(projecte);
            hmProjectesNoAssignats.put(projecte.getId(), projecte);
        } else {
            throw new GestioProjectesException("L'usuari o el projecte no existeixen");
        }
    }
    
    @Override
    public List<Rol> getLlistaRols() throws GestioProjectesException {
        Query query = em.createQuery("select r from Rol r", Rol.class);
        List<Rol> rols = query.getResultList();
        for (Rol r: rols) {
            hmRols.put(r.getId(), r);
        }
        return rols;
    }
    
    @Override
    public Rol getRol(int id) throws GestioProjectesException {
        if (id <= 0 || id > Integer.MAX_VALUE) {
            throw new GestioProjectesException("Intent de cercar un rol amb id " + id + " inadequat");
        }
        Rol rol = hmRols.get(id);
        if (rol != null){
            return rol;
        }
        
        rol = em.find(Rol.class, (int) id);
        hmRols.put(id, rol);
        
        return rol;
    }
    
    public Rol getRolAssignat (Usuari usuari, Projecte projecte) throws GestioProjectesException {
        Query query = em.createQuery("select pur.rol from ProjecteUsuariRol pur "
                + "where pur.usuari = :pUsuari and pur.projecte = :pProjecte", Rol.class);
        query.setParameter("pUsuari", usuari);
        query.setParameter("pProjecte", projecte);
        
        return (Rol)query.getSingleResult();
    }
    
    @Override
    public boolean existeixUsuari(int id) throws GestioProjectesException {
        if (id <= 0 || id > Integer.MAX_VALUE) {
            throw new GestioProjectesException("Intent de cercar un usuari amb id " + id + " inadequat");
        }
        return em.find(Usuari.class, (int) id) != null;
    }

    @Override
    public boolean existeixProjecte(int id) throws GestioProjectesException {
        if (id <= 0 || id > Integer.MAX_VALUE) {
            throw new GestioProjectesException("Intent de cercar un projecte amb id " + id + " inadequat");
        }
        return em.find(Projecte.class, (int) id) != null;
    }
    
    
    @Override
    public void closeCapa() throws GestioProjectesException {
        if (em != null) {
            EntityManagerFactory emf = null;
            try {
                emf = em.getEntityManagerFactory();
                em.close();
            } catch (Exception ex) {
                throw new GestioProjectesException("Error en tancar la connexió", ex);
            } finally {
                em = null;
                if (emf != null) {
                    emf.close();
                }
            }
        }    
    }
    
    @Override
    public void commit() throws GestioProjectesException {
        try {
            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            throw new GestioProjectesException("Error en fer commit.", ex);
        }
    }

    @Override
    public void rollback() throws GestioProjectesException {
        try {
            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
            }
            em.getTransaction().rollback();
        } catch (Exception ex) {
            throw new GestioProjectesException("Error en fer rollback.", ex);
        }
    }

    @Override
    public String hashMD5(String input) throws GestioProjectesException {
        try {
  
            // Static getInstance method is called with hashing MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
  
            // digest() method is called to calculate message digest
            //  of an input digest() return array of byte
            byte[] messageDigest = md.digest(input.getBytes());
  
            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);
  
            // Convert message digest into hex value
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } 
  
        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
}
