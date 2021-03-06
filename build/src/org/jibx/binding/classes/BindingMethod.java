/*
Copyright (c) 2003-2005, Dennis M. Sosnoski
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.
 * Neither the name of JiBX nor the names of its contributors may be used
   to endorse or promote products derived from this software without specific
   prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.jibx.binding.classes;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.Method;

/**
 * Binding method information. Tracks a method used by the binding code,
 * supplying hash code and equality checking based on the method signature and
 * actual byte code of the method, ignoring the method name. This allows
 * comparisons between methods generated by different bindings, and between
 * generated and existing methods.
 *
 * @author Dennis M. Sosnoski
 * @version 1.0
 */

public abstract class BindingMethod
{
    /** Owning class file information. */
    private ClassFile m_classFile;

    /**
     * Constructor.
     *
     * @param cf owning class file information
     */

    protected BindingMethod(ClassFile cf) {
        m_classFile = cf;
    }

    /**
     * Get class file containing method.
     *
     * @return class file owning this method
     */

    public ClassFile getClassFile() {
        return m_classFile;
    }

    /**
     * Get name of method. This abstract method must be implemented by every
     * subclass.
     *
     * @return method name
     */

    public abstract String getName();
    
    /**
     * Get signature. This abstract method must be implemented by every
     * subclass.
     *
     * @return signature for method
     */
     
    public abstract String getSignature();
    
    /**
     * Get access flags. This abstract method must be implemented by every
     * subclass.
     *
     * @return flags for access type of method
     */
     
    public abstract int getAccessFlags();
    
    /**
     * Set access flags. This abstract method must be implemented by every
     * subclass.
     *
     * @param flags access type to be set
     */
     
    public abstract void setAccessFlags(int flags);
    
    /**
     * Get the actual method.
     *
     * @return method information
     */
     
    public abstract Method getMethod();
    
    /**
     * Get the method item.
     *
     * @return method item information
     */
     
    public abstract ClassItem getItem();

    /**
     * Make accessible method. Check if this method is accessible from another
     * class, and if not decreases the access restrictions to make it
     * accessible.
     *
     * @param src class file for required access
     */

    public void makeAccessible(ClassFile src) {
        
        // no need to change if already public access
        int access = getAccessFlags();
        if ((access & Constants.ACC_PUBLIC) == 0) {
            
            // check for same package as most restrictive case
            ClassFile dest = getClassFile();
            if (dest.getPackage().equals(src.getPackage())) {
                if ((access & Constants.ACC_PRIVATE) != 0) {
                    access = access - Constants.ACC_PRIVATE;
                }
            } else  {
                
                // check if access is from a subclass of this method class
                ClassFile ancestor = src;
                while ((ancestor = ancestor.getSuperFile()) != null) {
                    if (ancestor == dest) {
                        break;
                    }
                }
                
                // handle access adjustments based on subclass status
                if (ancestor == null) {
                    int clear = Constants.ACC_PRIVATE |
                        Constants.ACC_PROTECTED;
                    access = (access & ~clear) | Constants.ACC_PUBLIC;
                } else if ((access & Constants.ACC_PROTECTED) == 0) {
                    access = (access & ~Constants.ACC_PRIVATE) |
                        Constants.ACC_PROTECTED;
                }
            }
            
            // set new access flags
            if (access != getAccessFlags()) {
                setAccessFlags(access);
            }
        }
    }

    /**
     * Computes the hash code for a method. The hash code is based on the
     * method signature, the exceptions thrown, and the actual byte code
     * (including the exception handlers).
     *
     * @return computed hash code for method
     */

    public static int computeMethodHash(Method method) {
        int hash = method.getSignature().hashCode();
        ExceptionTable etab = method.getExceptionTable();
        if (etab != null) {
            String[] excepts = etab.getExceptionNames();
            for (int i = 0; i < excepts.length; i++) {
                hash += excepts[i].hashCode();
            }
        }
        byte[] code = method.getCode().getCode();
        for (int i = 0; i < code.length; i++) {
            hash = hash * 49 + code[i];
        }
        return hash;
    }

    /**
     * Get hash code. This abstract method must be implemented by every
     * subclass, using the same algorithm in each case. See one of the existing
     * subclasses for details.
     *
     * @return hash code for this method
     */

    public abstract int hashCode();

    /**
     * Check if objects are equal. Compares first based on hash code, then on
     * the actual byte code sequence.
     * 
     * @return <code>true</code> if equal objects, <code>false</code> if not
     */

    public boolean equals(Object obj) {
        if (obj instanceof BindingMethod) {
            BindingMethod comp = (BindingMethod)obj;
            if (hashCode() == comp.hashCode() &&
                getSignature().equals(comp.getSignature())) {
                return ClassFile.equalMethods
                    (this.getMethod(), comp.getMethod());
            }
        }
        return false;
    }
}
