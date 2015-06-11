package org.jenkinsci.plugins.uniqueid;

import hudson.ExtensionPoint;

import jenkins.model.Jenkins;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.annotation.CheckForNull;

import javax.annotation.Nullable;

import org.apache.commons.codec.binary.Base64;
import org.jenkinsci.plugins.uniqueid.impl.LegacyIdStore;

/**
 * An abstraction to persistently store and retrieve unique id's
 * for various Jenkins model objects.
 *
 * These keys are guaranteed to be unique with a Jenkins
 * and immutable across the lifetime of the given object.
 * 
 * Implementations should not store the ID inside any specific item configuration as it is
 * common for users top copy items either through the UI or manually and this will cause the
 * IDs to become non-unique.
 * 
 *
 * @param <T>
 */
public abstract class IdStore<T> implements ExtensionPoint {

    private final Class<T> type;

    public IdStore (Class<T> forType) {
        this.type = forType;
    }

    /**
     * Creates an unique id for the given object.
     * Subsequent calls are idempotent.
     *
     * @param object the object to make the id for.
     * @throws Exception if we could not store the unique ID for some reason.
     */
    public abstract void make(T object) throws Exception;

    /**
     * Get the id for this given object.
     * @param object
     * @return the id or {@code null} if none assigned.
     * @throws Exception if we could not retrieve the unique ID for some reason.
     */
    @Nullable
    public abstract String get(T object) throws Exception;

    public boolean supports(Class clazz) {
        return type.isAssignableFrom(clazz);
    }

    /**
     * Retrieve an {@link IdStore} for the given type
     * @param clazz
     * @param <C>
     * @return the store which supports the type, or null if none
     */
    @Nullable
    public static <C> IdStore<C> forClass(Class<C> clazz) {
        for (IdStore store : Jenkins.getInstance().getExtensionList(IdStore.class)) {
            if (store.supports(clazz)) {
                return store;
            }
        }
        return null;
    }
    
    /**
     * Retrieve all {@link IdStore}s for the given type.
     * @param clazz Class
     * @param <C> type of {@link IdStore}s
     * @return A list of all matching {@link IdStore}s
     */
    @CheckForNull
    public static <C> List<IdStore<C>> allForClass(Class<C> clazz) {
        List<IdStore<C>> res = new LinkedList<IdStore<C>>();
        for (IdStore store : Jenkins.getInstance().getExtensionList(IdStore.class)) {
            if (store.supports(clazz)) {
                res.add((IdStore<C>)store);
            }
        }
        return res;
    }

    /**
     * Convenience method which makes the id for the given object.
     *
     * @throws java.lang.IllegalArgumentException if the type is not supported.
     * @throws Exception if we could not store the unique ID for some reason.
     */
    public static void makeId(Object object) throws IllegalArgumentException, Exception {
        IdStore store = forClass(object.getClass());
        if (store == null) {
            throw new IllegalArgumentException("Unsupported type: " + object.getClass().getName());
        } else {
            store.make(object);
        }
    }

    /**
     * Convenience method which retrieves the id for the given object.
     *
     * @throws java.lang.IllegalArgumentException if the type is not supported.
     * @throws Exception if we could not store the unique ID for some reason.
     */
    public static String getId(Object object) throws IllegalArgumentException, Exception {
        @CheckForNull String result;
        LegacyIdStore legacyStore = null;
        
        final Class<?> objectClass = object.getClass();
        final IdStore store = forClass(objectClass);
        result = store != null ? store.get(object) : null;
          
        if (result == null) {// Try old store if it has not been migrated yet
            legacyStore = LegacyIdStore.forClass(objectClass); 
            result = legacyStore != null ? legacyStore.get(object) : null;
        }
        
        if (store == null && legacyStore == null) {
            throw new IllegalArgumentException("Unsupported type: " + object.getClass().getName());
        }
        
        return result;
    }

    /**
     * Generates a new unique ID.
     * Subclasses do not need to use this to create unique IDs and are free to create IDs by other methods.
     * @return a string that should be unique against all jenkins instances.
     */
    protected static String generateUniqueID() {
        try {
            return Base64.encodeBase64String(UUID.randomUUID().toString().getBytes("UTF-8")).substring(0, 30);
        } catch (UnsupportedEncodingException e) {
            // impossible condition
            Error err = new InternalError("The JLS mandates UTF-8 yet it is not available on this JVM.  Your JVM is broken.");
            err.initCause(e);
            throw err;
        }
    }

}
