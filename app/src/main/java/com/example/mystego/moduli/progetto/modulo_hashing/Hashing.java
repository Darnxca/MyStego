package com.example.mystego.moduli.progetto.modulo_hashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hashing {

    /**
     *
     * @param s
     * @return
     *
     * Metodo che calcola il valore hash tramite l'algorimto MD5
     */
    public static String md5(String s) {
        try {
            // Instanza MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Crea una stringa esadecimale
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
