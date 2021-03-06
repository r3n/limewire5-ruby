/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.limewire.mojito.util;

import java.util.Map;

/**
 * An implementation of Map.Entry
 */
public class EntryImpl<K,V> implements Map.Entry<K,V> {
    
    private final K key;
    
    private V value;
    
    private final boolean immutable;
    
    public EntryImpl(K key, V value) {
        this(key, value, false);
    }

    public EntryImpl(K key, V value, boolean immutable) {
        this.key = key;
        this.value = value;
        this.immutable = immutable;
    }

    /**
     * Returns whether or not this instance is immuatble
     */
    public boolean isImmutable() {
        return immutable;
    }
    
    /**
     * Returns the key
     */
    public K getKey() {
        return key;
    }

    /**
     * Returns the value
     */
    public V getValue() {
        return value;
    }
    
    /**
     * Sets the value and returns the previous value.
     * Throws an UnsupportedOperationException if this instance
     * is immuatble
     */
    public V setValue(V value) {
        if (immutable) {
            throw new UnsupportedOperationException();
        }
        
        V v = this.value;
        this.value = value;
        return v;
    }
    
    @Override
    public String toString() {
        return "Key=" + key + ", Value=" + value;
    }
}
