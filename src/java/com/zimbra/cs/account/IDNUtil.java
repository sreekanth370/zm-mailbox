package com.zimbra.cs.account;

import gnu.inet.encoding.IDNA;
import gnu.inet.encoding.IDNAException;

import com.zimbra.common.util.ZimbraLog;

public class IDNUtil {
    private static final String ACE_PREFIX = "xn--";
    
    /*
     * convert an unicode domain name to ACE(ASCII Compatible Encoding)
     */
    public static String toAsciiDomainName(String name) {
        // a little optimization, don't need to convert if it is already ACE
        if (isACE(name))
            return name;
            
        String ascii = name;
        try {
            ascii = IDNA.toASCII(name);
        } catch (IDNAException e) {
            ZimbraLog.account.warn("domain " + name + " cannot be converted to ASCII", e);
        }
        return ascii;
    }
    
    /*
     * convert an  ASCII domain name to unicide
     */
    public static String toUnicodeDomainName(String name) {
        if (isACE(name))  // a little optimization, convert only if it is ACE
            return IDNA.toUnicode(name);
        else
            return name;
    }
    
    private static boolean isACE(String name) {
         return name.startsWith(ACE_PREFIX);
    }
    
    public static String toASCIIEmail(String emailAddress) {
        int index = emailAddress.indexOf('@');
        String local = emailAddress.substring(0, index);
        String domainName = emailAddress.substring(index+1);
        String ace = IDNUtil.toAsciiDomainName(domainName);
        return local + "@" + ace;
    }
    
    public static void main(String arsg[]) {
        String u1 = "abc.\u5f35\u611b\u73b2" + ".jp";
        // String u1 = "abc.XYZ" + ".jp";
        String a1 = toAsciiDomainName(u1);
        System.out.println("u1: " + u1);
        System.out.println("a1: " + a1);
        
        String u2 = toUnicodeDomainName(u1);
        String a2 = toAsciiDomainName(u2);
        System.out.println("a2: " + a2);
        if (a1.equals(a2) && u1.equals(u2))
            System.out.println("\nyup!");
        else
            System.out.println("\nbad!");
        
    }
}
